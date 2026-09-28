package uk.gov.justice.digital.hmpps.prisonregister.service

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.prisonregister.exceptions.ItemNotFoundException
import uk.gov.justice.digital.hmpps.prisonregister.model.Address
import uk.gov.justice.digital.hmpps.prisonregister.model.AgencyAddress
import uk.gov.justice.digital.hmpps.prisonregister.model.ApprovedPremisesRepository
import uk.gov.justice.digital.hmpps.prisonregister.model.Area
import uk.gov.justice.digital.hmpps.prisonregister.model.CourtRepository
import uk.gov.justice.digital.hmpps.prisonregister.model.EmailAddress
import uk.gov.justice.digital.hmpps.prisonregister.model.HospitalRepository
import uk.gov.justice.digital.hmpps.prisonregister.model.OtherAgencyRepository
import uk.gov.justice.digital.hmpps.prisonregister.model.PhoneNumber
import uk.gov.justice.digital.hmpps.prisonregister.model.PoliceCustodySuiteRepository
import uk.gov.justice.digital.hmpps.prisonregister.model.PrisonRepository
import uk.gov.justice.digital.hmpps.prisonregister.model.ProbationOfficeRepository
import uk.gov.justice.digital.hmpps.prisonregister.model.Region
import uk.gov.justice.digital.hmpps.prisonregister.resource.AgencyDetailsDto
import uk.gov.justice.digital.hmpps.prisonregister.resource.AgencySummaryDto
import uk.gov.justice.digital.hmpps.prisonregister.resource.LegacyAgencyType
import uk.gov.justice.digital.hmpps.prisonregister.resource.dto.AgencyAddressDto
import uk.gov.justice.digital.hmpps.prisonregister.resource.dto.AgencyEmailDto
import uk.gov.justice.digital.hmpps.prisonregister.resource.dto.AgencyPhoneDto
import uk.gov.justice.digital.hmpps.prisonregister.resource.dto.CodeDescription

@Service
class PublicApiService(
  private val courtRepository: CourtRepository,
  private val hospitalRepository: HospitalRepository,
  private val policeCustodySuiteRepository: PoliceCustodySuiteRepository,
  private val probationOfficeRepository: ProbationOfficeRepository,
  private val approvedPremisesRepository: ApprovedPremisesRepository,
  private val prisonRepository: PrisonRepository,
  private val otherAgencyRepository: OtherAgencyRepository,
) {
  fun getAll(): List<AgencySummaryDto> {
    val courts = courtRepository.findAll().map {
      AgencySummaryDto(agencyId = it.courtId, description = it.description, agencyType = LegacyAgencyType.COURT, active = it.active)
    }

    val hospitals = hospitalRepository.findAll().map {
      val agencyType = if (it.highSecurity) LegacyAgencyType.SECURE_HOSPITAL else LegacyAgencyType.HOSPITAL
      AgencySummaryDto(agencyId = it.hospitalId, description = it.description, agencyType = agencyType, active = it.active)
    }

    val policeCustodySuites = policeCustodySuiteRepository.findAll().map {
      AgencySummaryDto(agencyId = it.policeCustodySuiteId, description = it.description, agencyType = LegacyAgencyType.POLICE_CUSTODY_SUITE, active = it.active)
    }

    val probationOffices = probationOfficeRepository.findAll().map {
      AgencySummaryDto(agencyId = it.probationOfficeId, description = it.description, agencyType = LegacyAgencyType.PROBATION_OFFICE, active = it.active)
    }

    val prisons = prisonRepository.findAll().map {
      AgencySummaryDto(agencyId = it.prisonId, description = it.description, agencyType = LegacyAgencyType.PRISON, active = it.active)
    }

    val agencies = otherAgencyRepository.findAll().map {
      AgencySummaryDto(agencyId = it.agencyId, description = it.description, agencyType = LegacyAgencyType.valueOf(it.otherAgencyType.name), active = it.active)
    }

    return (courts + hospitals + policeCustodySuites + probationOffices + prisons + agencies)
      .sortedBy { it.agencyId }
  }

  fun getAgency(agencyId: String): AgencyDetailsDto {
    courtRepository.findByIdOrNull(agencyId)?.let {
      return AgencyDetailsDto(
        agencyId = it.courtId,
        description = it.name,
        longDescription = it.description,
        agencyType = LegacyAgencyType.COURT,
        active = it.active,
        courtType = it.courtType.code,
        courtTypeDescription = it.courtType.description,
        inactiveDate = it.inactiveDate,
        area = it.area?.toCodeDescription(),
        region = it.region?.toCodeDescription(),
        addresses = it.addresses.map(AgencyAddress::toAgencyAddressDto),
        emails = it.emailAddresses.map(EmailAddress::toAgencyEmailDto),
        phones = it.phoneNumbers.map(PhoneNumber::toAgencyPhoneDto),
      )
    }

    hospitalRepository.findByIdOrNull(agencyId)?.let {
      val agencyType = if (it.highSecurity) LegacyAgencyType.SECURE_HOSPITAL else LegacyAgencyType.HOSPITAL
      return AgencyDetailsDto(
        agencyId = it.hospitalId,
        description = it.name,
        longDescription = it.description,
        agencyType = agencyType,
        active = it.active,
        inactiveDate = it.inactiveDate,
        area = it.area?.toCodeDescription(),
        region = it.region?.toCodeDescription(),
        addresses = it.addresses.map(AgencyAddress::toAgencyAddressDto),
        emails = emptyList(),
        phones = it.phoneNumbers.map(PhoneNumber::toAgencyPhoneDto),
      )
    }

    policeCustodySuiteRepository.findByIdOrNull(agencyId)?.let {
      return AgencyDetailsDto(
        agencyId = it.policeCustodySuiteId,
        description = it.name,
        longDescription = it.description,
        agencyType = LegacyAgencyType.POLICE_CUSTODY_SUITE,
        active = it.active,
        inactiveDate = it.inactiveDate,
        area = it.area?.toCodeDescription(),
        region = it.region?.toCodeDescription(),
        addresses = it.addresses.map(AgencyAddress::toAgencyAddressDto),
        emails = it.emailAddresses.map(EmailAddress::toAgencyEmailDto),
        phones = it.phoneNumbers.map(PhoneNumber::toAgencyPhoneDto),
      )
    }

    probationOfficeRepository.findByIdOrNull(agencyId)?.let {
      return AgencyDetailsDto(
        agencyId = it.probationOfficeId,
        description = it.name,
        longDescription = it.description,
        agencyType = LegacyAgencyType.PROBATION_OFFICE,
        active = it.active,
        inactiveDate = it.inactiveDate,
        area = it.area?.toCodeDescription(),
        region = it.region?.toCodeDescription(),
        addresses = it.addresses.map(AgencyAddress::toAgencyAddressDto),
        emails = it.emailAddresses.map(EmailAddress::toAgencyEmailDto),
        phones = it.phoneNumbers.map(PhoneNumber::toAgencyPhoneDto),
      )
    }

    approvedPremisesRepository.findByIdOrNull(agencyId)?.let {
      return AgencyDetailsDto(
        agencyId = it.approvedPremisesId,
        description = it.name,
        longDescription = it.description,
        agencyType = LegacyAgencyType.APPROVED_PREMISES,
        active = it.active,
        inactiveDate = it.inactiveDate,
        area = it.area?.toCodeDescription(),
        region = it.region?.toCodeDescription(),
        addresses = it.addresses.map(AgencyAddress::toAgencyAddressDto),
        emails = it.emailAddresses.map(EmailAddress::toAgencyEmailDto),
        phones = it.phoneNumbers.map(PhoneNumber::toAgencyPhoneDto),
      )
    }

    prisonRepository.findByIdOrNull(agencyId)?.let {
      return AgencyDetailsDto(
        agencyId = it.prisonId,
        description = it.name,
        longDescription = it.description,
        agencyType = LegacyAgencyType.PRISON,
        active = it.active,
        inactiveDate = it.inactiveDate,
        addresses = it.addresses.map(Address::toAgencyAddressDto),
        emails = emptyList(),
        phones = emptyList(),
      )
    }

    otherAgencyRepository.findByIdOrNull(agencyId)?.let {
      return AgencyDetailsDto(
        agencyId = it.agencyId,
        description = it.name,
        longDescription = it.description,
        agencyType = LegacyAgencyType.valueOf(it.otherAgencyType.name),
        active = it.active,
        inactiveDate = it.inactiveDate,
        area = it.area?.toCodeDescription(),
        region = it.region?.toCodeDescription(),
        addresses = it.addresses.map(AgencyAddress::toAgencyAddressDto),
        emails = it.emailAddresses.map(EmailAddress::toAgencyEmailDto),
        phones = it.phoneNumbers.map(PhoneNumber::toAgencyPhoneDto),
      )
    }

    throw ItemNotFoundException("Agency $agencyId not found")
  }
}

private fun AgencyAddress.toAgencyAddressDto() = AgencyAddressDto(
  id = this.id,
  addressLine1 = this.addressLine1,
  addressLine2 = this.addressLine2,
  town = this.town,
  county = this.county,
  postcode = this.postcode,
  country = this.country,
)

private fun Address.toAgencyAddressDto() = AgencyAddressDto(
  id = this.id ?: -1,
  addressLine1 = this.addressLine1,
  addressLine2 = this.addressLine2,
  town = this.town,
  county = this.county,
  postcode = this.postcode,
  country = this.country,
)

private fun PhoneNumber.toAgencyPhoneDto() = AgencyPhoneDto(id = this.id, number = this.value)

private fun EmailAddress.toAgencyEmailDto() = AgencyEmailDto(id = this.id, address = this.value)

private fun Area.toCodeDescription() = CodeDescription(code, description)

private fun Region.toCodeDescription() = CodeDescription(code, description)
