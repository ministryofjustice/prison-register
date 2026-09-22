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
import uk.gov.justice.digital.hmpps.prisonregister.model.AgencyAddress
import uk.gov.justice.digital.hmpps.prisonregister.model.ApprovedPremises
import uk.gov.justice.digital.hmpps.prisonregister.model.ApprovedPremisesRepository
import uk.gov.justice.digital.hmpps.prisonregister.model.AreaRepository
import uk.gov.justice.digital.hmpps.prisonregister.model.EmailAddress
import uk.gov.justice.digital.hmpps.prisonregister.model.EmailAddressRepository
import uk.gov.justice.digital.hmpps.prisonregister.model.LocalAuthorityRepository
import uk.gov.justice.digital.hmpps.prisonregister.model.PayrollRegionRepository
import uk.gov.justice.digital.hmpps.prisonregister.model.PhoneNumber
import uk.gov.justice.digital.hmpps.prisonregister.model.RegionRepository
import uk.gov.justice.digital.hmpps.prisonregister.resource.ApprovedPremisesDto
import uk.gov.justice.digital.hmpps.prisonregister.resource.CreateApprovedPremisesDto
import uk.gov.justice.digital.hmpps.prisonregister.resource.LegacyAccessibleAccess
import uk.gov.justice.digital.hmpps.prisonregister.resource.LegacyAgencyAddressDto
import uk.gov.justice.digital.hmpps.prisonregister.resource.LegacyAgencyDto
import uk.gov.justice.digital.hmpps.prisonregister.resource.LegacyAgencyEmailDto
import uk.gov.justice.digital.hmpps.prisonregister.resource.LegacyAgencyPhoneDto
import uk.gov.justice.digital.hmpps.prisonregister.resource.LegacyAgencyResponse
import uk.gov.justice.digital.hmpps.prisonregister.resource.LegacyAgencyType
import uk.gov.justice.digital.hmpps.prisonregister.resource.UpdateAddressDto
import uk.gov.justice.digital.hmpps.prisonregister.resource.UpdateApprovedPremisesDto
import uk.gov.justice.digital.hmpps.prisonregister.resource.UpdateEmailAddressDto
import uk.gov.justice.digital.hmpps.prisonregister.resource.UpdatePhoneNumberDto
import uk.gov.justice.digital.hmpps.prisonregister.resource.dto.AgencyAddressDto
import uk.gov.justice.digital.hmpps.prisonregister.resource.dto.AgencyEmailDto
import uk.gov.justice.digital.hmpps.prisonregister.resource.dto.AgencyPhoneDto
import uk.gov.justice.digital.hmpps.prisonregister.resource.dto.CodeDescription

@Service
@Transactional
class ApprovedPremisesService(
  private val approvedPremisesRepository: ApprovedPremisesRepository,
  private val areaRepository: AreaRepository,
  private val regionRepository: RegionRepository,
  private val payrollRegionRepository: PayrollRegionRepository,
  private val localAuthorityRepository: LocalAuthorityRepository,
  private val emailAddressRepository: EmailAddressRepository,
  private val telemetryClient: TelemetryClient,
) {
  fun deleteAll() {
    approvedPremisesRepository.deleteAll()
  }

  fun getAllIds(): List<String> = approvedPremisesRepository.findAll().map { it.approvedPremisesId }

  fun getAll(): List<ApprovedPremisesDto> = approvedPremisesRepository.findAll().map { it.toApprovedPremisesDto() }

  fun findById(approvedPremisesId: String): ApprovedPremisesDto = approvedPremisesRepository.findByIdOrNull(approvedPremisesId)?.toApprovedPremisesDto()
    ?: throw EntityNotFoundException("Approved premises $approvedPremisesId not found")

  fun createApprovedPremises(createApprovedPremisesDto: CreateApprovedPremisesDto): ApprovedPremisesDto {
    if (approvedPremisesRepository.existsById(createApprovedPremisesDto.approvedPremisesId)) {
      throw ValidationException("Approved premises ${createApprovedPremisesDto.approvedPremisesId} already exists")
    }

    // todo add validation below when correct constraints known
    val approvedPremises = createApprovedPremisesDto.toApprovedPremises()
    approvedPremises.addresses += createApprovedPremisesDto.addresses.map { it.toAgencyAddress() }
    approvedPremises.emailAddresses += createApprovedPremisesDto.emailAddresses.map { EmailAddress(it.address) }
    approvedPremises.phoneNumbers += createApprovedPremisesDto.phoneNumbers.map { PhoneNumber(it.number) }

    val savedApprovedPremises = approvedPremisesRepository.saveAndFlush(approvedPremises)

    telemetryClient.trackEvent(
      "approved-premises-created",
      mapOf(
        "approvedPremisesId" to savedApprovedPremises.approvedPremisesId,
      ),
      null,
    )

    return savedApprovedPremises.toApprovedPremisesDto()
  }

  fun updateApprovedPremises(approvedPremisesId: String, updateApprovedPremisesDto: UpdateApprovedPremisesDto): ApprovedPremisesDto {
    val approvedPremises = approvedPremisesRepository.findByIdOrNull(approvedPremisesId) ?: throw EntityNotFoundException("Approved premises $approvedPremisesId not found")
    approvedPremises.update(updateApprovedPremisesDto)

    telemetryClient.trackEvent(
      "approved-premises-updated",
      mapOf(
        "approvedPremisesId" to approvedPremises.approvedPremisesId,
      ),
      null,
    )

    return approvedPremises.toApprovedPremisesDto()
  }

  fun deleteApprovedPremises(approvedPremisesId: String) {
    val approvedPremises = approvedPremisesRepository.findByIdOrNull(approvedPremisesId) ?: throw EntityNotFoundException("Approved premises $approvedPremisesId not found")
    approvedPremisesRepository.delete(approvedPremises)

    telemetryClient.trackEvent(
      "approved-premises-deleted",
      mapOf(
        "approvedPremisesId" to approvedPremises.approvedPremisesId,
      ),
      null,
    )
  }

  fun createApprovedPremisesAddress(approvedPremisesId: String, updateAddressDto: UpdateAddressDto): AgencyAddressDto {
    val approvedPremises = approvedPremisesRepository.findByIdOrNull(approvedPremisesId) ?: throw EntityNotFoundException("Approved premises $approvedPremisesId not found")

    val address = updateAddressDto.toAgencyAddress()
    approvedPremises.addresses += address
    approvedPremisesRepository.flush()

    telemetryClient.trackEvent(
      "approved-premises-address-created",
      mapOf(
        "approvedPremisesId" to approvedPremises.approvedPremisesId,
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

  fun updateApprovedPremisesAddress(approvedPremisesId: String, addressId: Long, updateAddressDto: UpdateAddressDto): AgencyAddressDto {
    val approvedPremises = approvedPremisesRepository.findByIdOrNull(approvedPremisesId) ?: throw EntityNotFoundException("Approved premises $approvedPremisesId not found")
    val address = approvedPremises.addresses.find { it.id == addressId } ?: throw EntityNotFoundException("Address $addressId not found for approved premises $approvedPremisesId")

    with(updateAddressDto) {
      address.addressLine1 = addressLine1
      address.addressLine2 = addressLine2
      address.town = town
      address.county = county
      address.postcode = postcode
      address.country = country
    }

    telemetryClient.trackEvent(
      "approved-premises-address-updated",
      mapOf(
        "approvedPremisesId" to approvedPremises.approvedPremisesId,
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

  fun deleteApprovedPremisesAddress(approvedPremisesId: String, addressId: Long): AgencyAddressDto {
    val approvedPremises = approvedPremisesRepository.findByIdOrNull(approvedPremisesId) ?: throw EntityNotFoundException("Approved premises $approvedPremisesId not found")
    val address = approvedPremises.addresses.find { it.id == addressId } ?: throw EntityNotFoundException("Address $addressId not found for approved premises $approvedPremisesId")

    approvedPremises.addresses.remove(address)

    telemetryClient.trackEvent(
      "approved-premises-address-deleted",
      mapOf(
        "approvedPremisesId" to approvedPremises.approvedPremisesId,
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

  fun createApprovedPremisesPhoneNumber(approvedPremisesId: String, updatePhoneNumberDto: UpdatePhoneNumberDto): AgencyPhoneDto {
    val approvedPremises = approvedPremisesRepository.findByIdOrNull(approvedPremisesId) ?: throw EntityNotFoundException("Approved premises $approvedPremisesId not found")

    // phone number must be unique within the approved premises
    if (approvedPremises.phoneNumbers.any { it.value == updatePhoneNumberDto.number }) {
      throw PhoneNumberAlreadyExistsException(updatePhoneNumberDto.number)
    }

    val phoneNumber = PhoneNumber(updatePhoneNumberDto.number)
    approvedPremises.phoneNumbers += phoneNumber
    approvedPremisesRepository.flush()

    telemetryClient.trackEvent(
      "approved-premises-phone-number-created",
      mapOf(
        "approvedPremisesId" to approvedPremises.approvedPremisesId,
        "phoneNumberId" to phoneNumber.id.toString(),
      ),
      null,
    )

    return AgencyPhoneDto(
      id = phoneNumber.id,
      number = phoneNumber.value,
    )
  }

  fun updateApprovedPremisesPhoneNumber(approvedPremisesId: String, phoneNumberId: Long, updatePhoneNumberDto: UpdatePhoneNumberDto): AgencyPhoneDto {
    val approvedPremises = approvedPremisesRepository.findByIdOrNull(approvedPremisesId) ?: throw EntityNotFoundException("Approved premises $approvedPremisesId not found")
    val phoneNumber = approvedPremises.phoneNumbers.find { it.id == phoneNumberId } ?: throw EntityNotFoundException("Phone number $phoneNumberId not found for approved premises $approvedPremisesId")

    // phone number must be unique within the approved premises
    if (approvedPremises.phoneNumbers.any { it.id != phoneNumberId && it.value == updatePhoneNumberDto.number }) {
      throw PhoneNumberAlreadyExistsException(updatePhoneNumberDto.number)
    }

    phoneNumber.value = updatePhoneNumberDto.number

    telemetryClient.trackEvent(
      "approved-premises-phone-number-updated",
      mapOf(
        "approvedPremisesId" to approvedPremises.approvedPremisesId,
        "phoneNumberId" to phoneNumber.id.toString(),
      ),
      null,
    )

    return AgencyPhoneDto(
      id = phoneNumber.id,
      number = phoneNumber.value,
    )
  }

  fun deleteApprovedPremisesPhoneNumber(approvedPremisesId: String, phoneNumberId: Long): AgencyPhoneDto {
    val approvedPremises = approvedPremisesRepository.findByIdOrNull(approvedPremisesId) ?: throw EntityNotFoundException("Approved premises $approvedPremisesId not found")
    val phoneNumber = approvedPremises.phoneNumbers.find { it.id == phoneNumberId } ?: throw EntityNotFoundException("Phone number $phoneNumberId not found for approved premises $approvedPremisesId")

    approvedPremises.phoneNumbers.remove(phoneNumber)

    telemetryClient.trackEvent(
      "approved-premises-phone-number-deleted",
      mapOf(
        "approvedPremisesId" to approvedPremises.approvedPremisesId,
        "phoneNumberId" to phoneNumber.id.toString(),
      ),
      null,
    )

    return AgencyPhoneDto(
      id = phoneNumber.id,
      number = phoneNumber.value,
    )
  }

  fun createApprovedPremisesEmailAddress(approvedPremisesId: String, updateEmailAddressDto: UpdateEmailAddressDto): AgencyEmailDto {
    val approvedPremises = approvedPremisesRepository.findByIdOrNull(approvedPremisesId) ?: throw EntityNotFoundException("Approved premises $approvedPremisesId not found")

    // email address is unique across all establishments
    if (emailAddressRepository.getByValue(updateEmailAddressDto.address) != null) {
      throw EmailAddressAlreadyExistsException(updateEmailAddressDto.address)
    }

    val emailAddress = EmailAddress(updateEmailAddressDto.address)
    approvedPremises.emailAddresses += emailAddress
    approvedPremisesRepository.flush()

    telemetryClient.trackEvent(
      "approved-premises-email-address-created",
      mapOf(
        "approvedPremisesId" to approvedPremises.approvedPremisesId,
        "emailAddressId" to emailAddress.id.toString(),
      ),
      null,
    )

    return AgencyEmailDto(
      id = emailAddress.id,
      address = emailAddress.value,
    )
  }

  fun updateApprovedPremisesEmailAddress(approvedPremisesId: String, emailAddressId: Long, updateEmailAddressDto: UpdateEmailAddressDto): AgencyEmailDto {
    val approvedPremises = approvedPremisesRepository.findByIdOrNull(approvedPremisesId) ?: throw EntityNotFoundException("Approved premises $approvedPremisesId not found")
    val emailAddress = approvedPremises.emailAddresses.find { it.id == emailAddressId } ?: throw EntityNotFoundException("Email address $emailAddressId not found for approved premises $approvedPremisesId")

    emailAddress.value = updateEmailAddressDto.address

    telemetryClient.trackEvent(
      "approved-premises-email-address-updated",
      mapOf(
        "approvedPremisesId" to approvedPremises.approvedPremisesId,
        "emailAddressId" to emailAddress.id.toString(),
      ),
      null,
    )

    return AgencyEmailDto(
      id = emailAddress.id,
      address = emailAddress.value,
    )
  }

  fun deleteApprovedPremisesEmailAddress(approvedPremisesId: String, emailAddressId: Long): AgencyEmailDto {
    val approvedPremises = approvedPremisesRepository.findByIdOrNull(approvedPremisesId) ?: throw EntityNotFoundException("Approved premises $approvedPremisesId not found")
    val emailAddress = approvedPremises.emailAddresses.find { it.id == emailAddressId } ?: throw EntityNotFoundException("Email address $emailAddressId not found for approved premises $approvedPremisesId")

    approvedPremises.emailAddresses.remove(emailAddress)

    telemetryClient.trackEvent(
      "approved-premises-email-address-deleted",
      mapOf(
        "approvedPremisesId" to approvedPremises.approvedPremisesId,
        "emailAddressId" to emailAddress.id.toString(),
      ),
      null,
    )

    return AgencyEmailDto(
      id = emailAddress.id,
      address = emailAddress.value,
    )
  }

  private fun ApprovedPremises.toApprovedPremisesDto() = ApprovedPremisesDto(
    approvedPremisesId = this.approvedPremisesId,
    approvedPremisesName = this.name,
    description = this.description,
    contact = this.contact,
    active = this.active,
    accessibleAccess = this.accessibleAccess?.name,
    inactiveDate = this.inactiveDate,
    cjitCode = this.cjitCode,
    area = this.area?.let { area -> CodeDescription(area.code, area.description) },
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

  fun tryFindById(agencyId: String): LegacyAgencyDto? = approvedPremisesRepository.findByIdOrNull(agencyId)?.let { ap ->
    LegacyAgencyDto(
      agencyType = LegacyAgencyType.APPROVED_PREMISES,
      name = ap.name,
      description = ap.description,
      active = ap.active,
      inactiveDate = ap.inactiveDate,
      cjitCode = ap.cjitCode,
      areaCode = ap.area?.code,
      regionCode = ap.region?.code,
      geographicalAreaCode = ap.geographicalArea?.code,
      payrollRegionCode = ap.payrollRegion?.code,
      localAuthorityCode = ap.localAuthority?.code,
      courtTypeCode = null,
      accessibleAccess = ap.accessibleAccess?.let { runCatching { LegacyAccessibleAccess.valueOf(it.name) }.getOrNull() },
      contact = ap.contact,
      addresses = ap.addresses.map { LegacyAgencyAddressDto(it.addressLine1, it.addressLine2, it.town, it.county, it.postcode, it.country) },
      emailAddresses = ap.emailAddresses.map { LegacyAgencyEmailDto(it.value) },
      phoneNumbers = ap.phoneNumbers.map { LegacyAgencyPhoneDto(it.value) },
    )
  }

  fun createOrUpdateApprovedPremisesFromLegacyData(approvedPremisesId: String, agencyDto: LegacyAgencyDto): LegacyAgencyResponse = approvedPremisesRepository.findByIdOrNull(approvedPremisesId)?.let { approvedPremises ->
    approvedPremises.update(agencyDto)
    if (approvedPremises.addresses.size == 1 && agencyDto.addresses.size == 1) {
      approvedPremises.addresses[0].update(agencyDto.addresses[0])
    } else {
      approvedPremises.addresses.clear()
      approvedPremises.addresses += agencyDto.addresses.map { it.toAgencyAddress() }
    }

    approvedPremises.phoneNumbers.updatePhoneNumberFrom(agencyDto.phoneNumbers)
    approvedPremises.emailAddresses.updateEmailAddressFrom(agencyDto.emailAddresses)

    LegacyAgencyResponse(updated = true)
  } ?: let {
    val approvedPremises = agencyDto.toApprovedPremises(approvedPremisesId)
    approvedPremises.addresses += agencyDto.addresses.map { it.toAgencyAddress() }
    approvedPremises.phoneNumbers += agencyDto.phoneNumbers.map { it.toAgencyPhone() }
    approvedPremises.emailAddresses += agencyDto.emailAddresses.map { it.toAgencyEmail() }
    approvedPremisesRepository.saveAndFlush(approvedPremises)
    LegacyAgencyResponse(updated = false)
  }

  private fun LegacyAgencyDto.toApprovedPremises(approvedPremisesId: String) = ApprovedPremises(
    approvedPremisesId = approvedPremisesId,
    name = this.name,
    description = this.description,
    contact = this.contact,
    active = this.active,
    accessibleAccess = this.accessibleAccess?.let { AccessibleAccess.valueOf(it.name) },
    inactiveDate = this.inactiveDate,
    cjitCode = this.cjitCode,
    area = this.areaCode?.let { areaRepository.findByIdOrNull(it) ?: throw ValidationException("$it area code not found for agency $approvedPremisesId") },
    region = this.regionCode?.let { regionRepository.findByIdOrNull(it) ?: throw ValidationException("$it region code not found for agency $approvedPremisesId") },
    payrollRegion = this.payrollRegionCode?.let { payrollRegionRepository.findByIdOrNull(it) ?: throw ValidationException("$it payroll region code not found for agency $approvedPremisesId") },
    localAuthority = this.localAuthorityCode?.let { localAuthorityRepository.findByIdOrNull(it) ?: throw ValidationException("$it local authority code not found for agency $approvedPremisesId") },
    geographicalArea = this.geographicalAreaCode?.let { areaRepository.findByIdOrNull(it) ?: throw ValidationException("$it geographical area code not found for agency $approvedPremisesId") },
  )

  private fun ApprovedPremises.update(agencyDto: LegacyAgencyDto) {
    this.name = agencyDto.name
    this.description = agencyDto.description
    this.active = agencyDto.active
    this.contact = agencyDto.contact
    this.accessibleAccess = agencyDto.accessibleAccess?.let { AccessibleAccess.valueOf(it.name) }
    this.inactiveDate = agencyDto.inactiveDate
    this.cjitCode = agencyDto.cjitCode
    this.area = agencyDto.areaCode?.let { areaRepository.findByIdOrNull(it) ?: throw ValidationException("$it area code not found for agency $approvedPremisesId") }
    this.region = agencyDto.regionCode?.let { regionRepository.findByIdOrNull(it) ?: throw ValidationException("$it region code not found for agency $approvedPremisesId") }
    this.geographicalArea = agencyDto.geographicalAreaCode?.let { areaRepository.findByIdOrNull(it) ?: throw ValidationException("$it geographical area code not found for agency $approvedPremisesId") }
    this.payrollRegion = agencyDto.payrollRegionCode?.let { payrollRegionRepository.findByIdOrNull(it) ?: throw ValidationException("$it payroll region code not found for agency $approvedPremisesId") }
    this.localAuthority = agencyDto.localAuthorityCode?.let { localAuthorityRepository.findByIdOrNull(it) ?: throw ValidationException("$it local authority code not found for agency $approvedPremisesId") }
  }

  private fun ApprovedPremises.update(updateApprovedPremisesDto: UpdateApprovedPremisesDto) {
    this.name = updateApprovedPremisesDto.approvedPremisesName
    this.description = updateApprovedPremisesDto.description
    this.contact = updateApprovedPremisesDto.contact
    this.active = updateApprovedPremisesDto.active
    this.accessibleAccess = updateApprovedPremisesDto.accessibleAccess
    this.inactiveDate = updateApprovedPremisesDto.inactiveDate
    this.cjitCode = updateApprovedPremisesDto.cjitCode
    this.area = updateApprovedPremisesDto.areaCode?.let { areaRepository.findByIdOrNull(it) ?: throw ValidationException("$it area code not found for approved premises $approvedPremisesId") }
    this.region = updateApprovedPremisesDto.regionCode?.let { regionRepository.findByIdOrNull(it) ?: throw ValidationException("$it region code not found for approved premises $approvedPremisesId") }
    this.geographicalArea = updateApprovedPremisesDto.geographicalAreaCode?.let { areaRepository.findByIdOrNull(it) ?: throw ValidationException("$it geographical area code not found for approved premises $approvedPremisesId") }
    this.payrollRegion = updateApprovedPremisesDto.payrollRegionCode?.let { payrollRegionRepository.findByIdOrNull(it) ?: throw ValidationException("$it payroll region code not found for approved premises $approvedPremisesId") }
    this.localAuthority = updateApprovedPremisesDto.localAuthorityCode?.let { localAuthorityRepository.findByIdOrNull(it) ?: throw ValidationException("$it local authority code not found for approved premises $approvedPremisesId") }
  }

  private fun CreateApprovedPremisesDto.toApprovedPremises() = ApprovedPremises(
    approvedPremisesId = this.approvedPremisesId,
    name = this.approvedPremisesName,
    description = this.description,
    contact = this.contact,
    active = this.active,
    accessibleAccess = this.accessibleAccess,
    inactiveDate = this.inactiveDate,
    cjitCode = this.cjitCode,
    area = this.areaCode?.let { areaRepository.findByIdOrNull(it) ?: throw ValidationException("$it area code not found for approved premises $approvedPremisesId") },
    region = this.regionCode?.let { regionRepository.findByIdOrNull(it) ?: throw ValidationException("$it region code not found for approved premises $approvedPremisesId") },
    geographicalArea = this.geographicalAreaCode?.let { areaRepository.findByIdOrNull(it) ?: throw ValidationException("$it geographical area code not found for approved premises $approvedPremisesId") },
    payrollRegion = this.payrollRegionCode?.let { payrollRegionRepository.findByIdOrNull(it) ?: throw ValidationException("$it payroll region code not found for approved premises $approvedPremisesId") },
    localAuthority = this.localAuthorityCode?.let { localAuthorityRepository.findByIdOrNull(it) ?: throw ValidationException("$it local authority code not found for approved premises $approvedPremisesId") },
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
