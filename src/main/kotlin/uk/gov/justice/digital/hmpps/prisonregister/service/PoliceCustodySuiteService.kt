package uk.gov.justice.digital.hmpps.prisonregister.service

import jakarta.persistence.EntityNotFoundException
import jakarta.validation.ValidationException
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.prisonregister.model.AreaRepository
import uk.gov.justice.digital.hmpps.prisonregister.model.LocalAuthorityRepository
import uk.gov.justice.digital.hmpps.prisonregister.model.PayrollRegionRepository
import uk.gov.justice.digital.hmpps.prisonregister.model.PoliceCustodySuite
import uk.gov.justice.digital.hmpps.prisonregister.model.PoliceCustodySuiteRepository
import uk.gov.justice.digital.hmpps.prisonregister.model.RegionRepository
import uk.gov.justice.digital.hmpps.prisonregister.resource.LegacyAgencyAddressDto
import uk.gov.justice.digital.hmpps.prisonregister.resource.LegacyAgencyDto
import uk.gov.justice.digital.hmpps.prisonregister.resource.LegacyAgencyEmailDto
import uk.gov.justice.digital.hmpps.prisonregister.resource.LegacyAgencyPhoneDto
import uk.gov.justice.digital.hmpps.prisonregister.resource.LegacyAgencyResponse
import uk.gov.justice.digital.hmpps.prisonregister.resource.LegacyAgencyType
import uk.gov.justice.digital.hmpps.prisonregister.resource.PoliceCustodySuiteDto
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
) {
  fun deleteAll() {
    policeCustodySuiteRepository.deleteAll()
  }

  fun getAllIds(): List<String> = policeCustodySuiteRepository.findAll().map { it.policeCustodySuiteId }

  fun findById(policeCustodySuiteId: String): PoliceCustodySuiteDto = policeCustodySuiteRepository.findByIdOrNull(policeCustodySuiteId)?.let {
    PoliceCustodySuiteDto(
      policeCustodySuiteId = it.policeCustodySuiteId,
      policeCustodySuiteName = it.name,
      description = it.description,
      active = it.active,
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
  } ?: throw EntityNotFoundException("Police custody suite $policeCustodySuiteId not found")

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
}
