package uk.gov.justice.digital.hmpps.prisonregister.service

import jakarta.persistence.EntityNotFoundException
import jakarta.validation.ValidationException
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.prisonregister.model.AccessibleAccess
import uk.gov.justice.digital.hmpps.prisonregister.model.AreaRepository
import uk.gov.justice.digital.hmpps.prisonregister.model.LocalAuthorityRepository
import uk.gov.justice.digital.hmpps.prisonregister.model.ProbationOffice
import uk.gov.justice.digital.hmpps.prisonregister.model.ProbationOfficeRepository
import uk.gov.justice.digital.hmpps.prisonregister.model.RegionRepository
import uk.gov.justice.digital.hmpps.prisonregister.model.SubareaRepository
import uk.gov.justice.digital.hmpps.prisonregister.resource.LegacyAccessibleAccess
import uk.gov.justice.digital.hmpps.prisonregister.resource.LegacyAgencyAddressDto
import uk.gov.justice.digital.hmpps.prisonregister.resource.LegacyAgencyDto
import uk.gov.justice.digital.hmpps.prisonregister.resource.LegacyAgencyEmailDto
import uk.gov.justice.digital.hmpps.prisonregister.resource.LegacyAgencyPhoneDto
import uk.gov.justice.digital.hmpps.prisonregister.resource.LegacyAgencyResponse
import uk.gov.justice.digital.hmpps.prisonregister.resource.LegacyAgencyType
import uk.gov.justice.digital.hmpps.prisonregister.resource.ProbationOfficeDto
import uk.gov.justice.digital.hmpps.prisonregister.resource.dto.AgencyAddressDto
import uk.gov.justice.digital.hmpps.prisonregister.resource.dto.AgencyEmailDto
import uk.gov.justice.digital.hmpps.prisonregister.resource.dto.AgencyPhoneDto
import uk.gov.justice.digital.hmpps.prisonregister.resource.dto.CodeDescription

@Service
@Transactional
class ProbationOfficeService(
  private val probationOfficeRepository: ProbationOfficeRepository,
  private val areaRepository: AreaRepository,
  private val subareaRepository: SubareaRepository,
  private val regionRepository: RegionRepository,
  private val localAuthorityRepository: LocalAuthorityRepository,
) {
  fun deleteAll() {
    probationOfficeRepository.deleteAll()
  }

  fun getAllIds(): List<String> = probationOfficeRepository.findAll().map { it.probationOfficeId }

  fun findById(probationOfficeId: String): ProbationOfficeDto = probationOfficeRepository.findByIdOrNull(probationOfficeId)?.let {
    ProbationOfficeDto(
      probationOfficeId = it.probationOfficeId,
      probationOfficeName = it.name,
      description = it.description,
      active = it.active,
      accessibleAccess = it.accessibleAccess?.name,
      inactiveDate = it.inactiveDate,
      cjitCode = it.cjitCode,
      area = it.area?.let { area -> CodeDescription(area.code, area.description) },
      subarea = it.subarea?.let { subarea -> CodeDescription(subarea.code, subarea.description) },
      region = it.region?.let { region -> CodeDescription(region.code, region.description) },
      geographicalArea = it.geographicalArea?.let { area -> CodeDescription(area.code, area.description) },
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
  } ?: throw EntityNotFoundException("Probation office $probationOfficeId not found")

  fun tryFindById(agencyId: String): LegacyAgencyDto? = probationOfficeRepository.findByIdOrNull(agencyId)?.let { po ->
    LegacyAgencyDto(
      agencyType = LegacyAgencyType.PROBATION_OFFICE,
      name = po.name,
      description = po.description,
      active = po.active,
      inactiveDate = po.inactiveDate,
      cjitCode = po.cjitCode,
      areaCode = po.area?.code,
      subareaCode = po.subarea?.code,
      regionCode = po.region?.code,
      geographicalAreaCode = po.geographicalArea?.code,
      payrollRegionCode = null,
      localAuthorityCode = po.localAuthority?.code,
      courtTypeCode = null,
      accessibleAccess = po.accessibleAccess?.let { runCatching { LegacyAccessibleAccess.valueOf(it.name) }.getOrNull() },
      contact = null,
      addresses = po.addresses.map { LegacyAgencyAddressDto(it.addressLine1, it.addressLine2, it.town, it.county, it.postcode, it.country) },
      emailAddresses = po.emailAddresses.map { LegacyAgencyEmailDto(it.value) },
      phoneNumbers = po.phoneNumbers.map { LegacyAgencyPhoneDto(it.value) },
    )
  }

  fun createOrUpdateProbationOfficeFromLegacyData(probationOfficeId: String, agencyDto: LegacyAgencyDto): LegacyAgencyResponse = probationOfficeRepository.findByIdOrNull(probationOfficeId)?.let { probationOffice ->
    probationOffice.update(agencyDto)
    if (probationOffice.addresses.size == 1 && agencyDto.addresses.size == 1) {
      probationOffice.addresses[0].update(agencyDto.addresses[0])
    } else {
      probationOffice.addresses.clear()
      probationOffice.addresses += agencyDto.addresses.map { it.toAgencyAddress() }
    }

    probationOffice.phoneNumbers.updatePhoneNumberFrom(agencyDto.phoneNumbers)
    probationOffice.emailAddresses.updateEmailAddressFrom(agencyDto.emailAddresses)

    LegacyAgencyResponse(updated = true)
  } ?: let {
    val probationOffice = agencyDto.toProbationOffice(probationOfficeId)
    probationOffice.addresses += agencyDto.addresses.map { it.toAgencyAddress() }
    probationOffice.phoneNumbers += agencyDto.phoneNumbers.map { it.toAgencyPhone() }
    probationOffice.emailAddresses += agencyDto.emailAddresses.map { it.toAgencyEmail() }
    probationOfficeRepository.saveAndFlush(probationOffice)
    LegacyAgencyResponse(updated = false)
  }

  private fun LegacyAgencyDto.toProbationOffice(probationOfficeId: String) = ProbationOffice(
    probationOfficeId = probationOfficeId,
    name = this.name,
    description = this.description,
    active = this.active,
    accessibleAccess = this.accessibleAccess?.let { AccessibleAccess.valueOf(it.name) },
    inactiveDate = this.inactiveDate,
    cjitCode = this.cjitCode,
    area = this.areaCode?.let { areaRepository.findByIdOrNull(it) ?: throw ValidationException("$it area code not found for agency $probationOfficeId") },
    subarea = this.subareaCode?.let { subareaRepository.findByIdOrNull(it) ?: throw ValidationException("$it subarea code not found for agency $probationOfficeId") },
    region = this.regionCode?.let { regionRepository.findByIdOrNull(it) ?: throw ValidationException("$it region code not found for agency $probationOfficeId") },
    geographicalArea = this.geographicalAreaCode?.let { areaRepository.findByIdOrNull(it) ?: throw ValidationException("$it geographical area code not found for agency $probationOfficeId") },
    localAuthority = this.localAuthorityCode?.let { localAuthorityRepository.findByIdOrNull(it) ?: throw ValidationException("$it local authority code not found for agency $probationOfficeId") },
  )

  private fun ProbationOffice.update(agencyDto: LegacyAgencyDto) {
    this.name = agencyDto.name
    this.description = agencyDto.description
    this.active = agencyDto.active
    this.inactiveDate = agencyDto.inactiveDate
    this.cjitCode = agencyDto.cjitCode
    this.accessibleAccess = agencyDto.accessibleAccess?.let { AccessibleAccess.valueOf(it.name) }
    this.area = agencyDto.areaCode?.let { areaRepository.findByIdOrNull(it) ?: throw ValidationException("$it area code not found for agency $probationOfficeId") }
    this.subarea = agencyDto.subareaCode?.let { subareaRepository.findByIdOrNull(it) ?: throw ValidationException("$it subarea code not found for agency $probationOfficeId") }
    this.region = agencyDto.regionCode?.let { regionRepository.findByIdOrNull(it) ?: throw ValidationException("$it region code not found for agency $probationOfficeId") }
    this.geographicalArea = agencyDto.geographicalAreaCode?.let { areaRepository.findByIdOrNull(it) ?: throw ValidationException("$it geographical area code not found for agency $probationOfficeId") }
    this.localAuthority = agencyDto.localAuthorityCode?.let { localAuthorityRepository.findByIdOrNull(it) ?: throw ValidationException("$it local authority code not found for agency $probationOfficeId") }
  }
}
