package uk.gov.justice.digital.hmpps.prisonregister.service

import jakarta.persistence.EntityNotFoundException
import jakarta.validation.ValidationException
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.prisonregister.model.AreaRepository
import uk.gov.justice.digital.hmpps.prisonregister.model.Hospital
import uk.gov.justice.digital.hmpps.prisonregister.model.HospitalRepository
import uk.gov.justice.digital.hmpps.prisonregister.model.LocalAuthorityRepository
import uk.gov.justice.digital.hmpps.prisonregister.model.PayrollRegionRepository
import uk.gov.justice.digital.hmpps.prisonregister.model.RegionRepository
import uk.gov.justice.digital.hmpps.prisonregister.resource.HospitalDto
import uk.gov.justice.digital.hmpps.prisonregister.resource.LegacyAgencyAddressDto
import uk.gov.justice.digital.hmpps.prisonregister.resource.LegacyAgencyDto
import uk.gov.justice.digital.hmpps.prisonregister.resource.LegacyAgencyPhoneDto
import uk.gov.justice.digital.hmpps.prisonregister.resource.LegacyAgencyResponse
import uk.gov.justice.digital.hmpps.prisonregister.resource.LegacyAgencyType
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
) {
  fun deleteAll() {
    hospitalRepository.deleteAll()
  }

  fun getAllIds(): List<String> = hospitalRepository.findAll().map { it.hospitalId }

  fun findById(hospitalId: String): HospitalDto = hospitalRepository.findByIdOrNull(hospitalId)?.let {
    HospitalDto(
      hospitalId = it.hospitalId,
      hospitalName = it.name,
      description = it.description,
      active = it.active,
      inactiveDate = it.inactiveDate,
      cjitCode = it.cjitCode,
      area = it.area?.let { area -> CodeDescription(area.code, area.description) },
      region = it.region?.let { area -> CodeDescription(area.code, area.description) },
      geographicalArea = it.geographicalArea?.let { area -> CodeDescription(area.code, area.description) },
      payrollRegion = it.payrollRegion?.let { area -> CodeDescription(area.code, area.description) },
      localAuthority = it.localAuthority?.let { localAuthority -> CodeDescription(localAuthority.code, localAuthority.description) },
      highSecurity = it.highSecurity,
      addresses = it.addresses.map { address ->
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
      phoneNumbers = it.phoneNumbers.map { phoneNumber ->
        AgencyPhoneDto(
          id = phoneNumber.id,
          number = phoneNumber.value,
        )
      },
    )
  } ?: throw EntityNotFoundException("Hospital $hospitalId not found")

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
}
