package uk.gov.justice.digital.hmpps.prisonregister.service

import com.microsoft.applicationinsights.TelemetryClient
import jakarta.persistence.EntityNotFoundException
import jakarta.validation.ValidationException
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.prisonregister.exceptions.EmailAddressAlreadyExistsException
import uk.gov.justice.digital.hmpps.prisonregister.exceptions.PhoneNumberAlreadyExistsException
import uk.gov.justice.digital.hmpps.prisonregister.model.AccessibleAccess
import uk.gov.justice.digital.hmpps.prisonregister.model.Agency
import uk.gov.justice.digital.hmpps.prisonregister.model.AgencyAddress
import uk.gov.justice.digital.hmpps.prisonregister.model.AgencyRepository
import uk.gov.justice.digital.hmpps.prisonregister.model.AgencyType
import uk.gov.justice.digital.hmpps.prisonregister.model.AreaRepository
import uk.gov.justice.digital.hmpps.prisonregister.model.EmailAddress
import uk.gov.justice.digital.hmpps.prisonregister.model.EmailAddressRepository
import uk.gov.justice.digital.hmpps.prisonregister.model.LocalAuthorityRepository
import uk.gov.justice.digital.hmpps.prisonregister.model.PayrollRegionRepository
import uk.gov.justice.digital.hmpps.prisonregister.model.PhoneNumber
import uk.gov.justice.digital.hmpps.prisonregister.model.RegionRepository
import uk.gov.justice.digital.hmpps.prisonregister.resource.AgencyDto
import uk.gov.justice.digital.hmpps.prisonregister.resource.CreateAgencyDto
import uk.gov.justice.digital.hmpps.prisonregister.resource.LegacyAccessibleAccess
import uk.gov.justice.digital.hmpps.prisonregister.resource.LegacyAgencyAddressDto
import uk.gov.justice.digital.hmpps.prisonregister.resource.LegacyAgencyDto
import uk.gov.justice.digital.hmpps.prisonregister.resource.LegacyAgencyEmailDto
import uk.gov.justice.digital.hmpps.prisonregister.resource.LegacyAgencyPhoneDto
import uk.gov.justice.digital.hmpps.prisonregister.resource.LegacyAgencyResponse
import uk.gov.justice.digital.hmpps.prisonregister.resource.LegacyAgencyType
import uk.gov.justice.digital.hmpps.prisonregister.resource.UpdateAddressDto
import uk.gov.justice.digital.hmpps.prisonregister.resource.UpdateAgencyDto
import uk.gov.justice.digital.hmpps.prisonregister.resource.UpdateEmailAddressDto
import uk.gov.justice.digital.hmpps.prisonregister.resource.UpdatePhoneNumberDto
import uk.gov.justice.digital.hmpps.prisonregister.resource.dto.AgencyAddressDto
import uk.gov.justice.digital.hmpps.prisonregister.resource.dto.AgencyEmailDto
import uk.gov.justice.digital.hmpps.prisonregister.resource.dto.AgencyPhoneDto
import uk.gov.justice.digital.hmpps.prisonregister.resource.dto.CodeDescription

@Service
@Transactional
class AgencyService(
  private val agencyRepository: AgencyRepository,
  private val areaRepository: AreaRepository,
  private val regionRepository: RegionRepository,
  private val payrollRegionRepository: PayrollRegionRepository,
  private val localAuthorityRepository: LocalAuthorityRepository,
  private val emailAddressRepository: EmailAddressRepository,
  private val telemetryClient: TelemetryClient,
) {
  fun deleteAll() {
    agencyRepository.deleteAll()
  }

  fun getAllIds(): List<String> = agencyRepository.findAll().map { it.agencyId }

  fun getAll(): List<AgencyDto> = agencyRepository.findAll().map { it.toAgencyDto() }

  fun findById(agencyId: String): AgencyDto = agencyRepository.findByIdOrNull(agencyId)?.toAgencyDto()
    ?: throw EntityNotFoundException("Agency $agencyId not found")

  fun createAgency(createAgencyDto: CreateAgencyDto): AgencyDto {
    if (agencyRepository.existsById(createAgencyDto.agencyId)) {
      throw ValidationException("Agency ${createAgencyDto.agencyId} already exists")
    }

    val agency = createAgencyDto.toAgency()
    // TODO add validation once constraints known
    agency.addresses += createAgencyDto.addresses.map { it.toAgencyAddress() }
    agency.emailAddresses += createAgencyDto.emailAddresses.map { EmailAddress(it.address) }
    agency.phoneNumbers += createAgencyDto.phoneNumbers.map { PhoneNumber(it.number) }

    val savedAgency = agencyRepository.saveAndFlush(agency)

    telemetryClient.trackEvent(
      "agency-created",
      mapOf(
        "agencyId" to savedAgency.agencyId,
      ),
      null,
    )

    return savedAgency.toAgencyDto()
  }

  fun updateAgency(agencyId: String, updateAgencyDto: UpdateAgencyDto): AgencyDto {
    val agency = agencyRepository.findByIdOrNull(agencyId) ?: throw EntityNotFoundException("Agency $agencyId not found")
    agency.update(updateAgencyDto)

    telemetryClient.trackEvent(
      "agency-updated",
      mapOf(
        "agencyId" to agency.agencyId,
      ),
      null,
    )

    return agency.toAgencyDto()
  }

  fun deleteAgency(agencyId: String) {
    val agency = agencyRepository.findByIdOrNull(agencyId) ?: throw EntityNotFoundException("Agency $agencyId not found")
    agencyRepository.delete(agency)

    telemetryClient.trackEvent(
      "agency-deleted",
      mapOf(
        "agencyId" to agency.agencyId,
      ),
      null,
    )
  }

  fun createAgencyAddress(agencyId: String, updateAddressDto: UpdateAddressDto): AgencyAddressDto {
    val agency = agencyRepository.findByIdOrNull(agencyId) ?: throw EntityNotFoundException("Agency $agencyId not found")

    val address = updateAddressDto.toAgencyAddress()
    agency.addresses += address
    agencyRepository.flush()

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
    val agency = agencyRepository.findByIdOrNull(agencyId) ?: throw EntityNotFoundException("Agency $agencyId not found")
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
    val agency = agencyRepository.findByIdOrNull(agencyId) ?: throw EntityNotFoundException("Agency $agencyId not found")
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
    val agency = agencyRepository.findByIdOrNull(agencyId) ?: throw EntityNotFoundException("Agency $agencyId not found")

    // phone number must be unique within the agency
    if (agency.phoneNumbers.any { it.value == updatePhoneNumberDto.number }) {
      throw PhoneNumberAlreadyExistsException(updatePhoneNumberDto.number)
    }

    val phoneNumber = PhoneNumber(updatePhoneNumberDto.number)
    agency.phoneNumbers += phoneNumber
    agencyRepository.flush()

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
    val agency = agencyRepository.findByIdOrNull(agencyId) ?: throw EntityNotFoundException("Agency $agencyId not found")
    val phoneNumber = agency.phoneNumbers.find { it.id == phoneNumberId } ?: throw EntityNotFoundException("Phone number $phoneNumberId not found for agency $agencyId")

    // phone number must be unique within the agency
    if (agency.phoneNumbers.any { it.id != phoneNumberId && it.value == updatePhoneNumberDto.number }) {
      throw PhoneNumberAlreadyExistsException(updatePhoneNumberDto.number)
    }

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
    val agency = agencyRepository.findByIdOrNull(agencyId) ?: throw EntityNotFoundException("Agency $agencyId not found")
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
    val agency = agencyRepository.findByIdOrNull(agencyId) ?: throw EntityNotFoundException("Agency $agencyId not found")

    // email address is unique across all establishments
    if (emailAddressRepository.getByValue(updateEmailAddressDto.address) != null) {
      throw EmailAddressAlreadyExistsException(updateEmailAddressDto.address)
    }

    val emailAddress = EmailAddress(updateEmailAddressDto.address)
    agency.emailAddresses += emailAddress
    agencyRepository.flush()

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
    val agency = agencyRepository.findByIdOrNull(agencyId) ?: throw EntityNotFoundException("Agency $agencyId not found")
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
    val agency = agencyRepository.findByIdOrNull(agencyId) ?: throw EntityNotFoundException("Agency $agencyId not found")
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

  private fun Agency.toAgencyDto() = AgencyDto(
    agencyId = this.agencyId,
    agencyName = this.name,
    description = this.description,
    active = this.active,
    accessibleAccess = this.accessibleAccess?.name,
    agencyType = this.agencyType.name,
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

  fun tryFindById(agencyId: String): LegacyAgencyDto? = agencyRepository.findByIdOrNull(agencyId)?.let { agency ->
    LegacyAgencyDto(
      agencyType = LegacyAgencyType.valueOf(agency.agencyType.name),
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

  fun createOrUpdateAgencyFromLegacyData(agencyId: String, agencyDto: LegacyAgencyDto): LegacyAgencyResponse = agencyRepository.findByIdOrNull(agencyId)?.let { agency ->
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
    agencyRepository.saveAndFlush(agency)
    LegacyAgencyResponse(updated = false)
  }

  private fun LegacyAgencyDto.toAgency(agencyId: String) = Agency(
    agencyId = agencyId,
    name = this.name,
    description = this.description,
    active = this.active,
    accessibleAccess = this.accessibleAccess?.let { AccessibleAccess.valueOf(it.name) },
    agencyType = runCatching { AgencyType.valueOf(this.agencyType.name) }.getOrElse { throw ValidationException("${this.agencyType} agency type not supported for agency $agencyId") },
    inactiveDate = this.inactiveDate,
    cjitCode = this.cjitCode,
    area = this.areaCode?.let { areaRepository.findByIdOrNull(it) ?: throw ValidationException("$it area code not found for agency $agencyId") },
    region = this.regionCode?.let { regionRepository.findByIdOrNull(it) ?: throw ValidationException("$it region code not found for agency $agencyId") },
    geographicalArea = this.geographicalAreaCode?.let { areaRepository.findByIdOrNull(it) ?: throw ValidationException("$it geographical area code not found for agency $agencyId") },
    payrollRegion = this.payrollRegionCode?.let { payrollRegionRepository.findByIdOrNull(it) ?: throw ValidationException("$it payroll region code not found for agency $agencyId") },
    localAuthority = this.localAuthorityCode?.let { localAuthorityRepository.findByIdOrNull(it) ?: throw ValidationException("$it local authority code not found for agency $agencyId") },
  )

  private fun Agency.update(agencyDto: LegacyAgencyDto) {
    this.name = agencyDto.name
    this.description = agencyDto.description
    this.active = agencyDto.active
    this.accessibleAccess = agencyDto.accessibleAccess?.let { AccessibleAccess.valueOf(it.name) }
    this.agencyType = runCatching { AgencyType.valueOf(agencyDto.agencyType.name) }.getOrElse { throw ValidationException("${agencyDto.agencyType} agency type not supported for agency $agencyId") }
    this.inactiveDate = agencyDto.inactiveDate
    this.cjitCode = agencyDto.cjitCode
    this.area = agencyDto.areaCode?.let { areaRepository.findByIdOrNull(it) ?: throw ValidationException("$it area code not found for agency $agencyId") }
    this.region = agencyDto.regionCode?.let { regionRepository.findByIdOrNull(it) ?: throw ValidationException("$it region code not found for agency $agencyId") }
    this.geographicalArea = agencyDto.geographicalAreaCode?.let { areaRepository.findByIdOrNull(it) ?: throw ValidationException("$it geographical area code not found for agency $agencyId") }
    this.payrollRegion = agencyDto.payrollRegionCode?.let { payrollRegionRepository.findByIdOrNull(it) ?: throw ValidationException("$it payroll region code not found for agency $agencyId") }
    this.localAuthority = agencyDto.localAuthorityCode?.let { localAuthorityRepository.findByIdOrNull(it) ?: throw ValidationException("$it local authority code not found for agency $agencyId") }
  }

  private fun Agency.update(updateAgencyDto: UpdateAgencyDto) {
    this.name = updateAgencyDto.agencyName
    this.description = updateAgencyDto.description
    this.active = updateAgencyDto.active
    this.accessibleAccess = updateAgencyDto.accessibleAccess
    this.agencyType = updateAgencyDto.agencyType
    this.inactiveDate = updateAgencyDto.inactiveDate
    this.cjitCode = updateAgencyDto.cjitCode
    this.area = updateAgencyDto.areaCode?.let { areaRepository.findByIdOrNull(it) ?: throw ValidationException("$it area code not found for agency $agencyId") }
    this.region = updateAgencyDto.regionCode?.let { regionRepository.findByIdOrNull(it) ?: throw ValidationException("$it region code not found for agency $agencyId") }
    this.geographicalArea = updateAgencyDto.geographicalAreaCode?.let { areaRepository.findByIdOrNull(it) ?: throw ValidationException("$it geographical area code not found for agency $agencyId") }
    this.payrollRegion = updateAgencyDto.payrollRegionCode?.let { payrollRegionRepository.findByIdOrNull(it) ?: throw ValidationException("$it payroll region code not found for agency $agencyId") }
    this.localAuthority = updateAgencyDto.localAuthorityCode?.let { localAuthorityRepository.findByIdOrNull(it) ?: throw ValidationException("$it local authority code not found for agency $agencyId") }
  }

  private fun CreateAgencyDto.toAgency() = Agency(
    agencyId = this.agencyId,
    name = this.agencyName,
    description = this.description,
    active = this.active,
    accessibleAccess = this.accessibleAccess,
    agencyType = this.agencyType,
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
