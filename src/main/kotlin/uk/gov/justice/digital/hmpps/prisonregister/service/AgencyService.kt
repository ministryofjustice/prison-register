package uk.gov.justice.digital.hmpps.prisonregister.service

import jakarta.persistence.EntityNotFoundException
import jakarta.validation.ValidationException
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.prisonregister.model.AccessibleAccess
import uk.gov.justice.digital.hmpps.prisonregister.model.Agency
import uk.gov.justice.digital.hmpps.prisonregister.model.AgencyRepository
import uk.gov.justice.digital.hmpps.prisonregister.model.AgencyType
import uk.gov.justice.digital.hmpps.prisonregister.model.AreaRepository
import uk.gov.justice.digital.hmpps.prisonregister.model.LocalAuthorityRepository
import uk.gov.justice.digital.hmpps.prisonregister.model.PayrollRegionRepository
import uk.gov.justice.digital.hmpps.prisonregister.model.RegionRepository
import uk.gov.justice.digital.hmpps.prisonregister.resource.AgencyDto
import uk.gov.justice.digital.hmpps.prisonregister.resource.LegacyAgencyDto
import uk.gov.justice.digital.hmpps.prisonregister.resource.LegacyAgencyResponse
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
) {
  fun deleteAll() {
    agencyRepository.deleteAll()
  }

  fun findById(agencyId: String): AgencyDto = agencyRepository.findByIdOrNull(agencyId)?.let {
    AgencyDto(
      agencyId = it.agencyId,
      agencyName = it.name,
      description = it.description,
      active = it.active,
      accessibleAccess = it.accessibleAccess?.name,
      agencyType = it.agencyType.name,
      inactiveDate = it.inactiveDate,
      cjitCode = it.cjitCode,
      area = it.area?.let { area -> CodeDescription(area.code, area.description) },
      region = it.region?.let { region -> CodeDescription(region.code, region.description) },
      geographicalArea = it.geographicalArea?.let { area -> CodeDescription(area.code, area.description) },
      payrollRegion = it.payrollRegion?.let { pr -> CodeDescription(pr.code, pr.description) },
      localAuthority = it.localAuthority?.let { localAuthority -> CodeDescription(localAuthority.code, localAuthority.description) },
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
      emailAddresses = it.emailAddresses.map { emailAddress ->
        AgencyEmailDto(
          id = emailAddress.id,
          address = emailAddress.value,
        )
      },
      phoneNumbers = it.phoneNumbers.map { phoneNumber ->
        AgencyPhoneDto(
          id = phoneNumber.id,
          number = phoneNumber.value,
        )
      },
    )
  } ?: throw EntityNotFoundException("Agency $agencyId not found")

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
}
