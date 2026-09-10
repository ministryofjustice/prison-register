package uk.gov.justice.digital.hmpps.prisonregister.service

import jakarta.persistence.EntityNotFoundException
import jakarta.validation.ValidationException
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.prisonregister.exceptions.EmailAddressAlreadyExistsException
import uk.gov.justice.digital.hmpps.prisonregister.exceptions.PhoneNumberAlreadyExistsException
import uk.gov.justice.digital.hmpps.prisonregister.model.AgencyAddress
import uk.gov.justice.digital.hmpps.prisonregister.model.AreaRepository
import uk.gov.justice.digital.hmpps.prisonregister.model.EmailAddress
import uk.gov.justice.digital.hmpps.prisonregister.model.EmailAddressRepository
import uk.gov.justice.digital.hmpps.prisonregister.model.LocalAuthorityRepository
import uk.gov.justice.digital.hmpps.prisonregister.model.PayrollRegionRepository
import uk.gov.justice.digital.hmpps.prisonregister.model.PhoneNumber
import uk.gov.justice.digital.hmpps.prisonregister.model.PhoneNumberRepository
import uk.gov.justice.digital.hmpps.prisonregister.model.PoliceCustodySuite
import uk.gov.justice.digital.hmpps.prisonregister.model.PoliceCustodySuiteRepository
import uk.gov.justice.digital.hmpps.prisonregister.model.RegionRepository
import uk.gov.justice.digital.hmpps.prisonregister.resource.CreatePoliceCustodySuiteDto
import uk.gov.justice.digital.hmpps.prisonregister.resource.LegacyAgencyAddressDto
import uk.gov.justice.digital.hmpps.prisonregister.resource.LegacyAgencyDto
import uk.gov.justice.digital.hmpps.prisonregister.resource.LegacyAgencyEmailDto
import uk.gov.justice.digital.hmpps.prisonregister.resource.LegacyAgencyPhoneDto
import uk.gov.justice.digital.hmpps.prisonregister.resource.LegacyAgencyResponse
import uk.gov.justice.digital.hmpps.prisonregister.resource.LegacyAgencyType
import uk.gov.justice.digital.hmpps.prisonregister.resource.PoliceCustodySuiteDto
import uk.gov.justice.digital.hmpps.prisonregister.resource.UpdateAddressDto
import uk.gov.justice.digital.hmpps.prisonregister.resource.UpdateEmailAddressDto
import uk.gov.justice.digital.hmpps.prisonregister.resource.UpdatePhoneNumberDto
import uk.gov.justice.digital.hmpps.prisonregister.resource.UpdatePoliceCustodySuiteDto
import uk.gov.justice.digital.hmpps.prisonregister.resource.dto.AgencyAddressDto
import uk.gov.justice.digital.hmpps.prisonregister.resource.dto.AgencyEmailDto
import uk.gov.justice.digital.hmpps.prisonregister.resource.dto.AgencyPhoneDto
import uk.gov.justice.digital.hmpps.prisonregister.resource.dto.CodeDescription

@Service
@Transactional
class PoliceCustodySuiteService(
  private val policeCustodySuiteRepository: PoliceCustodySuiteRepository,
  private val areaRepository: AreaRepository,
  private val regionRepository: RegionRepository,
  private val payrollRegionRepository: PayrollRegionRepository,
  private val localAuthorityRepository: LocalAuthorityRepository,
  private val phoneNumberRepository: PhoneNumberRepository,
  private val emailAddressRepository: EmailAddressRepository,
) {
  fun deleteAll() {
    policeCustodySuiteRepository.deleteAll()
  }

  fun getAllIds(): List<String> = policeCustodySuiteRepository.findAll().map { it.policeCustodySuiteId }

  fun getAll(): List<PoliceCustodySuiteDto> = policeCustodySuiteRepository.findAll().map { it.toPoliceCustodySuiteDto() }

  fun findById(policeCustodySuiteId: String): PoliceCustodySuiteDto = policeCustodySuiteRepository.findByIdOrNull(policeCustodySuiteId)?.toPoliceCustodySuiteDto()
    ?: throw EntityNotFoundException("Police custody suite $policeCustodySuiteId not found")

  fun createPoliceCustodySuite(createPoliceCustodySuiteDto: CreatePoliceCustodySuiteDto): PoliceCustodySuiteDto {
    if (policeCustodySuiteRepository.existsById(createPoliceCustodySuiteDto.policeCustodySuiteId)) {
      throw ValidationException("Police custody suite ${createPoliceCustodySuiteDto.policeCustodySuiteId} already exists")
    }

    val policeCustodySuite = createPoliceCustodySuiteDto.toPoliceCustodySuite()
    policeCustodySuite.addresses += createPoliceCustodySuiteDto.addresses.map { it.toAgencyAddress() }
    policeCustodySuite.emailAddresses += createPoliceCustodySuiteDto.emailAddresses.map { EmailAddress(it.address) }
    policeCustodySuite.phoneNumbers += createPoliceCustodySuiteDto.phoneNumbers.map { PhoneNumber(it.number) }

    return policeCustodySuiteRepository.saveAndFlush(policeCustodySuite).toPoliceCustodySuiteDto()
  }

  fun updatePoliceCustodySuite(policeCustodySuiteId: String, updatePoliceCustodySuiteDto: UpdatePoliceCustodySuiteDto): PoliceCustodySuiteDto {
    val policeCustodySuite = policeCustodySuiteRepository.findByIdOrNull(policeCustodySuiteId) ?: throw EntityNotFoundException("Police custody suite $policeCustodySuiteId not found")
    policeCustodySuite.update(updatePoliceCustodySuiteDto)
    return policeCustodySuite.toPoliceCustodySuiteDto()
  }

  fun createPoliceCustodySuiteAddress(policeCustodySuiteId: String, updateAddressDto: UpdateAddressDto): AgencyAddressDto {
    val policeCustodySuite = policeCustodySuiteRepository.findByIdOrNull(policeCustodySuiteId) ?: throw EntityNotFoundException("Police custody suite $policeCustodySuiteId not found")

    val address = updateAddressDto.toAgencyAddress()
    policeCustodySuite.addresses += address
    policeCustodySuiteRepository.flush()

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

  fun updatePoliceCustodySuiteAddress(policeCustodySuiteId: String, addressId: Long, updateAddressDto: UpdateAddressDto): AgencyAddressDto {
    val policeCustodySuite = policeCustodySuiteRepository.findByIdOrNull(policeCustodySuiteId) ?: throw EntityNotFoundException("Police custody suite $policeCustodySuiteId not found")
    val address = policeCustodySuite.addresses.find { it.id == addressId } ?: throw EntityNotFoundException("Address $addressId not found for police custody suite $policeCustodySuiteId")

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

  fun createPoliceCustodySuitePhoneNumber(policeCustodySuiteId: String, updatePhoneNumberDto: UpdatePhoneNumberDto): AgencyPhoneDto {
    val policeCustodySuite = policeCustodySuiteRepository.findByIdOrNull(policeCustodySuiteId) ?: throw EntityNotFoundException("Police custody suite $policeCustodySuiteId not found")

    // phone number is unique across all establishments, could be a bit restrictive but going with db constraints
    if (phoneNumberRepository.getByValue(updatePhoneNumberDto.number) != null) {
      throw PhoneNumberAlreadyExistsException(updatePhoneNumberDto.number)
    }

    val phoneNumber = PhoneNumber(updatePhoneNumberDto.number)
    policeCustodySuite.phoneNumbers += phoneNumber
    policeCustodySuiteRepository.flush()

    return AgencyPhoneDto(
      id = phoneNumber.id,
      number = phoneNumber.value,
    )
  }

  fun updatePoliceCustodySuitePhoneNumber(policeCustodySuiteId: String, phoneNumberId: Long, updatePhoneNumberDto: UpdatePhoneNumberDto): AgencyPhoneDto {
    val policeCustodySuite = policeCustodySuiteRepository.findByIdOrNull(policeCustodySuiteId) ?: throw EntityNotFoundException("Police custody suite $policeCustodySuiteId not found")
    val phoneNumber = policeCustodySuite.phoneNumbers.find { it.id == phoneNumberId } ?: throw EntityNotFoundException("Phone number $phoneNumberId not found for police custody suite $policeCustodySuiteId")

    phoneNumber.value = updatePhoneNumberDto.number

    return AgencyPhoneDto(
      id = phoneNumber.id,
      number = phoneNumber.value,
    )
  }

  fun createPoliceCustodySuiteEmailAddress(policeCustodySuiteId: String, updateEmailAddressDto: UpdateEmailAddressDto): AgencyEmailDto {
    val policeCustodySuite = policeCustodySuiteRepository.findByIdOrNull(policeCustodySuiteId) ?: throw EntityNotFoundException("Police custody suite $policeCustodySuiteId not found")

    // email address is unique across all establishments
    if (emailAddressRepository.getByValue(updateEmailAddressDto.address) != null) {
      throw EmailAddressAlreadyExistsException(updateEmailAddressDto.address)
    }

    val emailAddress = EmailAddress(updateEmailAddressDto.address)
    policeCustodySuite.emailAddresses += emailAddress
    policeCustodySuiteRepository.flush()

    return AgencyEmailDto(
      id = emailAddress.id,
      address = emailAddress.value,
    )
  }

  fun updatePoliceCustodySuiteEmailAddress(policeCustodySuiteId: String, emailAddressId: Long, updateEmailAddressDto: UpdateEmailAddressDto): AgencyEmailDto {
    val policeCustodySuite = policeCustodySuiteRepository.findByIdOrNull(policeCustodySuiteId) ?: throw EntityNotFoundException("Police custody suite $policeCustodySuiteId not found")
    val emailAddress = policeCustodySuite.emailAddresses.find { it.id == emailAddressId } ?: throw EntityNotFoundException("Email address $emailAddressId not found for police custody suite $policeCustodySuiteId")

    emailAddress.value = updateEmailAddressDto.address

    return AgencyEmailDto(
      id = emailAddress.id,
      address = emailAddress.value,
    )
  }

  private fun PoliceCustodySuite.toPoliceCustodySuiteDto() = PoliceCustodySuiteDto(
    policeCustodySuiteId = this.policeCustodySuiteId,
    policeCustodySuiteName = this.name,
    description = this.description,
    active = this.active,
    inactiveDate = this.inactiveDate,
    cjitCode = this.cjitCode,
    area = this.area?.let { area -> CodeDescription(area.code, area.description) },
    region = this.region?.let { region -> CodeDescription(region.code, region.description) },
    geographicalArea = this.geographicalArea?.let { area -> CodeDescription(area.code, area.description) },
    payrollRegion = this.payrollRegion?.let { pr -> CodeDescription(pr.code, pr.description) },
    localAuthority = this.localAuthority?.let { localAuthority -> CodeDescription(localAuthority.code, localAuthority.description) },
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

  fun tryFindById(agencyId: String): LegacyAgencyDto? = policeCustodySuiteRepository.findByIdOrNull(agencyId)?.let { pcs ->
    LegacyAgencyDto(
      agencyType = LegacyAgencyType.POLICE_CUSTODY_SUITE,
      name = pcs.name,
      description = pcs.description,
      active = pcs.active,
      inactiveDate = pcs.inactiveDate,
      cjitCode = pcs.cjitCode,
      areaCode = pcs.area?.code,
      regionCode = pcs.region?.code,
      geographicalAreaCode = pcs.geographicalArea?.code,
      payrollRegionCode = pcs.payrollRegion?.code,
      localAuthorityCode = pcs.localAuthority?.code,
      courtTypeCode = null,
      accessibleAccess = null,
      contact = null,
      addresses = pcs.addresses.map { LegacyAgencyAddressDto(it.addressLine1, it.addressLine2, it.town, it.county, it.postcode, it.country) },
      emailAddresses = pcs.emailAddresses.map { LegacyAgencyEmailDto(it.value) },
      phoneNumbers = pcs.phoneNumbers.map { LegacyAgencyPhoneDto(it.value) },
    )
  }

  fun createOrUpdatePoliceCustodySuiteFromLegacyData(policeCustodySuiteId: String, agencyDto: LegacyAgencyDto): LegacyAgencyResponse = policeCustodySuiteRepository.findByIdOrNull(policeCustodySuiteId)?.let { policeCustodySuite ->
    policeCustodySuite.update(agencyDto)
    if (policeCustodySuite.addresses.size == 1 && agencyDto.addresses.size == 1) {
      policeCustodySuite.addresses[0].update(agencyDto.addresses[0])
    } else {
      policeCustodySuite.addresses.clear()
      policeCustodySuite.addresses += agencyDto.addresses.map { it.toAgencyAddress() }
    }

    policeCustodySuite.phoneNumbers.updatePhoneNumberFrom(agencyDto.phoneNumbers)
    policeCustodySuite.emailAddresses.updateEmailAddressFrom(agencyDto.emailAddresses)

    LegacyAgencyResponse(updated = true)
  } ?: let {
    val policeCustodySuite = agencyDto.toPoliceCustodySuite(policeCustodySuiteId)
    policeCustodySuite.addresses += agencyDto.addresses.map { it.toAgencyAddress() }
    policeCustodySuite.phoneNumbers += agencyDto.phoneNumbers.map { it.toAgencyPhone() }
    policeCustodySuite.emailAddresses += agencyDto.emailAddresses.map { it.toAgencyEmail() }
    policeCustodySuiteRepository.saveAndFlush(policeCustodySuite)
    LegacyAgencyResponse(updated = false)
  }

  private fun LegacyAgencyDto.toPoliceCustodySuite(policeCustodySuiteId: String) = PoliceCustodySuite(
    policeCustodySuiteId = policeCustodySuiteId,
    name = this.name,
    description = this.description,
    active = this.active,
    inactiveDate = this.inactiveDate,
    cjitCode = this.cjitCode,
    area = this.areaCode?.let { areaRepository.findByIdOrNull(it) ?: throw ValidationException("$it area code not found for agency $policeCustodySuiteId") },
    region = this.regionCode?.let { regionRepository.findByIdOrNull(it) ?: throw ValidationException("$it region code not found for agency $policeCustodySuiteId") },
    geographicalArea = this.geographicalAreaCode?.let { areaRepository.findByIdOrNull(it) ?: throw ValidationException("$it geographical area code not found for agency $policeCustodySuiteId") },
    payrollRegion = this.payrollRegionCode?.let { payrollRegionRepository.findByIdOrNull(it) ?: throw ValidationException("$it payroll region code not found for agency $policeCustodySuiteId") },
    localAuthority = this.localAuthorityCode?.let { localAuthorityRepository.findByIdOrNull(it) ?: throw ValidationException("$it local authority code not found for agency $policeCustodySuiteId") },
  )

  private fun PoliceCustodySuite.update(agencyDto: LegacyAgencyDto) {
    this.name = agencyDto.name
    this.description = agencyDto.description
    this.active = agencyDto.active
    this.inactiveDate = agencyDto.inactiveDate
    this.cjitCode = agencyDto.cjitCode
    this.area = agencyDto.areaCode?.let { areaRepository.findByIdOrNull(it) ?: throw ValidationException("$it area code not found for agency $policeCustodySuiteId") }
    this.region = agencyDto.regionCode?.let { regionRepository.findByIdOrNull(it) ?: throw ValidationException("$it region code not found for agency $policeCustodySuiteId") }
    this.geographicalArea = agencyDto.geographicalAreaCode?.let { areaRepository.findByIdOrNull(it) ?: throw ValidationException("$it geographical area code not found for agency $policeCustodySuiteId") }
    this.payrollRegion = agencyDto.payrollRegionCode?.let { payrollRegionRepository.findByIdOrNull(it) ?: throw ValidationException("$it payroll region code not found for agency $policeCustodySuiteId") }
    this.localAuthority = agencyDto.localAuthorityCode?.let { localAuthorityRepository.findByIdOrNull(it) ?: throw ValidationException("$it local authority code not found for agency $policeCustodySuiteId") }
  }

  private fun PoliceCustodySuite.update(updatePoliceCustodySuiteDto: UpdatePoliceCustodySuiteDto) {
    this.name = updatePoliceCustodySuiteDto.policeCustodySuiteName
    this.description = updatePoliceCustodySuiteDto.description
    this.active = updatePoliceCustodySuiteDto.active
    this.inactiveDate = updatePoliceCustodySuiteDto.inactiveDate
    this.cjitCode = updatePoliceCustodySuiteDto.cjitCode
    this.area = updatePoliceCustodySuiteDto.areaCode?.let { areaRepository.findByIdOrNull(it) ?: throw ValidationException("$it area code not found for police custody suite $policeCustodySuiteId") }
    this.region = updatePoliceCustodySuiteDto.regionCode?.let { regionRepository.findByIdOrNull(it) ?: throw ValidationException("$it region code not found for police custody suite $policeCustodySuiteId") }
    this.geographicalArea = updatePoliceCustodySuiteDto.geographicalAreaCode?.let { areaRepository.findByIdOrNull(it) ?: throw ValidationException("$it geographical area code not found for police custody suite $policeCustodySuiteId") }
    this.payrollRegion = updatePoliceCustodySuiteDto.payrollRegionCode?.let { payrollRegionRepository.findByIdOrNull(it) ?: throw ValidationException("$it payroll region code not found for police custody suite $policeCustodySuiteId") }
    this.localAuthority = updatePoliceCustodySuiteDto.localAuthorityCode?.let { localAuthorityRepository.findByIdOrNull(it) ?: throw ValidationException("$it local authority code not found for police custody suite $policeCustodySuiteId") }
  }

  private fun CreatePoliceCustodySuiteDto.toPoliceCustodySuite() = PoliceCustodySuite(
    policeCustodySuiteId = this.policeCustodySuiteId,
    name = this.policeCustodySuiteName,
    description = this.description,
    active = this.active,
    inactiveDate = this.inactiveDate,
    cjitCode = this.cjitCode,
    area = this.areaCode?.let { areaRepository.findByIdOrNull(it) ?: throw ValidationException("$it area code not found for police custody suite $policeCustodySuiteId") },
    region = this.regionCode?.let { regionRepository.findByIdOrNull(it) ?: throw ValidationException("$it region code not found for police custody suite $policeCustodySuiteId") },
    geographicalArea = this.geographicalAreaCode?.let { areaRepository.findByIdOrNull(it) ?: throw ValidationException("$it geographical area code not found for police custody suite $policeCustodySuiteId") },
    payrollRegion = this.payrollRegionCode?.let { payrollRegionRepository.findByIdOrNull(it) ?: throw ValidationException("$it payroll region code not found for police custody suite $policeCustodySuiteId") },
    localAuthority = this.localAuthorityCode?.let { localAuthorityRepository.findByIdOrNull(it) ?: throw ValidationException("$it local authority code not found for police custody suite $policeCustodySuiteId") },
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
