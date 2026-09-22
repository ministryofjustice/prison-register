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
import uk.gov.justice.digital.hmpps.prisonregister.model.ApprovedPremise
import uk.gov.justice.digital.hmpps.prisonregister.model.ApprovedPremiseRepository
import uk.gov.justice.digital.hmpps.prisonregister.model.AreaRepository
import uk.gov.justice.digital.hmpps.prisonregister.model.EmailAddress
import uk.gov.justice.digital.hmpps.prisonregister.model.EmailAddressRepository
import uk.gov.justice.digital.hmpps.prisonregister.model.LocalAuthorityRepository
import uk.gov.justice.digital.hmpps.prisonregister.model.PayrollRegionRepository
import uk.gov.justice.digital.hmpps.prisonregister.model.PhoneNumber
import uk.gov.justice.digital.hmpps.prisonregister.model.RegionRepository
import uk.gov.justice.digital.hmpps.prisonregister.resource.ApprovedPremiseDto
import uk.gov.justice.digital.hmpps.prisonregister.resource.CreateApprovedPremiseDto
import uk.gov.justice.digital.hmpps.prisonregister.resource.LegacyAccessibleAccess
import uk.gov.justice.digital.hmpps.prisonregister.resource.LegacyAgencyAddressDto
import uk.gov.justice.digital.hmpps.prisonregister.resource.LegacyAgencyDto
import uk.gov.justice.digital.hmpps.prisonregister.resource.LegacyAgencyEmailDto
import uk.gov.justice.digital.hmpps.prisonregister.resource.LegacyAgencyPhoneDto
import uk.gov.justice.digital.hmpps.prisonregister.resource.LegacyAgencyResponse
import uk.gov.justice.digital.hmpps.prisonregister.resource.LegacyAgencyType
import uk.gov.justice.digital.hmpps.prisonregister.resource.UpdateAddressDto
import uk.gov.justice.digital.hmpps.prisonregister.resource.UpdateApprovedPremiseDto
import uk.gov.justice.digital.hmpps.prisonregister.resource.UpdateEmailAddressDto
import uk.gov.justice.digital.hmpps.prisonregister.resource.UpdatePhoneNumberDto
import uk.gov.justice.digital.hmpps.prisonregister.resource.dto.AgencyAddressDto
import uk.gov.justice.digital.hmpps.prisonregister.resource.dto.AgencyEmailDto
import uk.gov.justice.digital.hmpps.prisonregister.resource.dto.AgencyPhoneDto
import uk.gov.justice.digital.hmpps.prisonregister.resource.dto.CodeDescription

@Service
@Transactional
class ApprovedPremiseService(
  private val approvedPremiseRepository: ApprovedPremiseRepository,
  private val areaRepository: AreaRepository,
  private val regionRepository: RegionRepository,
  private val payrollRegionRepository: PayrollRegionRepository,
  private val localAuthorityRepository: LocalAuthorityRepository,
  private val emailAddressRepository: EmailAddressRepository,
  private val telemetryClient: TelemetryClient,
) {
  fun deleteAll() {
    approvedPremiseRepository.deleteAll()
  }

  fun getAllIds(): List<String> = approvedPremiseRepository.findAll().map { it.approvedPremiseId }

  fun getAll(): List<ApprovedPremiseDto> = approvedPremiseRepository.findAll().map { it.toApprovedPremiseDto() }

  fun findById(approvedPremiseId: String): ApprovedPremiseDto = approvedPremiseRepository.findByIdOrNull(approvedPremiseId)?.toApprovedPremiseDto()
    ?: throw EntityNotFoundException("Approved premise $approvedPremiseId not found")

  fun createApprovedPremise(createApprovedPremiseDto: CreateApprovedPremiseDto): ApprovedPremiseDto {
    if (approvedPremiseRepository.existsById(createApprovedPremiseDto.approvedPremiseId)) {
      throw ValidationException("Approved premise ${createApprovedPremiseDto.approvedPremiseId} already exists")
    }

    // todo add validation below when correct constraints known
    val approvedPremise = createApprovedPremiseDto.toApprovedPremise()
    approvedPremise.addresses += createApprovedPremiseDto.addresses.map { it.toAgencyAddress() }
    approvedPremise.emailAddresses += createApprovedPremiseDto.emailAddresses.map { EmailAddress(it.address) }
    approvedPremise.phoneNumbers += createApprovedPremiseDto.phoneNumbers.map { PhoneNumber(it.number) }

    val savedApprovedPremise = approvedPremiseRepository.saveAndFlush(approvedPremise)

    telemetryClient.trackEvent(
      "approved-premises-created",
      mapOf(
        "approvedPremiseId" to savedApprovedPremise.approvedPremiseId,
      ),
      null,
    )

    return savedApprovedPremise.toApprovedPremiseDto()
  }

  fun updateApprovedPremise(approvedPremiseId: String, updateApprovedPremiseDto: UpdateApprovedPremiseDto): ApprovedPremiseDto {
    val approvedPremise = approvedPremiseRepository.findByIdOrNull(approvedPremiseId) ?: throw EntityNotFoundException("Approved premise $approvedPremiseId not found")
    approvedPremise.update(updateApprovedPremiseDto)

    telemetryClient.trackEvent(
      "approved-premises-updated",
      mapOf(
        "approvedPremiseId" to approvedPremise.approvedPremiseId,
      ),
      null,
    )

    return approvedPremise.toApprovedPremiseDto()
  }

  fun deleteApprovedPremise(approvedPremiseId: String) {
    val approvedPremise = approvedPremiseRepository.findByIdOrNull(approvedPremiseId) ?: throw EntityNotFoundException("Approved premise $approvedPremiseId not found")
    approvedPremiseRepository.delete(approvedPremise)

    telemetryClient.trackEvent(
      "approved-premises-deleted",
      mapOf(
        "approvedPremiseId" to approvedPremise.approvedPremiseId,
      ),
      null,
    )
  }

  fun createApprovedPremiseAddress(approvedPremiseId: String, updateAddressDto: UpdateAddressDto): AgencyAddressDto {
    val approvedPremise = approvedPremiseRepository.findByIdOrNull(approvedPremiseId) ?: throw EntityNotFoundException("Approved premise $approvedPremiseId not found")

    val address = updateAddressDto.toAgencyAddress()
    approvedPremise.addresses += address
    approvedPremiseRepository.flush()

    telemetryClient.trackEvent(
      "approved-premises-address-created",
      mapOf(
        "approvedPremiseId" to approvedPremise.approvedPremiseId,
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

  fun updateApprovedPremiseAddress(approvedPremiseId: String, addressId: Long, updateAddressDto: UpdateAddressDto): AgencyAddressDto {
    val approvedPremise = approvedPremiseRepository.findByIdOrNull(approvedPremiseId) ?: throw EntityNotFoundException("Approved premise $approvedPremiseId not found")
    val address = approvedPremise.addresses.find { it.id == addressId } ?: throw EntityNotFoundException("Address $addressId not found for approved premise $approvedPremiseId")

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
        "approvedPremiseId" to approvedPremise.approvedPremiseId,
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

  fun deleteApprovedPremiseAddress(approvedPremiseId: String, addressId: Long): AgencyAddressDto {
    val approvedPremise = approvedPremiseRepository.findByIdOrNull(approvedPremiseId) ?: throw EntityNotFoundException("Approved premise $approvedPremiseId not found")
    val address = approvedPremise.addresses.find { it.id == addressId } ?: throw EntityNotFoundException("Address $addressId not found for approved premise $approvedPremiseId")

    approvedPremise.addresses.remove(address)

    telemetryClient.trackEvent(
      "approved-premises-address-deleted",
      mapOf(
        "approvedPremiseId" to approvedPremise.approvedPremiseId,
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

  fun createApprovedPremisePhoneNumber(approvedPremiseId: String, updatePhoneNumberDto: UpdatePhoneNumberDto): AgencyPhoneDto {
    val approvedPremise = approvedPremiseRepository.findByIdOrNull(approvedPremiseId) ?: throw EntityNotFoundException("Approved premise $approvedPremiseId not found")

    // phone number must be unique within the approved premise
    if (approvedPremise.phoneNumbers.any { it.value == updatePhoneNumberDto.number }) {
      throw PhoneNumberAlreadyExistsException(updatePhoneNumberDto.number)
    }

    val phoneNumber = PhoneNumber(updatePhoneNumberDto.number)
    approvedPremise.phoneNumbers += phoneNumber
    approvedPremiseRepository.flush()

    telemetryClient.trackEvent(
      "approved-premises-phone-number-created",
      mapOf(
        "approvedPremiseId" to approvedPremise.approvedPremiseId,
        "phoneNumberId" to phoneNumber.id.toString(),
      ),
      null,
    )

    return AgencyPhoneDto(
      id = phoneNumber.id,
      number = phoneNumber.value,
    )
  }

  fun updateApprovedPremisePhoneNumber(approvedPremiseId: String, phoneNumberId: Long, updatePhoneNumberDto: UpdatePhoneNumberDto): AgencyPhoneDto {
    val approvedPremise = approvedPremiseRepository.findByIdOrNull(approvedPremiseId) ?: throw EntityNotFoundException("Approved premise $approvedPremiseId not found")
    val phoneNumber = approvedPremise.phoneNumbers.find { it.id == phoneNumberId } ?: throw EntityNotFoundException("Phone number $phoneNumberId not found for approved premise $approvedPremiseId")

    // phone number must be unique within the approved premise
    if (approvedPremise.phoneNumbers.any { it.id != phoneNumberId && it.value == updatePhoneNumberDto.number }) {
      throw PhoneNumberAlreadyExistsException(updatePhoneNumberDto.number)
    }

    phoneNumber.value = updatePhoneNumberDto.number

    telemetryClient.trackEvent(
      "approved-premises-phone-number-updated",
      mapOf(
        "approvedPremiseId" to approvedPremise.approvedPremiseId,
        "phoneNumberId" to phoneNumber.id.toString(),
      ),
      null,
    )

    return AgencyPhoneDto(
      id = phoneNumber.id,
      number = phoneNumber.value,
    )
  }

  fun deleteApprovedPremisePhoneNumber(approvedPremiseId: String, phoneNumberId: Long): AgencyPhoneDto {
    val approvedPremise = approvedPremiseRepository.findByIdOrNull(approvedPremiseId) ?: throw EntityNotFoundException("Approved premise $approvedPremiseId not found")
    val phoneNumber = approvedPremise.phoneNumbers.find { it.id == phoneNumberId } ?: throw EntityNotFoundException("Phone number $phoneNumberId not found for approved premise $approvedPremiseId")

    approvedPremise.phoneNumbers.remove(phoneNumber)

    telemetryClient.trackEvent(
      "approved-premises-phone-number-deleted",
      mapOf(
        "approvedPremiseId" to approvedPremise.approvedPremiseId,
        "phoneNumberId" to phoneNumber.id.toString(),
      ),
      null,
    )

    return AgencyPhoneDto(
      id = phoneNumber.id,
      number = phoneNumber.value,
    )
  }

  fun createApprovedPremiseEmailAddress(approvedPremiseId: String, updateEmailAddressDto: UpdateEmailAddressDto): AgencyEmailDto {
    val approvedPremise = approvedPremiseRepository.findByIdOrNull(approvedPremiseId) ?: throw EntityNotFoundException("Approved premise $approvedPremiseId not found")

    // email address is unique across all establishments
    if (emailAddressRepository.getByValue(updateEmailAddressDto.address) != null) {
      throw EmailAddressAlreadyExistsException(updateEmailAddressDto.address)
    }

    val emailAddress = EmailAddress(updateEmailAddressDto.address)
    approvedPremise.emailAddresses += emailAddress
    approvedPremiseRepository.flush()

    telemetryClient.trackEvent(
      "approved-premises-email-address-created",
      mapOf(
        "approvedPremiseId" to approvedPremise.approvedPremiseId,
        "emailAddressId" to emailAddress.id.toString(),
      ),
      null,
    )

    return AgencyEmailDto(
      id = emailAddress.id,
      address = emailAddress.value,
    )
  }

  fun updateApprovedPremiseEmailAddress(approvedPremiseId: String, emailAddressId: Long, updateEmailAddressDto: UpdateEmailAddressDto): AgencyEmailDto {
    val approvedPremise = approvedPremiseRepository.findByIdOrNull(approvedPremiseId) ?: throw EntityNotFoundException("Approved premise $approvedPremiseId not found")
    val emailAddress = approvedPremise.emailAddresses.find { it.id == emailAddressId } ?: throw EntityNotFoundException("Email address $emailAddressId not found for approved premise $approvedPremiseId")

    emailAddress.value = updateEmailAddressDto.address

    telemetryClient.trackEvent(
      "approved-premises-email-address-updated",
      mapOf(
        "approvedPremiseId" to approvedPremise.approvedPremiseId,
        "emailAddressId" to emailAddress.id.toString(),
      ),
      null,
    )

    return AgencyEmailDto(
      id = emailAddress.id,
      address = emailAddress.value,
    )
  }

  fun deleteApprovedPremiseEmailAddress(approvedPremiseId: String, emailAddressId: Long): AgencyEmailDto {
    val approvedPremise = approvedPremiseRepository.findByIdOrNull(approvedPremiseId) ?: throw EntityNotFoundException("Approved premise $approvedPremiseId not found")
    val emailAddress = approvedPremise.emailAddresses.find { it.id == emailAddressId } ?: throw EntityNotFoundException("Email address $emailAddressId not found for approved premise $approvedPremiseId")

    approvedPremise.emailAddresses.remove(emailAddress)

    telemetryClient.trackEvent(
      "approved-premises-email-address-deleted",
      mapOf(
        "approvedPremiseId" to approvedPremise.approvedPremiseId,
        "emailAddressId" to emailAddress.id.toString(),
      ),
      null,
    )

    return AgencyEmailDto(
      id = emailAddress.id,
      address = emailAddress.value,
    )
  }

  private fun ApprovedPremise.toApprovedPremiseDto() = ApprovedPremiseDto(
    approvedPremiseId = this.approvedPremiseId,
    approvedPremiseName = this.name,
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

  fun tryFindById(agencyId: String): LegacyAgencyDto? = approvedPremiseRepository.findByIdOrNull(agencyId)?.let { ap ->
    LegacyAgencyDto(
      agencyType = LegacyAgencyType.APPROVED_PREMISE,
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

  fun createOrUpdateApprovedPremiseFromLegacyData(approvedPremiseId: String, agencyDto: LegacyAgencyDto): LegacyAgencyResponse = approvedPremiseRepository.findByIdOrNull(approvedPremiseId)?.let { approvedPremise ->
    approvedPremise.update(agencyDto)
    if (approvedPremise.addresses.size == 1 && agencyDto.addresses.size == 1) {
      approvedPremise.addresses[0].update(agencyDto.addresses[0])
    } else {
      approvedPremise.addresses.clear()
      approvedPremise.addresses += agencyDto.addresses.map { it.toAgencyAddress() }
    }

    approvedPremise.phoneNumbers.updatePhoneNumberFrom(agencyDto.phoneNumbers)
    approvedPremise.emailAddresses.updateEmailAddressFrom(agencyDto.emailAddresses)

    LegacyAgencyResponse(updated = true)
  } ?: let {
    val approvedPremise = agencyDto.toApprovedPremise(approvedPremiseId)
    approvedPremise.addresses += agencyDto.addresses.map { it.toAgencyAddress() }
    approvedPremise.phoneNumbers += agencyDto.phoneNumbers.map { it.toAgencyPhone() }
    approvedPremise.emailAddresses += agencyDto.emailAddresses.map { it.toAgencyEmail() }
    approvedPremiseRepository.saveAndFlush(approvedPremise)
    LegacyAgencyResponse(updated = false)
  }

  private fun LegacyAgencyDto.toApprovedPremise(approvedPremiseId: String) = ApprovedPremise(
    approvedPremiseId = approvedPremiseId,
    name = this.name,
    description = this.description,
    contact = this.contact,
    active = this.active,
    accessibleAccess = this.accessibleAccess?.let { AccessibleAccess.valueOf(it.name) },
    inactiveDate = this.inactiveDate,
    cjitCode = this.cjitCode,
    area = this.areaCode?.let { areaRepository.findByIdOrNull(it) ?: throw ValidationException("$it area code not found for agency $approvedPremiseId") },
    region = this.regionCode?.let { regionRepository.findByIdOrNull(it) ?: throw ValidationException("$it region code not found for agency $approvedPremiseId") },
    payrollRegion = this.payrollRegionCode?.let { payrollRegionRepository.findByIdOrNull(it) ?: throw ValidationException("$it payroll region code not found for agency $approvedPremiseId") },
    localAuthority = this.localAuthorityCode?.let { localAuthorityRepository.findByIdOrNull(it) ?: throw ValidationException("$it local authority code not found for agency $approvedPremiseId") },
    geographicalArea = this.geographicalAreaCode?.let { areaRepository.findByIdOrNull(it) ?: throw ValidationException("$it geographical area code not found for agency $approvedPremiseId") },
  )

  private fun ApprovedPremise.update(agencyDto: LegacyAgencyDto) {
    this.name = agencyDto.name
    this.description = agencyDto.description
    this.active = agencyDto.active
    this.contact = agencyDto.contact
    this.accessibleAccess = agencyDto.accessibleAccess?.let { AccessibleAccess.valueOf(it.name) }
    this.inactiveDate = agencyDto.inactiveDate
    this.cjitCode = agencyDto.cjitCode
    this.area = agencyDto.areaCode?.let { areaRepository.findByIdOrNull(it) ?: throw ValidationException("$it area code not found for agency $approvedPremiseId") }
    this.region = agencyDto.regionCode?.let { regionRepository.findByIdOrNull(it) ?: throw ValidationException("$it region code not found for agency $approvedPremiseId") }
    this.geographicalArea = agencyDto.geographicalAreaCode?.let { areaRepository.findByIdOrNull(it) ?: throw ValidationException("$it geographical area code not found for agency $approvedPremiseId") }
    this.payrollRegion = agencyDto.payrollRegionCode?.let { payrollRegionRepository.findByIdOrNull(it) ?: throw ValidationException("$it payroll region code not found for agency $approvedPremiseId") }
    this.localAuthority = agencyDto.localAuthorityCode?.let { localAuthorityRepository.findByIdOrNull(it) ?: throw ValidationException("$it local authority code not found for agency $approvedPremiseId") }
  }

  private fun ApprovedPremise.update(updateApprovedPremiseDto: UpdateApprovedPremiseDto) {
    this.name = updateApprovedPremiseDto.approvedPremiseName
    this.description = updateApprovedPremiseDto.description
    this.contact = updateApprovedPremiseDto.contact
    this.active = updateApprovedPremiseDto.active
    this.accessibleAccess = updateApprovedPremiseDto.accessibleAccess
    this.inactiveDate = updateApprovedPremiseDto.inactiveDate
    this.cjitCode = updateApprovedPremiseDto.cjitCode
    this.area = updateApprovedPremiseDto.areaCode?.let { areaRepository.findByIdOrNull(it) ?: throw ValidationException("$it area code not found for approved premise $approvedPremiseId") }
    this.region = updateApprovedPremiseDto.regionCode?.let { regionRepository.findByIdOrNull(it) ?: throw ValidationException("$it region code not found for approved premise $approvedPremiseId") }
    this.geographicalArea = updateApprovedPremiseDto.geographicalAreaCode?.let { areaRepository.findByIdOrNull(it) ?: throw ValidationException("$it geographical area code not found for approved premise $approvedPremiseId") }
    this.payrollRegion = updateApprovedPremiseDto.payrollRegionCode?.let { payrollRegionRepository.findByIdOrNull(it) ?: throw ValidationException("$it payroll region code not found for approved premise $approvedPremiseId") }
    this.localAuthority = updateApprovedPremiseDto.localAuthorityCode?.let { localAuthorityRepository.findByIdOrNull(it) ?: throw ValidationException("$it local authority code not found for approved premise $approvedPremiseId") }
  }

  private fun CreateApprovedPremiseDto.toApprovedPremise() = ApprovedPremise(
    approvedPremiseId = this.approvedPremiseId,
    name = this.approvedPremiseName,
    description = this.description,
    contact = this.contact,
    active = this.active,
    accessibleAccess = this.accessibleAccess,
    inactiveDate = this.inactiveDate,
    cjitCode = this.cjitCode,
    area = this.areaCode?.let { areaRepository.findByIdOrNull(it) ?: throw ValidationException("$it area code not found for approved premise $approvedPremiseId") },
    region = this.regionCode?.let { regionRepository.findByIdOrNull(it) ?: throw ValidationException("$it region code not found for approved premise $approvedPremiseId") },
    geographicalArea = this.geographicalAreaCode?.let { areaRepository.findByIdOrNull(it) ?: throw ValidationException("$it geographical area code not found for approved premise $approvedPremiseId") },
    payrollRegion = this.payrollRegionCode?.let { payrollRegionRepository.findByIdOrNull(it) ?: throw ValidationException("$it payroll region code not found for approved premise $approvedPremiseId") },
    localAuthority = this.localAuthorityCode?.let { localAuthorityRepository.findByIdOrNull(it) ?: throw ValidationException("$it local authority code not found for approved premise $approvedPremiseId") },
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
