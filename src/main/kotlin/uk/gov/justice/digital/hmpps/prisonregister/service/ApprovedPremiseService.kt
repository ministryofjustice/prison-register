package uk.gov.justice.digital.hmpps.prisonregister.service

import jakarta.persistence.EntityNotFoundException
import jakarta.validation.ValidationException
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.prisonregister.model.AccessibleAccess
import uk.gov.justice.digital.hmpps.prisonregister.model.ApprovedPremise
import uk.gov.justice.digital.hmpps.prisonregister.model.ApprovedPremiseRepository
import uk.gov.justice.digital.hmpps.prisonregister.model.AreaRepository
import uk.gov.justice.digital.hmpps.prisonregister.model.LocalAuthorityRepository
import uk.gov.justice.digital.hmpps.prisonregister.model.RegionRepository
import uk.gov.justice.digital.hmpps.prisonregister.resource.ApprovedPremiseDto
import uk.gov.justice.digital.hmpps.prisonregister.resource.LegacyAgencyDto
import uk.gov.justice.digital.hmpps.prisonregister.resource.LegacyAgencyResponse
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
  private val localAuthorityRepository: LocalAuthorityRepository,
) {
  fun deleteAll() {
    approvedPremiseRepository.deleteAll()
  }

  fun findById(approvedPremiseId: String): ApprovedPremiseDto = approvedPremiseRepository.findByIdOrNull(approvedPremiseId)?.let {
    ApprovedPremiseDto(
      approvedPremiseId = it.approvedPremiseId,
      approvedPremiseName = it.name,
      description = it.description,
      contact = it.contact,
      active = it.active,
      accessibleAccess = it.accessibleAccess?.name,
      inactiveDate = it.inactiveDate,
      cjitCode = it.cjitCode,
      area = it.area?.let { area -> CodeDescription(area.code, area.description) },
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
  } ?: throw EntityNotFoundException("Approved premise $approvedPremiseId not found")

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
    this.localAuthority = agencyDto.localAuthorityCode?.let { localAuthorityRepository.findByIdOrNull(it) ?: throw ValidationException("$it local authority code not found for agency $approvedPremiseId") }
  }
}
