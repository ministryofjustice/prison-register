package uk.gov.justice.digital.hmpps.prisonregister.service

import jakarta.persistence.EntityNotFoundException
import jakarta.validation.ValidationException
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.prisonregister.model.AccessibleAccess
import uk.gov.justice.digital.hmpps.prisonregister.model.AgencyAddress
import uk.gov.justice.digital.hmpps.prisonregister.model.AreaRepository
import uk.gov.justice.digital.hmpps.prisonregister.model.Court
import uk.gov.justice.digital.hmpps.prisonregister.model.CourtRepository
import uk.gov.justice.digital.hmpps.prisonregister.model.CourtTypeRepository
import uk.gov.justice.digital.hmpps.prisonregister.model.EmailAddress
import uk.gov.justice.digital.hmpps.prisonregister.model.LocalAuthorityRepository
import uk.gov.justice.digital.hmpps.prisonregister.model.PayrollRegionRepository
import uk.gov.justice.digital.hmpps.prisonregister.model.PhoneNumber
import uk.gov.justice.digital.hmpps.prisonregister.model.RegionRepository
import uk.gov.justice.digital.hmpps.prisonregister.resource.CourtDto
import uk.gov.justice.digital.hmpps.prisonregister.resource.CreateCourtDto
import uk.gov.justice.digital.hmpps.prisonregister.resource.LegacyAccessibleAccess
import uk.gov.justice.digital.hmpps.prisonregister.resource.LegacyAgencyAddressDto
import uk.gov.justice.digital.hmpps.prisonregister.resource.LegacyAgencyDto
import uk.gov.justice.digital.hmpps.prisonregister.resource.LegacyAgencyEmailDto
import uk.gov.justice.digital.hmpps.prisonregister.resource.LegacyAgencyPhoneDto
import uk.gov.justice.digital.hmpps.prisonregister.resource.LegacyAgencyResponse
import uk.gov.justice.digital.hmpps.prisonregister.resource.LegacyAgencyType
import uk.gov.justice.digital.hmpps.prisonregister.resource.UpdateAddressDto
import uk.gov.justice.digital.hmpps.prisonregister.resource.UpdateCourtDto
import uk.gov.justice.digital.hmpps.prisonregister.resource.UpdateEmailAddressDto
import uk.gov.justice.digital.hmpps.prisonregister.resource.UpdatePhoneNumberDto
import uk.gov.justice.digital.hmpps.prisonregister.resource.dto.AgencyAddressDto
import uk.gov.justice.digital.hmpps.prisonregister.resource.dto.AgencyEmailDto
import uk.gov.justice.digital.hmpps.prisonregister.resource.dto.AgencyPhoneDto
import uk.gov.justice.digital.hmpps.prisonregister.resource.dto.CodeDescription

@Service
@Transactional
class CourtService(
  private val courtRepository: CourtRepository,
  private val areaRepository: AreaRepository,
  private val regionRepository: RegionRepository,
  private val courtTypeRepository: CourtTypeRepository,
  private val payrollRegionRepository: PayrollRegionRepository,
  private val localAuthorityRepository: LocalAuthorityRepository,
) {
  fun deleteAll() {
    courtRepository.deleteAll()
  }

  fun getAllIds(): List<String> = courtRepository.findAll().map { it.courtId }

  fun findById(courtId: String): CourtDto = courtRepository.findByIdOrNull(courtId)?.toCourtDto() ?: throw EntityNotFoundException("Court $courtId not found")

  fun getAll(): List<CourtDto> = courtRepository.findAll().map { it.toCourtDto() }

  @Transactional
  fun createCourt(createCourtDto: CreateCourtDto): CourtDto {
    if (courtRepository.existsById(createCourtDto.courtId)) {
      throw ValidationException("Court ${createCourtDto.courtId} already exists")
    }

    val court = createCourtDto.toCourt()
    court.addresses += createCourtDto.addresses.map { it.toAgencyAddress() }
    court.emailAddresses += createCourtDto.emailAddresses.map { EmailAddress(it.address) }
    court.phoneNumbers += createCourtDto.phoneNumbers.map { PhoneNumber(it.number) }

    return courtRepository.saveAndFlush(court).toCourtDto()
  }

  @Transactional
  fun updateCourt(courtId: String, updateCourtDto: UpdateCourtDto): CourtDto {
    val court = courtRepository.findByIdOrNull(courtId) ?: throw EntityNotFoundException("Court $courtId not found")
    court.update(updateCourtDto)
    return court.toCourtDto()
  }

  @Transactional
  fun updateCourtAddress(courtId: String, addressId: Long, updateAddressDto: UpdateAddressDto): AgencyAddressDto {
    val court = courtRepository.findByIdOrNull(courtId) ?: throw EntityNotFoundException("Court $courtId not found")
    val address = court.addresses.find { it.id == addressId } ?: throw EntityNotFoundException("Address $addressId not found for court $courtId")

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

  @Transactional
  fun updateCourtPhoneNumber(courtId: String, phoneNumberId: Long, updatePhoneNumberDto: UpdatePhoneNumberDto): AgencyPhoneDto {
    val court = courtRepository.findByIdOrNull(courtId) ?: throw EntityNotFoundException("Court $courtId not found")
    val phoneNumber = court.phoneNumbers.find { it.id == phoneNumberId } ?: throw EntityNotFoundException("Phone number $phoneNumberId not found for court $courtId")

    phoneNumber.value = updatePhoneNumberDto.number

    return AgencyPhoneDto(
      id = phoneNumber.id,
      number = phoneNumber.value,
    )
  }

  @Transactional
  fun updateCourtEmailAddress(courtId: String, emailAddressId: Long, updateEmailAddressDto: UpdateEmailAddressDto): AgencyEmailDto {
    val court = courtRepository.findByIdOrNull(courtId) ?: throw EntityNotFoundException("Court $courtId not found")
    val emailAddress = court.emailAddresses.find { it.id == emailAddressId } ?: throw EntityNotFoundException("Email address $emailAddressId not found for court $courtId")

    emailAddress.value = updateEmailAddressDto.address

    return AgencyEmailDto(
      id = emailAddress.id,
      address = emailAddress.value,
    )
  }

  fun tryFindById(agencyId: String): LegacyAgencyDto? = courtRepository.findByIdOrNull(agencyId)?.let { court ->
    LegacyAgencyDto(
      agencyType = LegacyAgencyType.COURT,
      name = court.name,
      description = court.description,
      active = court.active,
      inactiveDate = court.inactiveDate,
      cjitCode = court.cjitCode,
      areaCode = court.area?.code,
      regionCode = court.region?.code,
      geographicalAreaCode = court.geographicalArea?.code,
      payrollRegionCode = court.payrollRegion?.code,
      localAuthorityCode = court.localAuthority?.code,
      courtTypeCode = court.courtType.code.takeUnless { it == "UNK" },
      accessibleAccess = court.accessibleAccess?.let { runCatching { LegacyAccessibleAccess.valueOf(it.name) }.getOrNull() },
      contact = null,
      addresses = court.addresses.map { LegacyAgencyAddressDto(it.addressLine1, it.addressLine2, it.town, it.county, it.postcode, it.country) },
      emailAddresses = court.emailAddresses.map { LegacyAgencyEmailDto(it.value) },
      phoneNumbers = court.phoneNumbers.map { LegacyAgencyPhoneDto(it.value) },
    )
  }

  fun createOrUpdateCourtFromLegacyData(courtId: String, agencyDto: LegacyAgencyDto): LegacyAgencyResponse = courtRepository.findByIdOrNull(courtId)?.let { court ->
    court.update(agencyDto)
    // treat these as value objects and replace. If we are just updating a single address (vast majority of agencies will have at most one), keep the original entity and its ID
    if (court.addresses.size == 1 && agencyDto.addresses.size == 1) {
      court.addresses[0].update(agencyDto.addresses[0])
    } else {
      court.addresses.clear()
      court.addresses += agencyDto.addresses.map { it.toAgencyAddress() }
    }

    court.phoneNumbers.updatePhoneNumberFrom(agencyDto.phoneNumbers)
    court.emailAddresses.updateEmailAddressFrom(agencyDto.emailAddresses)

    LegacyAgencyResponse(updated = true)
  } ?: let {
    val court = agencyDto.toCourt(courtId)
    court.addresses += agencyDto.addresses.map { it.toAgencyAddress() }
    court.phoneNumbers += agencyDto.phoneNumbers.map { it.toAgencyPhone() }
    court.emailAddresses += agencyDto.emailAddresses.map { it.toAgencyEmail() }
    courtRepository.saveAndFlush(court)
    LegacyAgencyResponse(updated = false)
  }

  private fun Court.toCourtDto() = CourtDto(
    courtId = this.courtId,
    courtName = this.name,
    description = this.description,
    active = this.active,
    inactiveDate = this.inactiveDate,
    cjitCode = this.cjitCode,
    accessibleAccess = this.accessibleAccess?.name,
    area = this.area?.let { area -> CodeDescription(area.code, area.description) },
    region = this.region?.let { area -> CodeDescription(area.code, area.description) },
    geographicalArea = this.geographicalArea?.let { area -> CodeDescription(area.code, area.description) },
    localAuthority = this.localAuthority?.let { la -> CodeDescription(la.code, la.description) },
    payrollRegion = this.payrollRegion?.let { pr -> CodeDescription(pr.code, pr.description) },
    courtType = CodeDescription(this.courtType.code, this.courtType.description),
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

  private fun LegacyAgencyDto.toCourt(courtId: String) = Court(
    courtId = courtId,
    name = this.name,
    description = this.description,
    active = this.active,
    inactiveDate = this.inactiveDate,
    cjitCode = this.cjitCode,
    accessibleAccess = this.accessibleAccess?.let { AccessibleAccess.valueOf(it.name) },
    area = this.areaCode?.let { areaRepository.findByIdOrNull(it) ?: throw ValidationException("$it area code not found for agency $courtId") },
    region = this.regionCode?.let { regionRepository.findByIdOrNull(it) ?: throw ValidationException("$it region code not found for agency $courtId") },
    geographicalArea = this.geographicalAreaCode?.let { areaRepository.findByIdOrNull(it) ?: throw ValidationException("$it geographical area code not found for agency $courtId") },
    payrollRegion = this.payrollRegionCode?.let { payrollRegionRepository.findByIdOrNull(it) ?: throw ValidationException("$it payroll region code not found for agency $courtId") },
    localAuthority = this.localAuthorityCode?.let { localAuthorityRepository.findByIdOrNull(it) ?: throw ValidationException("$it local authority code not found for agency $courtId") },
    courtType = (this.courtTypeCode ?: "UNK").let { courtTypeRepository.findByIdOrNull(it) } ?: throw ValidationException("$courtTypeCode court type not found for agency $courtId"),
  )

  private fun Court.update(agencyDto: LegacyAgencyDto) {
    this.name = agencyDto.name
    this.description = agencyDto.description
    this.active = agencyDto.active
    this.inactiveDate = agencyDto.inactiveDate
    this.cjitCode = agencyDto.cjitCode
    this.accessibleAccess = agencyDto.accessibleAccess?.let { AccessibleAccess.valueOf(it.name) }
    this.area = agencyDto.areaCode?.let { areaRepository.findByIdOrNull(it) ?: throw ValidationException("$it area code not found for agency $courtId") }
    this.region = agencyDto.regionCode?.let { regionRepository.findByIdOrNull(it) ?: throw ValidationException("$it region code not found for agency $courtId") }
    this.geographicalArea = agencyDto.geographicalAreaCode?.let { areaRepository.findByIdOrNull(it) ?: throw ValidationException("$it geographical area code not found for agency $courtId") }
    this.payrollRegion = agencyDto.payrollRegionCode?.let { payrollRegionRepository.findByIdOrNull(it) ?: throw ValidationException("$it payroll region code not found for agency $courtId") }
    this.localAuthority = agencyDto.localAuthorityCode?.let { localAuthorityRepository.findByIdOrNull(it) ?: throw ValidationException("$it local authority code not found for agency $courtId") }
    this.courtType = (agencyDto.courtTypeCode ?: "UNK").let { courtTypeRepository.findByIdOrNull(it) } ?: throw ValidationException("${agencyDto.courtTypeCode} court type not found for agency $courtId")
  }

  private fun CreateCourtDto.toCourt() = Court(
    courtId = this.courtId,
    name = this.courtName,
    description = this.description,
    active = this.active,
    inactiveDate = this.inactiveDate,
    cjitCode = this.cjitCode,
    accessibleAccess = this.accessibleAccess,
    area = this.areaCode?.let { areaRepository.findByIdOrNull(it) ?: throw ValidationException("$it area code not found for court $courtId") },
    region = this.regionCode?.let { regionRepository.findByIdOrNull(it) ?: throw ValidationException("$it region code not found for court $courtId") },
    geographicalArea = this.geographicalAreaCode?.let { areaRepository.findByIdOrNull(it) ?: throw ValidationException("$it geographical area code not found for court $courtId") },
    payrollRegion = this.payrollRegionCode?.let { payrollRegionRepository.findByIdOrNull(it) ?: throw ValidationException("$it payroll region code not found for court $courtId") },
    localAuthority = this.localAuthorityCode?.let { localAuthorityRepository.findByIdOrNull(it) ?: throw ValidationException("$it local authority code not found for court $courtId") },
    courtType = courtTypeRepository.findByIdOrNull(this.courtTypeCode) ?: throw ValidationException("${this.courtTypeCode} court type not found for court $courtId"),
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

  private fun Court.update(updateCourtDto: UpdateCourtDto) {
    this.name = updateCourtDto.courtName
    this.description = updateCourtDto.description
    this.active = updateCourtDto.active
    this.inactiveDate = updateCourtDto.inactiveDate
    this.cjitCode = updateCourtDto.cjitCode
    this.accessibleAccess = updateCourtDto.accessibleAccess
    this.area = updateCourtDto.areaCode?.let { areaRepository.findByIdOrNull(it) ?: throw ValidationException("$it area code not found for court $courtId") }
    this.region = updateCourtDto.regionCode?.let { regionRepository.findByIdOrNull(it) ?: throw ValidationException("$it region code not found for court $courtId") }
    this.geographicalArea = updateCourtDto.geographicalAreaCode?.let { areaRepository.findByIdOrNull(it) ?: throw ValidationException("$it geographical area code not found for court $courtId") }
    this.payrollRegion = updateCourtDto.payrollRegionCode?.let { payrollRegionRepository.findByIdOrNull(it) ?: throw ValidationException("$it payroll region code not found for court $courtId") }
    this.localAuthority = updateCourtDto.localAuthorityCode?.let { localAuthorityRepository.findByIdOrNull(it) ?: throw ValidationException("$it local authority code not found for court $courtId") }
    this.courtType = courtTypeRepository.findByIdOrNull(updateCourtDto.courtTypeCode) ?: throw ValidationException("${updateCourtDto.courtTypeCode} court type not found for court $courtId")
  }
}
