package uk.gov.justice.digital.hmpps.prisonregister.service

import com.microsoft.applicationinsights.TelemetryClient
import jakarta.persistence.EntityNotFoundException
import jakarta.validation.ValidationException
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.prisonregister.model.AccessibleAccess
import uk.gov.justice.digital.hmpps.prisonregister.model.AgencyAddress
import uk.gov.justice.digital.hmpps.prisonregister.model.AreaRepository
import uk.gov.justice.digital.hmpps.prisonregister.model.EmailAddress
import uk.gov.justice.digital.hmpps.prisonregister.model.LocalAuthorityRepository
import uk.gov.justice.digital.hmpps.prisonregister.model.OtherAgency
import uk.gov.justice.digital.hmpps.prisonregister.model.OtherAgencyFilter
import uk.gov.justice.digital.hmpps.prisonregister.model.OtherAgencyRepository
import uk.gov.justice.digital.hmpps.prisonregister.model.OtherAgencyType
import uk.gov.justice.digital.hmpps.prisonregister.model.PayrollRegionRepository
import uk.gov.justice.digital.hmpps.prisonregister.model.PhoneNumber
import uk.gov.justice.digital.hmpps.prisonregister.model.RegionRepository
import uk.gov.justice.digital.hmpps.prisonregister.resource.CreateOtherAgencyDto
import uk.gov.justice.digital.hmpps.prisonregister.resource.LegacyAccessibleAccess
import uk.gov.justice.digital.hmpps.prisonregister.resource.LegacyAgencyAddressDto
import uk.gov.justice.digital.hmpps.prisonregister.resource.LegacyAgencyDto
import uk.gov.justice.digital.hmpps.prisonregister.resource.LegacyAgencyEmailDto
import uk.gov.justice.digital.hmpps.prisonregister.resource.LegacyAgencyPhoneDto
import uk.gov.justice.digital.hmpps.prisonregister.resource.LegacyAgencyResponse
import uk.gov.justice.digital.hmpps.prisonregister.resource.LegacyAgencyType
import uk.gov.justice.digital.hmpps.prisonregister.resource.OtherAgencyDto
import uk.gov.justice.digital.hmpps.prisonregister.resource.UpdateAddressDto
import uk.gov.justice.digital.hmpps.prisonregister.resource.UpdateEmailAddressDto
import uk.gov.justice.digital.hmpps.prisonregister.resource.UpdateOtherAgencyDto
import uk.gov.justice.digital.hmpps.prisonregister.resource.UpdatePhoneNumberDto
import uk.gov.justice.digital.hmpps.prisonregister.resource.dto.AgencyAddressDto
import uk.gov.justice.digital.hmpps.prisonregister.resource.dto.AgencyEmailDto
import uk.gov.justice.digital.hmpps.prisonregister.resource.dto.AgencyPhoneDto
import uk.gov.justice.digital.hmpps.prisonregister.resource.dto.CodeDescription

@Service
@Transactional
class OtherAgencyService(
  private val otherAgencyRepository: OtherAgencyRepository,
  private val areaRepository: AreaRepository,
  private val regionRepository: RegionRepository,
  private val payrollRegionRepository: PayrollRegionRepository,
  private val localAuthorityRepository: LocalAuthorityRepository,
  private val telemetryClient: TelemetryClient,
) {
  fun deleteAll() {
    otherAgencyRepository.deleteAll()
  }

  fun getAllIds(): List<String> = otherAgencyRepository.findAll().map { it.agencyId }

  fun getAll(active: Boolean?, textSearch: String?, otherAgencyTypeCodes: List<OtherAgencyType>?): List<OtherAgencyDto> = otherAgencyRepository.findAll(OtherAgencyFilter(active, textSearch, otherAgencyTypeCodes)).map { it.toOtherAgencyDto() }

  fun findById(agencyId: String): OtherAgencyDto = otherAgencyRepository.findByIdOrNull(agencyId)?.toOtherAgencyDto()
    ?: throw EntityNotFoundException("Agency $agencyId not found")

  fun createAgency(createAgencyDto: CreateOtherAgencyDto): OtherAgencyDto {
    if (otherAgencyRepository.existsById(createAgencyDto.agencyId)) {
      throw ValidationException("Agency ${createAgencyDto.agencyId} already exists")
    }

    val agency = createAgencyDto.toAgency()
    // TODO add validation once constraints known
    agency.addresses += createAgencyDto.addresses.map { it.toAgencyAddress() }
    agency.emailAddresses += createAgencyDto.emailAddresses.map { EmailAddress(it.address) }
    agency.phoneNumbers += createAgencyDto.phoneNumbers.map { PhoneNumber(it.number) }

    val savedAgency = otherAgencyRepository.saveAndFlush(agency)

    telemetryClient.trackEvent(
      "agency-created",
      mapOf(
        "agencyId" to savedAgency.agencyId,
      ),
      null,
    )

    return savedAgency.toOtherAgencyDto()
  }

  fun updateAgency(agencyId: String, updateAgencyDto: UpdateOtherAgencyDto): OtherAgencyDto {
    val agency = otherAgencyRepository.findByIdOrNull(agencyId) ?: throw EntityNotFoundException("Agency $agencyId not found")
    agency.update(updateAgencyDto)

    telemetryClient.trackEvent(
      "agency-updated",
      mapOf(
        "agencyId" to agency.agencyId,
      ),
      null,
    )

    return agency.toOtherAgencyDto()
  }

  fun deleteAgency(agencyId: String) {
    val agency = otherAgencyRepository.findByIdOrNull(agencyId) ?: throw EntityNotFoundException("Agency $agencyId not found")
    otherAgencyRepository.delete(agency)

    telemetryClient.trackEvent(
      "agency-deleted",
      mapOf(
        "agencyId" to agency.agencyId,
      ),
      null,
    )
  }

  fun createAgencyAddress(agencyId: String, updateAddressDto: UpdateAddressDto): AgencyAddressDto {
    val agency = otherAgencyRepository.findByIdOrNull(agencyId) ?: throw EntityNotFoundException("Agency $agencyId not found")

    val address = updateAddressDto.toAgencyAddress()
    agency.addresses += address
    otherAgencyRepository.flush()

    telemetryClient.trackEvent(
      "agency-address-created",
      mapOf(
        "agencyId" to agency.agencyId,
        "addressId" to address.id.toString(),
      ),
      null,
    )

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

  fun updateAgencyAddress(agencyId: String, addressId: Long, updateAddressDto: UpdateAddressDto): AgencyAddressDto {
    val agency = otherAgencyRepository.findByIdOrNull(agencyId) ?: throw EntityNotFoundException("Agency $agencyId not found")
    val address = agency.addresses.find { it.id == addressId } ?: throw EntityNotFoundException("Address $addressId not found for agency $agencyId")

    with(updateAddressDto) {
      address.addressLine1 = addressLine1
      address.addressLine2 = addressLine2
      address.town = town
      address.county = county
      address.postcode = postcode
      address.country = country
    }

    telemetryClient.trackEvent(
      "agency-address-updated",
      mapOf(
        "agencyId" to agency.agencyId,
        "addressId" to address.id.toString(),
      ),
      null,
    )

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

  fun deleteAgencyAddress(agencyId: String, addressId: Long): AgencyAddressDto {
    val agency = otherAgencyRepository.findByIdOrNull(agencyId) ?: throw EntityNotFoundException("Agency $agencyId not found")
    val address = agency.addresses.find { it.id == addressId } ?: throw EntityNotFoundException("Address $addressId not found for agency $agencyId")

    agency.addresses.remove(address)

    telemetryClient.trackEvent(
      "agency-address-deleted",
      mapOf(
        "agencyId" to agency.agencyId,
        "addressId" to address.id.toString(),
      ),
      null,
    )

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

  fun createAgencyPhoneNumber(agencyId: String, updatePhoneNumberDto: UpdatePhoneNumberDto): AgencyPhoneDto {
    val agency = otherAgencyRepository.findByIdOrNull(agencyId) ?: throw EntityNotFoundException("Agency $agencyId not found")

    val phoneNumber = PhoneNumber(updatePhoneNumberDto.number)
    agency.phoneNumbers += phoneNumber
    otherAgencyRepository.flush()

    telemetryClient.trackEvent(
      "agency-phone-number-created",
      mapOf(
        "agencyId" to agency.agencyId,
        "phoneNumberId" to phoneNumber.id.toString(),
      ),
      null,
    )

    return AgencyPhoneDto(
      id = phoneNumber.id,
      number = phoneNumber.value,
    )
  }

  fun updateAgencyPhoneNumber(agencyId: String, phoneNumberId: Long, updatePhoneNumberDto: UpdatePhoneNumberDto): AgencyPhoneDto {
    val agency = otherAgencyRepository.findByIdOrNull(agencyId) ?: throw EntityNotFoundException("Agency $agencyId not found")
    val phoneNumber = agency.phoneNumbers.find { it.id == phoneNumberId } ?: throw EntityNotFoundException("Phone number $phoneNumberId not found for agency $agencyId")

    phoneNumber.value = updatePhoneNumberDto.number

    telemetryClient.trackEvent(
      "agency-phone-number-updated",
      mapOf(
        "agencyId" to agency.agencyId,
        "phoneNumberId" to phoneNumber.id.toString(),
      ),
      null,
    )

    return AgencyPhoneDto(
      id = phoneNumber.id,
      number = phoneNumber.value,
    )
  }

  fun deleteAgencyPhoneNumber(agencyId: String, phoneNumberId: Long): AgencyPhoneDto {
    val agency = otherAgencyRepository.findByIdOrNull(agencyId) ?: throw EntityNotFoundException("Agency $agencyId not found")
    val phoneNumber = agency.phoneNumbers.find { it.id == phoneNumberId } ?: throw EntityNotFoundException("Phone number $phoneNumberId not found for agency $agencyId")

    agency.phoneNumbers.remove(phoneNumber)

    telemetryClient.trackEvent(
      "agency-phone-number-deleted",
      mapOf(
        "agencyId" to agency.agencyId,
        "phoneNumberId" to phoneNumber.id.toString(),
      ),
      null,
    )

    return AgencyPhoneDto(
      id = phoneNumber.id,
      number = phoneNumber.value,
    )
  }

  fun createAgencyEmailAddress(agencyId: String, updateEmailAddressDto: UpdateEmailAddressDto): AgencyEmailDto {
    val agency = otherAgencyRepository.findByIdOrNull(agencyId) ?: throw EntityNotFoundException("Agency $agencyId not found")

    val emailAddress = EmailAddress(updateEmailAddressDto.address)
    agency.emailAddresses += emailAddress
    otherAgencyRepository.flush()

    telemetryClient.trackEvent(
      "agency-email-address-created",
      mapOf(
        "agencyId" to agency.agencyId,
        "emailAddressId" to emailAddress.id.toString(),
      ),
      null,
    )

    return AgencyEmailDto(
      id = emailAddress.id,
      address = emailAddress.value,
    )
  }

  fun updateAgencyEmailAddress(agencyId: String, emailAddressId: Long, updateEmailAddressDto: UpdateEmailAddressDto): AgencyEmailDto {
    val agency = otherAgencyRepository.findByIdOrNull(agencyId) ?: throw EntityNotFoundException("Agency $agencyId not found")
    val emailAddress = agency.emailAddresses.find { it.id == emailAddressId } ?: throw EntityNotFoundException("Email address $emailAddressId not found for agency $agencyId")

    emailAddress.value = updateEmailAddressDto.address

    telemetryClient.trackEvent(
      "agency-email-address-updated",
      mapOf(
        "agencyId" to agency.agencyId,
        "emailAddressId" to emailAddress.id.toString(),
      ),
      null,
    )

    return AgencyEmailDto(
      id = emailAddress.id,
      address = emailAddress.value,
    )
  }

  fun deleteAgencyEmailAddress(agencyId: String, emailAddressId: Long): AgencyEmailDto {
    val agency = otherAgencyRepository.findByIdOrNull(agencyId) ?: throw EntityNotFoundException("Agency $agencyId not found")
    val emailAddress = agency.emailAddresses.find { it.id == emailAddressId } ?: throw EntityNotFoundException("Email address $emailAddressId not found for agency $agencyId")

    agency.emailAddresses.remove(emailAddress)

    telemetryClient.trackEvent(
      "agency-email-address-deleted",
      mapOf(
        "agencyId" to agency.agencyId,
        "emailAddressId" to emailAddress.id.toString(),
      ),
      null,
    )

    return AgencyEmailDto(
      id = emailAddress.id,
      address = emailAddress.value,
    )
  }

  private fun OtherAgency.toOtherAgencyDto() = OtherAgencyDto(
    agencyId = this.agencyId,
    agencyName = this.name,
    description = this.description,
    active = this.active,
    accessibleAccess = this.accessibleAccess?.name,
    agencyType = this.otherAgencyType.name,
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

  fun tryFindById(agencyId: String): LegacyAgencyDto? = otherAgencyRepository.findByIdOrNull(agencyId)?.let { agency ->
    LegacyAgencyDto(
      agencyType = LegacyAgencyType.valueOf(agency.otherAgencyType.name),
      name = agency.name,
      description = agency.description,
      active = agency.active,
      inactiveDate = agency.inactiveDate,
      cjitCode = agency.cjitCode,
      areaCode = agency.area?.code,
      regionCode = agency.region?.code,
      geographicalAreaCode = agency.geographicalArea?.code,
      payrollRegionCode = agency.payrollRegion?.code,
      localAuthorityCode = agency.localAuthority?.code,
      courtTypeCode = null,
      accessibleAccess = agency.accessibleAccess?.let { runCatching { LegacyAccessibleAccess.valueOf(it.name) }.getOrNull() },
      contact = null,
      addresses = agency.addresses.map { LegacyAgencyAddressDto(it.addressLine1, it.addressLine2, it.town, it.county, it.postcode, it.country) },
      emailAddresses = agency.emailAddresses.map { LegacyAgencyEmailDto(it.value) },
      phoneNumbers = agency.phoneNumbers.map { LegacyAgencyPhoneDto(it.value) },
    )
  }

  fun createOrUpdateAgencyFromLegacyData(agencyId: String, agencyDto: LegacyAgencyDto): LegacyAgencyResponse = otherAgencyRepository.findByIdOrNull(agencyId)?.let { agency ->
    agency.update(agencyDto)
    if (agency.addresses.size == 1 && agencyDto.addresses.size == 1) {
      agency.addresses[0].update(agencyDto.addresses[0])
    } else {
      agency.addresses.clear()
      agency.addresses += agencyDto.addresses.map { it.toAgencyAddress() }
    }

    agency.phoneNumbers.updatePhoneNumberFrom(agencyDto.phoneNumbers)
    agency.emailAddresses.updateEmailAddressFrom(agencyDto.emailAddresses)

    LegacyAgencyResponse(updated = true)
  } ?: let {
    val agency = agencyDto.toAgency(agencyId)
    agency.addresses += agencyDto.addresses.map { it.toAgencyAddress() }
    agency.phoneNumbers += agencyDto.phoneNumbers.map { it.toAgencyPhone() }
    agency.emailAddresses += agencyDto.emailAddresses.map { it.toAgencyEmail() }
    otherAgencyRepository.saveAndFlush(agency)
    LegacyAgencyResponse(updated = false)
  }

  private fun LegacyAgencyDto.toAgency(agencyId: String) = OtherAgency(
    agencyId = agencyId,
    name = this.name,
    description = this.description,
    active = this.active,
    accessibleAccess = this.accessibleAccess?.let { AccessibleAccess.valueOf(it.name) },
    otherAgencyType = runCatching { OtherAgencyType.valueOf(this.agencyType.name) }.getOrElse { throw ValidationException("${this.agencyType} agency type not supported for agency $agencyId") },
    inactiveDate = this.inactiveDate,
    cjitCode = this.cjitCode,
    area = this.areaCode?.let { areaRepository.findByIdOrNull(it) ?: throw ValidationException("$it area code not found for agency $agencyId") },
    region = this.regionCode?.let { regionRepository.findByIdOrNull(it) ?: throw ValidationException("$it region code not found for agency $agencyId") },
    geographicalArea = this.geographicalAreaCode?.let { areaRepository.findByIdOrNull(it) ?: throw ValidationException("$it geographical area code not found for agency $agencyId") },
    payrollRegion = this.payrollRegionCode?.let { payrollRegionRepository.findByIdOrNull(it) ?: throw ValidationException("$it payroll region code not found for agency $agencyId") },
    localAuthority = this.localAuthorityCode?.let { localAuthorityRepository.findByIdOrNull(it) ?: throw ValidationException("$it local authority code not found for agency $agencyId") },
  )

  private fun OtherAgency.update(agencyDto: LegacyAgencyDto) {
    this.name = agencyDto.name
    this.description = agencyDto.description
    this.active = agencyDto.active
    this.accessibleAccess = agencyDto.accessibleAccess?.let { AccessibleAccess.valueOf(it.name) }
    this.otherAgencyType = runCatching { OtherAgencyType.valueOf(agencyDto.agencyType.name) }.getOrElse { throw ValidationException("${agencyDto.agencyType} agency type not supported for agency $agencyId") }
    this.inactiveDate = agencyDto.inactiveDate
    this.cjitCode = agencyDto.cjitCode
    this.area = agencyDto.areaCode?.let { areaRepository.findByIdOrNull(it) ?: throw ValidationException("$it area code not found for agency $agencyId") }
    this.region = agencyDto.regionCode?.let { regionRepository.findByIdOrNull(it) ?: throw ValidationException("$it region code not found for agency $agencyId") }
    this.geographicalArea = agencyDto.geographicalAreaCode?.let { areaRepository.findByIdOrNull(it) ?: throw ValidationException("$it geographical area code not found for agency $agencyId") }
    this.payrollRegion = agencyDto.payrollRegionCode?.let { payrollRegionRepository.findByIdOrNull(it) ?: throw ValidationException("$it payroll region code not found for agency $agencyId") }
    this.localAuthority = agencyDto.localAuthorityCode?.let { localAuthorityRepository.findByIdOrNull(it) ?: throw ValidationException("$it local authority code not found for agency $agencyId") }
  }

  private fun OtherAgency.update(updateAgencyDto: UpdateOtherAgencyDto) {
    this.name = updateAgencyDto.agencyName
    this.description = updateAgencyDto.description
    this.active = updateAgencyDto.active
    this.accessibleAccess = updateAgencyDto.accessibleAccess
    this.otherAgencyType = updateAgencyDto.otherAgencyType
    this.inactiveDate = updateAgencyDto.inactiveDate
    this.cjitCode = updateAgencyDto.cjitCode
    this.area = updateAgencyDto.areaCode?.let { areaRepository.findByIdOrNull(it) ?: throw ValidationException("$it area code not found for agency $agencyId") }
    this.region = updateAgencyDto.regionCode?.let { regionRepository.findByIdOrNull(it) ?: throw ValidationException("$it region code not found for agency $agencyId") }
    this.geographicalArea = updateAgencyDto.geographicalAreaCode?.let { areaRepository.findByIdOrNull(it) ?: throw ValidationException("$it geographical area code not found for agency $agencyId") }
    this.payrollRegion = updateAgencyDto.payrollRegionCode?.let { payrollRegionRepository.findByIdOrNull(it) ?: throw ValidationException("$it payroll region code not found for agency $agencyId") }
    this.localAuthority = updateAgencyDto.localAuthorityCode?.let { localAuthorityRepository.findByIdOrNull(it) ?: throw ValidationException("$it local authority code not found for agency $agencyId") }
  }

  private fun CreateOtherAgencyDto.toAgency() = OtherAgency(
    agencyId = this.agencyId,
    name = this.agencyName,
    description = this.description,
    active = this.active,
    accessibleAccess = this.accessibleAccess,
    otherAgencyType = this.otherAgencyType,
    inactiveDate = this.inactiveDate,
    cjitCode = this.cjitCode,
    area = this.areaCode?.let { areaRepository.findByIdOrNull(it) ?: throw ValidationException("$it area code not found for agency $agencyId") },
    region = this.regionCode?.let { regionRepository.findByIdOrNull(it) ?: throw ValidationException("$it region code not found for agency $agencyId") },
    geographicalArea = this.geographicalAreaCode?.let { areaRepository.findByIdOrNull(it) ?: throw ValidationException("$it geographical area code not found for agency $agencyId") },
    payrollRegion = this.payrollRegionCode?.let { payrollRegionRepository.findByIdOrNull(it) ?: throw ValidationException("$it payroll region code not found for agency $agencyId") },
    localAuthority = this.localAuthorityCode?.let { localAuthorityRepository.findByIdOrNull(it) ?: throw ValidationException("$it local authority code not found for agency $agencyId") },
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
