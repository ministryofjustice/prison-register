package uk.gov.justice.digital.hmpps.prisonregister.service

import jakarta.persistence.EntityNotFoundException
import jakarta.validation.ValidationException
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.prisonregister.model.AreaRepository
import uk.gov.justice.digital.hmpps.prisonregister.model.Court
import uk.gov.justice.digital.hmpps.prisonregister.model.CourtRepository
import uk.gov.justice.digital.hmpps.prisonregister.model.CourtTypeRepository
import uk.gov.justice.digital.hmpps.prisonregister.model.RegionRepository
import uk.gov.justice.digital.hmpps.prisonregister.resource.CourtDto
import uk.gov.justice.digital.hmpps.prisonregister.resource.LegacyAgencyAddressDto
import uk.gov.justice.digital.hmpps.prisonregister.resource.LegacyAgencyDto
import uk.gov.justice.digital.hmpps.prisonregister.resource.LegacyAgencyEmailDto
import uk.gov.justice.digital.hmpps.prisonregister.resource.LegacyAgencyPhoneDto
import uk.gov.justice.digital.hmpps.prisonregister.resource.LegacyAgencyResponse
import uk.gov.justice.digital.hmpps.prisonregister.resource.LegacyAgencyType
import uk.gov.justice.digital.hmpps.prisonregister.resource.dto.AgencyAddressDto
import uk.gov.justice.digital.hmpps.prisonregister.resource.dto.AgencyEmailDto
import uk.gov.justice.digital.hmpps.prisonregister.resource.dto.AgencyPhoneDto
import uk.gov.justice.digital.hmpps.prisonregister.resource.dto.CodeDescription

@Service
@Transactional
class CourtService(
  private val courtRepository: CourtRepository,
  private val areaRepository: AreaRepository,
  private val regionRepository: RegionRepository,
  private val courtTypeRepository: CourtTypeRepository,
) {
  fun deleteAll() {
    courtRepository.deleteAll()
  }

  fun getAllIds(): List<String> = courtRepository.findAll().map { it.courtId }

  fun findById(courtId: String): CourtDto = courtRepository.findByIdOrNull(courtId)?.let {
    CourtDto(
      courtId = it.courtId,
      courtName = it.name,
      description = it.description,
      active = it.active,
      inactiveDate = it.inactiveDate,
      cjitCode = it.cjitCode,
      area = it.area?.let { area -> CodeDescription(area.code, area.description) },
      region = it.region?.let { area -> CodeDescription(area.code, area.description) },
      geographicalArea = it.geographicalArea?.let { area -> CodeDescription(area.code, area.description) },
      courtType = CodeDescription(it.courtType.code, it.courtType.description),
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
  } ?: throw EntityNotFoundException("Court $courtId not found")

  fun tryFindById(agencyId: String): LegacyAgencyDto? = courtRepository.findByIdOrNull(agencyId)?.let { court ->
    LegacyAgencyDto(
      agencyType = LegacyAgencyType.COURT,
      name = court.name,
      description = court.description,
      active = court.active,
      inactiveDate = court.inactiveDate,
      cjitCode = court.cjitCode,
      areaCode = court.area?.code,
      regionCode = court.region?.code,
      geographicalAreaCode = court.geographicalArea?.code,
      payrollRegionCode = null,
      localAuthorityCode = null,
      courtTypeCode = court.courtType.code,
      accessibleAccess = null,
      contact = null,
      addresses = court.addresses.map { LegacyAgencyAddressDto(it.addressLine1, it.addressLine2, it.town, it.county, it.postcode, it.country) },
      emailAddresses = court.emailAddresses.map { LegacyAgencyEmailDto(it.value) },
      phoneNumbers = court.phoneNumbers.map { LegacyAgencyPhoneDto(it.value) },
    )
  }

  fun createOrUpdateCourtFromLegacyData(courtId: String, agencyDto: LegacyAgencyDto): LegacyAgencyResponse = courtRepository.findByIdOrNull(courtId)?.let { court ->
    court.update(agencyDto)
    // treat these as value objects and replace. If we are just updating a single address (vast majority of agencies will have at most one), keep the original entity and its ID
    if (court.addresses.size == 1 && agencyDto.addresses.size == 1) {
      court.addresses[0].update(agencyDto.addresses[0])
    } else {
      court.addresses.clear()
      court.addresses += agencyDto.addresses.map { it.toAgencyAddress() }
    }

    court.phoneNumbers.updatePhoneNumberFrom(agencyDto.phoneNumbers)
    court.emailAddresses.updateEmailAddressFrom(agencyDto.emailAddresses)

    LegacyAgencyResponse(updated = true)
  } ?: let {
    val court = agencyDto.toCourt(courtId)
    court.addresses += agencyDto.addresses.map { it.toAgencyAddress() }
    court.phoneNumbers += agencyDto.phoneNumbers.map { it.toAgencyPhone() }
    court.emailAddresses += agencyDto.emailAddresses.map { it.toAgencyEmail() }
    courtRepository.saveAndFlush(court)
    LegacyAgencyResponse(updated = false)
  }

  private fun LegacyAgencyDto.toCourt(courtId: String) = Court(
    courtId = courtId,
    name = this.name,
    description = this.description,
    active = this.active,
    inactiveDate = this.inactiveDate,
    cjitCode = this.cjitCode,
    area = this.areaCode?.let { areaRepository.findByIdOrNull(it) ?: throw ValidationException("$it area code not found for agency $courtId") },
    region = this.regionCode?.let { regionRepository.findByIdOrNull(it) ?: throw ValidationException("$it region code not found for agency $courtId") },
    geographicalArea = this.geographicalAreaCode?.let { areaRepository.findByIdOrNull(it) ?: throw ValidationException("$it geographical area code not found for agency $courtId") },
    courtType = (this.courtTypeCode ?: "UNK").let { courtTypeRepository.findByIdOrNull(it) } ?: throw ValidationException("$courtTypeCode court type not found for agency $courtId"),
  )

  private fun Court.update(agencyDto: LegacyAgencyDto) {
    this.name = agencyDto.name
    this.description = agencyDto.description
    this.active = agencyDto.active
    this.inactiveDate = agencyDto.inactiveDate
    this.cjitCode = agencyDto.cjitCode
    this.area = agencyDto.areaCode?.let { areaRepository.findByIdOrNull(it) ?: throw ValidationException("$it area code not found for agency $courtId") }
    this.region = agencyDto.regionCode?.let { regionRepository.findByIdOrNull(it) ?: throw ValidationException("$it region code not found for agency $courtId") }
    this.geographicalArea = agencyDto.geographicalAreaCode?.let { areaRepository.findByIdOrNull(it) ?: throw ValidationException("$it geographical area code not found for agency $courtId") }
    this.courtType = (agencyDto.courtTypeCode ?: "UNK").let { courtTypeRepository.findByIdOrNull(it) } ?: throw ValidationException("${agencyDto.courtTypeCode} court type not found for agency $courtId")
  }
}
