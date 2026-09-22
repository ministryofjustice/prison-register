package uk.gov.justice.digital.hmpps.prisonregister.service

import com.microsoft.applicationinsights.TelemetryClient
import jakarta.persistence.EntityNotFoundException
import jakarta.validation.ValidationException
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.prisonregister.exceptions.PhoneNumberAlreadyExistsException
import uk.gov.justice.digital.hmpps.prisonregister.model.AgencyAddress
import uk.gov.justice.digital.hmpps.prisonregister.model.AreaRepository
import uk.gov.justice.digital.hmpps.prisonregister.model.Hospital
import uk.gov.justice.digital.hmpps.prisonregister.model.HospitalRepository
import uk.gov.justice.digital.hmpps.prisonregister.model.LocalAuthorityRepository
import uk.gov.justice.digital.hmpps.prisonregister.model.PayrollRegionRepository
import uk.gov.justice.digital.hmpps.prisonregister.model.PhoneNumber
import uk.gov.justice.digital.hmpps.prisonregister.model.RegionRepository
import uk.gov.justice.digital.hmpps.prisonregister.resource.CreateHospitalDto
import uk.gov.justice.digital.hmpps.prisonregister.resource.HospitalDto
import uk.gov.justice.digital.hmpps.prisonregister.resource.LegacyAgencyAddressDto
import uk.gov.justice.digital.hmpps.prisonregister.resource.LegacyAgencyDto
import uk.gov.justice.digital.hmpps.prisonregister.resource.LegacyAgencyPhoneDto
import uk.gov.justice.digital.hmpps.prisonregister.resource.LegacyAgencyResponse
import uk.gov.justice.digital.hmpps.prisonregister.resource.LegacyAgencyType
import uk.gov.justice.digital.hmpps.prisonregister.resource.UpdateAddressDto
import uk.gov.justice.digital.hmpps.prisonregister.resource.UpdateHospitalDto
import uk.gov.justice.digital.hmpps.prisonregister.resource.UpdatePhoneNumberDto
import uk.gov.justice.digital.hmpps.prisonregister.resource.dto.AgencyAddressDto
import uk.gov.justice.digital.hmpps.prisonregister.resource.dto.AgencyPhoneDto
import uk.gov.justice.digital.hmpps.prisonregister.resource.dto.CodeDescription

@Service
@Transactional
class HospitalService(
  private val hospitalRepository: HospitalRepository,
  private val areaRepository: AreaRepository,
  private val regionRepository: RegionRepository,
  private val payrollRegionRepository: PayrollRegionRepository,
  private val localAuthorityRepository: LocalAuthorityRepository,
  private val telemetryClient: TelemetryClient,
) {
  fun deleteAll() {
    hospitalRepository.deleteAll()
  }

  fun getAllIds(): List<String> = hospitalRepository.findAll().map { it.hospitalId }

  fun getAll(): List<HospitalDto> = hospitalRepository.findAll().map { it.toHospitalDto() }

  fun findById(hospitalId: String): HospitalDto = hospitalRepository.findByIdOrNull(hospitalId)?.toHospitalDto() ?: throw EntityNotFoundException("Hospital $hospitalId not found")

  fun createHospital(createHospitalDto: CreateHospitalDto): HospitalDto {
    if (hospitalRepository.existsById(createHospitalDto.hospitalId)) {
      throw ValidationException("Hospital ${createHospitalDto.hospitalId} already exists")
    }

    // phone numbers must be unique within the hospital
    val existingNumbers = mutableSetOf<String>()
    createHospitalDto.phoneNumbers.forEach {
      if (!existingNumbers.add(it.number)) {
        throw PhoneNumberAlreadyExistsException(it.number)
      }
    }

    val hospital = createHospitalDto.toHospital()
    hospital.addresses += createHospitalDto.addresses.map { it.toAgencyAddress() }
    hospital.phoneNumbers += createHospitalDto.phoneNumbers.map { PhoneNumber(it.number) }

    val savedHospital = hospitalRepository.saveAndFlush(hospital)

    telemetryClient.trackEvent(
      "hospital-created",
      mapOf(
        "hospitalId" to savedHospital.hospitalId,
      ),
      null,
    )

    return savedHospital.toHospitalDto()
  }

  fun updateHospital(hospitalId: String, updateHospitalDto: UpdateHospitalDto): HospitalDto {
    val hospital = hospitalRepository.findByIdOrNull(hospitalId) ?: throw EntityNotFoundException("Hospital $hospitalId not found")
    hospital.update(updateHospitalDto)

    telemetryClient.trackEvent(
      "hospital-updated",
      mapOf(
        "hospitalId" to hospital.hospitalId,
      ),
      null,
    )

    return hospital.toHospitalDto()
  }

  fun createHospitalAddress(hospitalId: String, updateAddressDto: UpdateAddressDto): AgencyAddressDto {
    val hospital = hospitalRepository.findByIdOrNull(hospitalId) ?: throw EntityNotFoundException("Hospital $hospitalId not found")

    val address = updateAddressDto.toAgencyAddress()
    hospital.addresses += address
    hospitalRepository.flush()

    telemetryClient.trackEvent(
      "hospital-address-created",
      mapOf(
        "hospitalId" to hospital.hospitalId,
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

  fun updateHospitalAddress(hospitalId: String, addressId: Long, updateAddressDto: UpdateAddressDto): AgencyAddressDto {
    val hospital = hospitalRepository.findByIdOrNull(hospitalId) ?: throw EntityNotFoundException("Hospital $hospitalId not found")
    val address = hospital.addresses.find { it.id == addressId } ?: throw EntityNotFoundException("Address $addressId not found for hospital $hospitalId")

    with(updateAddressDto) {
      address.addressLine1 = addressLine1
      address.addressLine2 = addressLine2
      address.town = town
      address.county = county
      address.postcode = postcode
      address.country = country
    }

    telemetryClient.trackEvent(
      "hospital-address-updated",
      mapOf(
        "hospitalId" to hospital.hospitalId,
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

  fun createHospitalPhoneNumber(hospitalId: String, updatePhoneNumberDto: UpdatePhoneNumberDto): AgencyPhoneDto {
    val hospital = hospitalRepository.findByIdOrNull(hospitalId) ?: throw EntityNotFoundException("Hospital $hospitalId not found")

    // phone number must be unique within the hospital
    if (hospital.phoneNumbers.any { it.value == updatePhoneNumberDto.number }) {
      throw PhoneNumberAlreadyExistsException(updatePhoneNumberDto.number)
    }

    val phoneNumber = PhoneNumber(updatePhoneNumberDto.number)
    hospital.phoneNumbers += phoneNumber
    hospitalRepository.flush()

    telemetryClient.trackEvent(
      "hospital-phone-number-created",
      mapOf(
        "hospitalId" to hospital.hospitalId,
        "phoneNumberId" to phoneNumber.id.toString(),
      ),
      null,
    )

    return AgencyPhoneDto(
      id = phoneNumber.id,
      number = phoneNumber.value,
    )
  }

  fun updateHospitalPhoneNumber(hospitalId: String, phoneNumberId: Long, updatePhoneNumberDto: UpdatePhoneNumberDto): AgencyPhoneDto {
    val hospital = hospitalRepository.findByIdOrNull(hospitalId) ?: throw EntityNotFoundException("Hospital $hospitalId not found")
    val phoneNumber = hospital.phoneNumbers.find { it.id == phoneNumberId } ?: throw EntityNotFoundException("Phone number $phoneNumberId not found for hospital $hospitalId")

    // phone number must be unique within the hospital
    if (hospital.phoneNumbers.any { it.id != phoneNumberId && it.value == updatePhoneNumberDto.number }) {
      throw PhoneNumberAlreadyExistsException(updatePhoneNumberDto.number)
    }

    phoneNumber.value = updatePhoneNumberDto.number

    telemetryClient.trackEvent(
      "hospital-phone-number-updated",
      mapOf(
        "hospitalId" to hospital.hospitalId,
        "phoneNumberId" to phoneNumber.id.toString(),
      ),
      null,
    )

    return AgencyPhoneDto(
      id = phoneNumber.id,
      number = phoneNumber.value,
    )
  }

  fun deleteHospital(hospitalId: String) {
    val hospital = hospitalRepository.findByIdOrNull(hospitalId) ?: throw EntityNotFoundException("Hospital $hospitalId not found")
    hospitalRepository.delete(hospital)

    telemetryClient.trackEvent(
      "hospital-deleted",
      mapOf(
        "hospitalId" to hospital.hospitalId,
      ),
      null,
    )
  }

  fun deleteHospitalAddress(hospitalId: String, addressId: Long): AgencyAddressDto {
    val hospital = hospitalRepository.findByIdOrNull(hospitalId) ?: throw EntityNotFoundException("Hospital $hospitalId not found")
    val address = hospital.addresses.find { it.id == addressId } ?: throw EntityNotFoundException("Address $addressId not found for hospital $hospitalId")

    hospital.addresses.remove(address)

    telemetryClient.trackEvent(
      "hospital-address-deleted",
      mapOf(
        "hospitalId" to hospital.hospitalId,
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

  fun deleteHospitalPhoneNumber(hospitalId: String, phoneNumberId: Long): AgencyPhoneDto {
    val hospital = hospitalRepository.findByIdOrNull(hospitalId) ?: throw EntityNotFoundException("Hospital $hospitalId not found")
    val phoneNumber = hospital.phoneNumbers.find { it.id == phoneNumberId } ?: throw EntityNotFoundException("Phone number $phoneNumberId not found for hospital $hospitalId")

    hospital.phoneNumbers.remove(phoneNumber)

    telemetryClient.trackEvent(
      "hospital-phone-number-deleted",
      mapOf(
        "hospitalId" to hospital.hospitalId,
        "phoneNumberId" to phoneNumber.id.toString(),
      ),
      null,
    )

    return AgencyPhoneDto(
      id = phoneNumber.id,
      number = phoneNumber.value,
    )
  }

  private fun Hospital.toHospitalDto() = HospitalDto(
    hospitalId = this.hospitalId,
    hospitalName = this.name,
    description = this.description,
    active = this.active,
    inactiveDate = this.inactiveDate,
    cjitCode = this.cjitCode,
    area = this.area?.let { area -> CodeDescription(area.code, area.description) },
    region = this.region?.let { area -> CodeDescription(area.code, area.description) },
    geographicalArea = this.geographicalArea?.let { area -> CodeDescription(area.code, area.description) },
    payrollRegion = this.payrollRegion?.let { area -> CodeDescription(area.code, area.description) },
    localAuthority = this.localAuthority?.let { localAuthority -> CodeDescription(localAuthority.code, localAuthority.description) },
    highSecurity = this.highSecurity,
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
    phoneNumbers = this.phoneNumbers.map { phoneNumber ->
      AgencyPhoneDto(
        id = phoneNumber.id,
        number = phoneNumber.value,
      )
    },
  )

  fun tryFindById(agencyId: String): LegacyAgencyDto? = hospitalRepository.findByIdOrNull(agencyId)?.let { hospital ->
    LegacyAgencyDto(
      agencyType = if (hospital.highSecurity) LegacyAgencyType.SECURE_HOSPITAL else LegacyAgencyType.HOSPITAL,
      name = hospital.name,
      description = hospital.description,
      active = hospital.active,
      inactiveDate = hospital.inactiveDate,
      cjitCode = hospital.cjitCode,
      areaCode = hospital.area?.code,
      regionCode = hospital.region?.code,
      geographicalAreaCode = hospital.geographicalArea?.code,
      payrollRegionCode = hospital.payrollRegion?.code,
      localAuthorityCode = hospital.localAuthority?.code,
      courtTypeCode = null,
      accessibleAccess = null,
      contact = null,
      addresses = hospital.addresses.map { LegacyAgencyAddressDto(it.addressLine1, it.addressLine2, it.town, it.county, it.postcode, it.country) },
      emailAddresses = emptyList(),
      phoneNumbers = hospital.phoneNumbers.map { LegacyAgencyPhoneDto(it.value) },
    )
  }

  fun createOrUpdateHospitalFromLegacyData(hospitalId: String, agencyDto: LegacyAgencyDto, highSecurity: Boolean): LegacyAgencyResponse = hospitalRepository.findByIdOrNull(hospitalId)?.let { hospital ->
    hospital.update(agencyDto, highSecurity)
    if (hospital.addresses.size == 1 && agencyDto.addresses.size == 1) {
      hospital.addresses[0].update(agencyDto.addresses[0])
    } else {
      hospital.addresses.clear()
      hospital.addresses += agencyDto.addresses.map { it.toAgencyAddress() }
    }

    hospital.phoneNumbers.updatePhoneNumberFrom(agencyDto.phoneNumbers)

    LegacyAgencyResponse(updated = true)
  } ?: let {
    val hospital = agencyDto.toHospital(hospitalId, highSecurity)
    hospital.addresses += agencyDto.addresses.map { it.toAgencyAddress() }
    hospital.phoneNumbers += agencyDto.phoneNumbers.map { it.toAgencyPhone() }
    hospitalRepository.saveAndFlush(hospital)
    LegacyAgencyResponse(updated = false)
  }

  private fun LegacyAgencyDto.toHospital(hospitalId: String, highSecurity: Boolean) = Hospital(
    hospitalId = hospitalId,
    name = this.name,
    description = this.description,
    active = this.active,
    highSecurity = highSecurity,
    inactiveDate = this.inactiveDate,
    cjitCode = this.cjitCode,
    area = this.areaCode?.let { areaRepository.findByIdOrNull(it) ?: throw ValidationException("$it area code not found for agency $hospitalId") },
    region = this.regionCode?.let { regionRepository.findByIdOrNull(it) ?: throw ValidationException("$it region code not found for agency $hospitalId") },
    geographicalArea = this.geographicalAreaCode?.let { areaRepository.findByIdOrNull(it) ?: throw ValidationException("$it geographical area code not found for agency $hospitalId") },
    payrollRegion = this.payrollRegionCode?.let { payrollRegionRepository.findByIdOrNull(it) ?: throw ValidationException("$it payroll region code not found for agency $hospitalId") },
    localAuthority = this.localAuthorityCode?.let { localAuthorityRepository.findByIdOrNull(it) ?: throw ValidationException("$it local authority code not found for agency $hospitalId") },
  )

  private fun Hospital.update(agencyDto: LegacyAgencyDto, highSecurity: Boolean) {
    this.name = agencyDto.name
    this.description = agencyDto.description
    this.active = agencyDto.active
    this.highSecurity = highSecurity
    this.inactiveDate = agencyDto.inactiveDate
    this.cjitCode = agencyDto.cjitCode
    this.area = agencyDto.areaCode?.let { areaRepository.findByIdOrNull(it) ?: throw ValidationException("$it area code not found for agency $hospitalId") }
    this.region = agencyDto.regionCode?.let { regionRepository.findByIdOrNull(it) ?: throw ValidationException("$it region code not found for agency $hospitalId") }
    this.geographicalArea = agencyDto.geographicalAreaCode?.let { areaRepository.findByIdOrNull(it) ?: throw ValidationException("$it geographical area code not found for agency $hospitalId") }
    this.payrollRegion = agencyDto.payrollRegionCode?.let { payrollRegionRepository.findByIdOrNull(it) ?: throw ValidationException("$it payroll region code not found for agency $hospitalId") }
    this.localAuthority = agencyDto.localAuthorityCode?.let { localAuthorityRepository.findByIdOrNull(it) ?: throw ValidationException("$it local authority code not found for agency $hospitalId") }
  }

  private fun CreateHospitalDto.toHospital() = Hospital(
    hospitalId = this.hospitalId,
    name = this.hospitalName,
    description = this.description,
    active = this.active,
    highSecurity = this.highSecurity,
    inactiveDate = this.inactiveDate,
    cjitCode = this.cjitCode,
    area = this.areaCode?.let { areaRepository.findByIdOrNull(it) ?: throw ValidationException("$it area code not found for hospital ${this.hospitalId}") },
    region = this.regionCode?.let { regionRepository.findByIdOrNull(it) ?: throw ValidationException("$it region code not found for hospital ${this.hospitalId}") },
    geographicalArea = this.geographicalAreaCode?.let { areaRepository.findByIdOrNull(it) ?: throw ValidationException("$it geographical area code not found for hospital ${this.hospitalId}") },
    payrollRegion = this.payrollRegionCode?.let { payrollRegionRepository.findByIdOrNull(it) ?: throw ValidationException("$it payroll region code not found for hospital ${this.hospitalId}") },
    localAuthority = this.localAuthorityCode?.let { localAuthorityRepository.findByIdOrNull(it) ?: throw ValidationException("$it local authority code not found for hospital ${this.hospitalId}") },
  )

  private fun UpdateAddressDto.toAgencyAddress() = AgencyAddress(
    addressLine1 = this.addressLine1,
    addressLine2 = this.addressLine2,
    town = this.town,
    county = this.county,
    postcode = this.postcode,
    country = this.country,
  )

  private fun Hospital.update(updateHospitalDto: UpdateHospitalDto) {
    this.name = updateHospitalDto.hospitalName
    this.description = updateHospitalDto.description
    this.active = updateHospitalDto.active
    this.highSecurity = updateHospitalDto.highSecurity
    this.inactiveDate = updateHospitalDto.inactiveDate
    this.cjitCode = updateHospitalDto.cjitCode
    this.area = updateHospitalDto.areaCode?.let { areaRepository.findByIdOrNull(it) ?: throw ValidationException("$it area code not found for hospital $hospitalId") }
    this.region = updateHospitalDto.regionCode?.let { regionRepository.findByIdOrNull(it) ?: throw ValidationException("$it region code not found for hospital $hospitalId") }
    this.geographicalArea = updateHospitalDto.geographicalAreaCode?.let { areaRepository.findByIdOrNull(it) ?: throw ValidationException("$it geographical area code not found for hospital $hospitalId") }
    this.payrollRegion = updateHospitalDto.payrollRegionCode?.let { payrollRegionRepository.findByIdOrNull(it) ?: throw ValidationException("$it payroll region code not found for hospital $hospitalId") }
    this.localAuthority = updateHospitalDto.localAuthorityCode?.let { localAuthorityRepository.findByIdOrNull(it) ?: throw ValidationException("$it local authority code not found for hospital $hospitalId") }
  }
}
