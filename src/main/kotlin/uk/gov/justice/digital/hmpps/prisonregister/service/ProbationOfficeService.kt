package uk.gov.justice.digital.hmpps.prisonregister.service

import jakarta.persistence.EntityNotFoundException
import jakarta.validation.ValidationException
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.prisonregister.exceptions.EmailAddressAlreadyExistsException
import uk.gov.justice.digital.hmpps.prisonregister.exceptions.PhoneNumberAlreadyExistsException
import uk.gov.justice.digital.hmpps.prisonregister.model.AccessibleAccess
import uk.gov.justice.digital.hmpps.prisonregister.model.AgencyAddress
import uk.gov.justice.digital.hmpps.prisonregister.model.AreaRepository
import uk.gov.justice.digital.hmpps.prisonregister.model.EmailAddress
import uk.gov.justice.digital.hmpps.prisonregister.model.EmailAddressRepository
import uk.gov.justice.digital.hmpps.prisonregister.model.LocalAuthorityRepository
import uk.gov.justice.digital.hmpps.prisonregister.model.PayrollRegionRepository
import uk.gov.justice.digital.hmpps.prisonregister.model.PhoneNumber
import uk.gov.justice.digital.hmpps.prisonregister.model.ProbationOffice
import uk.gov.justice.digital.hmpps.prisonregister.model.ProbationOfficeRepository
import uk.gov.justice.digital.hmpps.prisonregister.model.RegionRepository
import uk.gov.justice.digital.hmpps.prisonregister.model.SubareaRepository
import uk.gov.justice.digital.hmpps.prisonregister.resource.CreateProbationOfficeDto
import uk.gov.justice.digital.hmpps.prisonregister.resource.LegacyAccessibleAccess
import uk.gov.justice.digital.hmpps.prisonregister.resource.LegacyAgencyAddressDto
import uk.gov.justice.digital.hmpps.prisonregister.resource.LegacyAgencyDto
import uk.gov.justice.digital.hmpps.prisonregister.resource.LegacyAgencyEmailDto
import uk.gov.justice.digital.hmpps.prisonregister.resource.LegacyAgencyPhoneDto
import uk.gov.justice.digital.hmpps.prisonregister.resource.LegacyAgencyResponse
import uk.gov.justice.digital.hmpps.prisonregister.resource.LegacyAgencyType
import uk.gov.justice.digital.hmpps.prisonregister.resource.ProbationOfficeDto
import uk.gov.justice.digital.hmpps.prisonregister.resource.UpdateAddressDto
import uk.gov.justice.digital.hmpps.prisonregister.resource.UpdateEmailAddressDto
import uk.gov.justice.digital.hmpps.prisonregister.resource.UpdatePhoneNumberDto
import uk.gov.justice.digital.hmpps.prisonregister.resource.UpdateProbationOfficeDto
import uk.gov.justice.digital.hmpps.prisonregister.resource.dto.AgencyAddressDto
import uk.gov.justice.digital.hmpps.prisonregister.resource.dto.AgencyEmailDto
import uk.gov.justice.digital.hmpps.prisonregister.resource.dto.AgencyPhoneDto
import uk.gov.justice.digital.hmpps.prisonregister.resource.dto.CodeDescription

@Service
@Transactional
class ProbationOfficeService(
  private val probationOfficeRepository: ProbationOfficeRepository,
  private val areaRepository: AreaRepository,
  private val subareaRepository: SubareaRepository,
  private val regionRepository: RegionRepository,
  private val payrollRegionRepository: PayrollRegionRepository,
  private val localAuthorityRepository: LocalAuthorityRepository,
  private val emailAddressRepository: EmailAddressRepository,
) {
  fun deleteAll() {
    probationOfficeRepository.deleteAll()
  }

  fun getAllIds(): List<String> = probationOfficeRepository.findAll().map { it.probationOfficeId }

  fun getAll(): List<ProbationOfficeDto> = probationOfficeRepository.findAll().map { it.toProbationOfficeDto() }

  fun findById(probationOfficeId: String): ProbationOfficeDto = probationOfficeRepository.findByIdOrNull(probationOfficeId)?.toProbationOfficeDto()
    ?: throw EntityNotFoundException("Probation office $probationOfficeId not found")

  fun createProbationOffice(createProbationOfficeDto: CreateProbationOfficeDto): ProbationOfficeDto {
    if (probationOfficeRepository.existsById(createProbationOfficeDto.probationOfficeId)) {
      throw ValidationException("Probation office ${createProbationOfficeDto.probationOfficeId} already exists")
    }

    val probationOffice = createProbationOfficeDto.toProbationOffice()
    probationOffice.addresses += createProbationOfficeDto.addresses.map { it.toAgencyAddress() }
    probationOffice.emailAddresses += createProbationOfficeDto.emailAddresses.map { EmailAddress(it.address) }
    probationOffice.phoneNumbers += createProbationOfficeDto.phoneNumbers.map { PhoneNumber(it.number) }

    return probationOfficeRepository.saveAndFlush(probationOffice).toProbationOfficeDto()
  }

  fun updateProbationOffice(probationOfficeId: String, updateProbationOfficeDto: UpdateProbationOfficeDto): ProbationOfficeDto {
    val probationOffice = probationOfficeRepository.findByIdOrNull(probationOfficeId) ?: throw EntityNotFoundException("Probation office $probationOfficeId not found")
    probationOffice.update(updateProbationOfficeDto)
    return probationOffice.toProbationOfficeDto()
  }

  fun deleteProbationOffice(probationOfficeId: String) {
    val probationOffice = probationOfficeRepository.findByIdOrNull(probationOfficeId) ?: throw EntityNotFoundException("Probation office $probationOfficeId not found")
    probationOfficeRepository.delete(probationOffice)
  }

  fun createProbationOfficeAddress(probationOfficeId: String, updateAddressDto: UpdateAddressDto): AgencyAddressDto {
    val probationOffice = probationOfficeRepository.findByIdOrNull(probationOfficeId) ?: throw EntityNotFoundException("Probation office $probationOfficeId not found")

    val address = updateAddressDto.toAgencyAddress()
    probationOffice.addresses += address
    probationOfficeRepository.flush()

    return AgencyAddressDto(
      id = address.id,
      addressLine1 = address.addressLine1,
      addressLine2 = address.addressLine2,
      town = address.town,
      county = address.county,
      postcode = address.postcode,
      country = address.country,
    )
  }

  fun updateProbationOfficeAddress(probationOfficeId: String, addressId: Long, updateAddressDto: UpdateAddressDto): AgencyAddressDto {
    val probationOffice = probationOfficeRepository.findByIdOrNull(probationOfficeId) ?: throw EntityNotFoundException("Probation office $probationOfficeId not found")
    val address = probationOffice.addresses.find { it.id == addressId } ?: throw EntityNotFoundException("Address $addressId not found for probation office $probationOfficeId")

    with(updateAddressDto) {
      address.addressLine1 = addressLine1
      address.addressLine2 = addressLine2
      address.town = town
      address.county = county
      address.postcode = postcode
      address.country = country
    }

    return AgencyAddressDto(
      id = address.id,
      addressLine1 = address.addressLine1,
      addressLine2 = address.addressLine2,
      town = address.town,
      county = address.county,
      postcode = address.postcode,
      country = address.country,
    )
  }

  fun deleteProbationOfficeAddress(probationOfficeId: String, addressId: Long): AgencyAddressDto {
    val probationOffice = probationOfficeRepository.findByIdOrNull(probationOfficeId) ?: throw EntityNotFoundException("Probation office $probationOfficeId not found")
    val address = probationOffice.addresses.find { it.id == addressId } ?: throw EntityNotFoundException("Address $addressId not found for probation office $probationOfficeId")

    probationOffice.addresses.remove(address)

    // returned for audit payload
    return AgencyAddressDto(
      id = address.id,
      addressLine1 = address.addressLine1,
      addressLine2 = address.addressLine2,
      town = address.town,
      county = address.county,
      postcode = address.postcode,
      country = address.country,
    )
  }

  fun createProbationOfficePhoneNumber(probationOfficeId: String, updatePhoneNumberDto: UpdatePhoneNumberDto): AgencyPhoneDto {
    val probationOffice = probationOfficeRepository.findByIdOrNull(probationOfficeId) ?: throw EntityNotFoundException("Probation office $probationOfficeId not found")

    // phone number must be unique within the probation office
    if (probationOffice.phoneNumbers.any { it.value == updatePhoneNumberDto.number }) {
      throw PhoneNumberAlreadyExistsException(updatePhoneNumberDto.number)
    }

    val phoneNumber = PhoneNumber(updatePhoneNumberDto.number)
    probationOffice.phoneNumbers += phoneNumber
    probationOfficeRepository.flush()

    return AgencyPhoneDto(
      id = phoneNumber.id,
      number = phoneNumber.value,
    )
  }

  fun updateProbationOfficePhoneNumber(probationOfficeId: String, phoneNumberId: Long, updatePhoneNumberDto: UpdatePhoneNumberDto): AgencyPhoneDto {
    val probationOffice = probationOfficeRepository.findByIdOrNull(probationOfficeId) ?: throw EntityNotFoundException("Probation office $probationOfficeId not found")
    val phoneNumber = probationOffice.phoneNumbers.find { it.id == phoneNumberId } ?: throw EntityNotFoundException("Phone number $phoneNumberId not found for probation office $probationOfficeId")

    // phone number must be unique within the probation office
    if (probationOffice.phoneNumbers.any { it.id != phoneNumberId && it.value == updatePhoneNumberDto.number }) {
      throw PhoneNumberAlreadyExistsException(updatePhoneNumberDto.number)
    }

    phoneNumber.value = updatePhoneNumberDto.number

    return AgencyPhoneDto(
      id = phoneNumber.id,
      number = phoneNumber.value,
    )
  }

  fun deleteProbationOfficePhoneNumber(probationOfficeId: String, phoneNumberId: Long): AgencyPhoneDto {
    val probationOffice = probationOfficeRepository.findByIdOrNull(probationOfficeId) ?: throw EntityNotFoundException("Probation office $probationOfficeId not found")
    val phoneNumber = probationOffice.phoneNumbers.find { it.id == phoneNumberId } ?: throw EntityNotFoundException("Phone number $phoneNumberId not found for probation office $probationOfficeId")

    probationOffice.phoneNumbers.remove(phoneNumber)

    return AgencyPhoneDto(
      id = phoneNumber.id,
      number = phoneNumber.value,
    )
  }

  fun createProbationOfficeEmailAddress(probationOfficeId: String, updateEmailAddressDto: UpdateEmailAddressDto): AgencyEmailDto {
    val probationOffice = probationOfficeRepository.findByIdOrNull(probationOfficeId) ?: throw EntityNotFoundException("Probation office $probationOfficeId not found")

    // email address is unique across all establishments
    if (emailAddressRepository.getByValue(updateEmailAddressDto.address) != null) {
      throw EmailAddressAlreadyExistsException(updateEmailAddressDto.address)
    }

    val emailAddress = EmailAddress(updateEmailAddressDto.address)
    probationOffice.emailAddresses += emailAddress
    probationOfficeRepository.flush()

    return AgencyEmailDto(
      id = emailAddress.id,
      address = emailAddress.value,
    )
  }

  fun updateProbationOfficeEmailAddress(probationOfficeId: String, emailAddressId: Long, updateEmailAddressDto: UpdateEmailAddressDto): AgencyEmailDto {
    val probationOffice = probationOfficeRepository.findByIdOrNull(probationOfficeId) ?: throw EntityNotFoundException("Probation office $probationOfficeId not found")
    val emailAddress = probationOffice.emailAddresses.find { it.id == emailAddressId } ?: throw EntityNotFoundException("Email address $emailAddressId not found for probation office $probationOfficeId")

    emailAddress.value = updateEmailAddressDto.address

    return AgencyEmailDto(
      id = emailAddress.id,
      address = emailAddress.value,
    )
  }

  fun deleteProbationOfficeEmailAddress(probationOfficeId: String, emailAddressId: Long): AgencyEmailDto {
    val probationOffice = probationOfficeRepository.findByIdOrNull(probationOfficeId) ?: throw EntityNotFoundException("Probation office $probationOfficeId not found")
    val emailAddress = probationOffice.emailAddresses.find { it.id == emailAddressId } ?: throw EntityNotFoundException("Email address $emailAddressId not found for probation office $probationOfficeId")

    probationOffice.emailAddresses.remove(emailAddress)

    return AgencyEmailDto(
      id = emailAddress.id,
      address = emailAddress.value,
    )
  }

  private fun ProbationOffice.toProbationOfficeDto() = ProbationOfficeDto(
    probationOfficeId = this.probationOfficeId,
    probationOfficeName = this.name,
    description = this.description,
    contact = this.contact,
    active = this.active,
    accessibleAccess = this.accessibleAccess?.name,
    inactiveDate = this.inactiveDate,
    cjitCode = this.cjitCode,
    area = this.area?.let { area -> CodeDescription(area.code, area.description) },
    subarea = this.subarea?.let { subarea -> CodeDescription(subarea.code, subarea.description) },
    region = this.region?.let { region -> CodeDescription(region.code, region.description) },
    geographicalArea = this.geographicalArea?.let { area -> CodeDescription(area.code, area.description) },
    localAuthority = this.localAuthority?.let { localAuthority -> CodeDescription(localAuthority.code, localAuthority.description) },
    payrollRegion = this.payrollRegion?.let { pr -> CodeDescription(pr.code, pr.description) },
    addresses = this.addresses.map { address ->
      AgencyAddressDto(
        id = address.id,
        addressLine1 = address.addressLine1,
        addressLine2 = address.addressLine2,
        town = address.town,
        county = address.county,
        postcode = address.postcode,
        country = address.country,
      )
    },
    emailAddresses = this.emailAddresses.map { emailAddress ->
      AgencyEmailDto(
        id = emailAddress.id,
        address = emailAddress.value,
      )
    },
    phoneNumbers = this.phoneNumbers.map { phoneNumber ->
      AgencyPhoneDto(
        id = phoneNumber.id,
        number = phoneNumber.value,
      )
    },
  )

  fun tryFindById(agencyId: String): LegacyAgencyDto? = probationOfficeRepository.findByIdOrNull(agencyId)?.let { po ->
    LegacyAgencyDto(
      agencyType = LegacyAgencyType.PROBATION_OFFICE,
      name = po.name,
      description = po.description,
      active = po.active,
      inactiveDate = po.inactiveDate,
      cjitCode = po.cjitCode,
      areaCode = po.area?.code,
      subareaCode = po.subarea?.code,
      regionCode = po.region?.code,
      geographicalAreaCode = po.geographicalArea?.code,
      payrollRegionCode = po.payrollRegion?.code,
      localAuthorityCode = po.localAuthority?.code,
      courtTypeCode = null,
      accessibleAccess = po.accessibleAccess?.let { runCatching { LegacyAccessibleAccess.valueOf(it.name) }.getOrNull() },
      contact = po.contact,
      addresses = po.addresses.map { LegacyAgencyAddressDto(it.addressLine1, it.addressLine2, it.town, it.county, it.postcode, it.country) },
      emailAddresses = po.emailAddresses.map { LegacyAgencyEmailDto(it.value) },
      phoneNumbers = po.phoneNumbers.map { LegacyAgencyPhoneDto(it.value) },
    )
  }

  fun createOrUpdateProbationOfficeFromLegacyData(probationOfficeId: String, agencyDto: LegacyAgencyDto): LegacyAgencyResponse = probationOfficeRepository.findByIdOrNull(probationOfficeId)?.let { probationOffice ->
    probationOffice.update(agencyDto)
    if (probationOffice.addresses.size == 1 && agencyDto.addresses.size == 1) {
      probationOffice.addresses[0].update(agencyDto.addresses[0])
    } else {
      probationOffice.addresses.clear()
      probationOffice.addresses += agencyDto.addresses.map { it.toAgencyAddress() }
    }

    probationOffice.phoneNumbers.updatePhoneNumberFrom(agencyDto.phoneNumbers)
    probationOffice.emailAddresses.updateEmailAddressFrom(agencyDto.emailAddresses)

    LegacyAgencyResponse(updated = true)
  } ?: let {
    val probationOffice = agencyDto.toProbationOffice(probationOfficeId)
    probationOffice.addresses += agencyDto.addresses.map { it.toAgencyAddress() }
    probationOffice.phoneNumbers += agencyDto.phoneNumbers.map { it.toAgencyPhone() }
    probationOffice.emailAddresses += agencyDto.emailAddresses.map { it.toAgencyEmail() }
    probationOfficeRepository.saveAndFlush(probationOffice)
    LegacyAgencyResponse(updated = false)
  }

  private fun LegacyAgencyDto.toProbationOffice(probationOfficeId: String) = ProbationOffice(
    probationOfficeId = probationOfficeId,
    name = this.name,
    description = this.description,
    contact = this.contact,
    active = this.active,
    accessibleAccess = this.accessibleAccess?.let { AccessibleAccess.valueOf(it.name) },
    inactiveDate = this.inactiveDate,
    cjitCode = this.cjitCode,
    area = this.areaCode?.let { areaRepository.findByIdOrNull(it) ?: throw ValidationException("$it area code not found for agency $probationOfficeId") },
    subarea = this.subareaCode?.let { subareaRepository.findByIdOrNull(it) ?: throw ValidationException("$it subarea code not found for agency $probationOfficeId") },
    region = this.regionCode?.let { regionRepository.findByIdOrNull(it) ?: throw ValidationException("$it region code not found for agency $probationOfficeId") },
    geographicalArea = this.geographicalAreaCode?.let { areaRepository.findByIdOrNull(it) ?: throw ValidationException("$it geographical area code not found for agency $probationOfficeId") },
    payrollRegion = this.payrollRegionCode?.let { payrollRegionRepository.findByIdOrNull(it) ?: throw ValidationException("$it payroll region code not found for agency $probationOfficeId") },
    localAuthority = this.localAuthorityCode?.let { localAuthorityRepository.findByIdOrNull(it) ?: throw ValidationException("$it local authority code not found for agency $probationOfficeId") },
  )

  private fun ProbationOffice.update(agencyDto: LegacyAgencyDto) {
    this.name = agencyDto.name
    this.description = agencyDto.description
    this.contact = agencyDto.contact
    this.active = agencyDto.active
    this.inactiveDate = agencyDto.inactiveDate
    this.cjitCode = agencyDto.cjitCode
    this.accessibleAccess = agencyDto.accessibleAccess?.let { AccessibleAccess.valueOf(it.name) }
    this.area = agencyDto.areaCode?.let { areaRepository.findByIdOrNull(it) ?: throw ValidationException("$it area code not found for agency $probationOfficeId") }
    this.subarea = agencyDto.subareaCode?.let { subareaRepository.findByIdOrNull(it) ?: throw ValidationException("$it subarea code not found for agency $probationOfficeId") }
    this.region = agencyDto.regionCode?.let { regionRepository.findByIdOrNull(it) ?: throw ValidationException("$it region code not found for agency $probationOfficeId") }
    this.geographicalArea = agencyDto.geographicalAreaCode?.let { areaRepository.findByIdOrNull(it) ?: throw ValidationException("$it geographical area code not found for agency $probationOfficeId") }
    this.payrollRegion = agencyDto.payrollRegionCode?.let { payrollRegionRepository.findByIdOrNull(it) ?: throw ValidationException("$it payroll region code not found for agency $probationOfficeId") }
    this.localAuthority = agencyDto.localAuthorityCode?.let { localAuthorityRepository.findByIdOrNull(it) ?: throw ValidationException("$it local authority code not found for agency $probationOfficeId") }
  }

  private fun ProbationOffice.update(updateProbationOfficeDto: UpdateProbationOfficeDto) {
    this.name = updateProbationOfficeDto.probationOfficeName
    this.description = updateProbationOfficeDto.description
    this.contact = updateProbationOfficeDto.contact
    this.active = updateProbationOfficeDto.active
    this.accessibleAccess = updateProbationOfficeDto.accessibleAccess
    this.inactiveDate = updateProbationOfficeDto.inactiveDate
    this.cjitCode = updateProbationOfficeDto.cjitCode
    this.area = updateProbationOfficeDto.areaCode?.let { areaRepository.findByIdOrNull(it) ?: throw ValidationException("$it area code not found for probation office $probationOfficeId") }
    this.subarea = updateProbationOfficeDto.subareaCode?.let { subareaRepository.findByIdOrNull(it) ?: throw ValidationException("$it subarea code not found for probation office $probationOfficeId") }
    this.region = updateProbationOfficeDto.regionCode?.let { regionRepository.findByIdOrNull(it) ?: throw ValidationException("$it region code not found for probation office $probationOfficeId") }
    this.geographicalArea = updateProbationOfficeDto.geographicalAreaCode?.let { areaRepository.findByIdOrNull(it) ?: throw ValidationException("$it geographical area code not found for probation office $probationOfficeId") }
    this.payrollRegion = updateProbationOfficeDto.payrollRegionCode?.let { payrollRegionRepository.findByIdOrNull(it) ?: throw ValidationException("$it payroll region code not found for probation office $probationOfficeId") }
    this.localAuthority = updateProbationOfficeDto.localAuthorityCode?.let { localAuthorityRepository.findByIdOrNull(it) ?: throw ValidationException("$it local authority code not found for probation office $probationOfficeId") }
  }

  private fun CreateProbationOfficeDto.toProbationOffice() = ProbationOffice(
    probationOfficeId = this.probationOfficeId,
    name = this.probationOfficeName,
    description = this.description,
    contact = this.contact,
    active = this.active,
    accessibleAccess = this.accessibleAccess,
    inactiveDate = this.inactiveDate,
    cjitCode = this.cjitCode,
    area = this.areaCode?.let { areaRepository.findByIdOrNull(it) ?: throw ValidationException("$it area code not found for probation office $probationOfficeId") },
    subarea = this.subareaCode?.let { subareaRepository.findByIdOrNull(it) ?: throw ValidationException("$it subarea code not found for probation office $probationOfficeId") },
    region = this.regionCode?.let { regionRepository.findByIdOrNull(it) ?: throw ValidationException("$it region code not found for probation office $probationOfficeId") },
    geographicalArea = this.geographicalAreaCode?.let { areaRepository.findByIdOrNull(it) ?: throw ValidationException("$it geographical area code not found for probation office $probationOfficeId") },
    payrollRegion = this.payrollRegionCode?.let { payrollRegionRepository.findByIdOrNull(it) ?: throw ValidationException("$it payroll region code not found for probation office $probationOfficeId") },
    localAuthority = this.localAuthorityCode?.let { localAuthorityRepository.findByIdOrNull(it) ?: throw ValidationException("$it local authority code not found for probation office $probationOfficeId") },
    new = true,
  )

  private fun UpdateAddressDto.toAgencyAddress() = AgencyAddress(
    addressLine1 = this.addressLine1,
    addressLine2 = this.addressLine2,
    town = this.town,
    county = this.county,
    postcode = this.postcode,
    country = this.country,
  )
}
