package uk.gov.justice.digital.hmpps.prisonregister.service

import jakarta.persistence.EntityNotFoundException
import jakarta.validation.ValidationException
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.prisonregister.model.AccessibleAccess
import uk.gov.justice.digital.hmpps.prisonregister.model.AreaRepository
import uk.gov.justice.digital.hmpps.prisonregister.model.ProbationOffice
import uk.gov.justice.digital.hmpps.prisonregister.model.ProbationOfficeRepository
import uk.gov.justice.digital.hmpps.prisonregister.model.RegionRepository
import uk.gov.justice.digital.hmpps.prisonregister.resource.LegacyAgencyDto
import uk.gov.justice.digital.hmpps.prisonregister.resource.LegacyAgencyResponse
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
  private val regionRepository: RegionRepository,
) {
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
      region = it.region?.let { region -> CodeDescription(region.code, region.description) },
      geographicalArea = it.geographicalArea?.let { area -> CodeDescription(area.code, area.description) },
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
    region = this.regionCode?.let { regionRepository.findByIdOrNull(it) ?: throw ValidationException("$it region code not found for agency $probationOfficeId") },
    geographicalArea = this.geographicalAreaCode?.let { areaRepository.findByIdOrNull(it) ?: throw ValidationException("$it geographical area code not found for agency $probationOfficeId") },
  )

  private fun ProbationOffice.update(agencyDto: LegacyAgencyDto) {
    this.name = agencyDto.name
    this.description = agencyDto.description
    this.active = agencyDto.active
    this.inactiveDate = agencyDto.inactiveDate
    this.cjitCode = agencyDto.cjitCode
    this.area = agencyDto.areaCode?.let { areaRepository.findByIdOrNull(it) ?: throw ValidationException("$it area code not found for agency $probationOfficeId") }
    this.region = agencyDto.regionCode?.let { regionRepository.findByIdOrNull(it) ?: throw ValidationException("$it region code not found for agency $probationOfficeId") }
    this.geographicalArea = agencyDto.geographicalAreaCode?.let { areaRepository.findByIdOrNull(it) ?: throw ValidationException("$it geographical area code not found for agency $probationOfficeId") }
  }
}
