package uk.gov.justice.digital.hmpps.prisonregister.service

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.prisonregister.exceptions.ItemNotFoundException
import uk.gov.justice.digital.hmpps.prisonregister.model.Address
import uk.gov.justice.digital.hmpps.prisonregister.model.AgencyAddress
import uk.gov.justice.digital.hmpps.prisonregister.model.ApprovedPremises
import uk.gov.justice.digital.hmpps.prisonregister.model.ApprovedPremisesRepository
import uk.gov.justice.digital.hmpps.prisonregister.model.Area
import uk.gov.justice.digital.hmpps.prisonregister.model.Court
import uk.gov.justice.digital.hmpps.prisonregister.model.CourtRepository
import uk.gov.justice.digital.hmpps.prisonregister.model.EmailAddress
import uk.gov.justice.digital.hmpps.prisonregister.model.Hospital
import uk.gov.justice.digital.hmpps.prisonregister.model.HospitalRepository
import uk.gov.justice.digital.hmpps.prisonregister.model.OtherAgency
import uk.gov.justice.digital.hmpps.prisonregister.model.OtherAgencyRepository
import uk.gov.justice.digital.hmpps.prisonregister.model.OtherAgencyType
import uk.gov.justice.digital.hmpps.prisonregister.model.PhoneNumber
import uk.gov.justice.digital.hmpps.prisonregister.model.PoliceCustodySuite
import uk.gov.justice.digital.hmpps.prisonregister.model.PoliceCustodySuiteRepository
import uk.gov.justice.digital.hmpps.prisonregister.model.Prison
import uk.gov.justice.digital.hmpps.prisonregister.model.PrisonRepository
import uk.gov.justice.digital.hmpps.prisonregister.model.ProbationOffice
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
@Transactional
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
    courtRepository.findByIdOrNull(agencyId)?.let { return it.toAgencyDetailsDto() }
    hospitalRepository.findByIdOrNull(agencyId)?.let { return it.toAgencyDetailsDto() }
    policeCustodySuiteRepository.findByIdOrNull(agencyId)?.let { return it.toAgencyDetailsDto() }
    probationOfficeRepository.findByIdOrNull(agencyId)?.let { return it.toAgencyDetailsDto() }
    approvedPremisesRepository.findByIdOrNull(agencyId)?.let { return it.toAgencyDetailsDto() }
    prisonRepository.findByIdOrNull(agencyId)?.let { return it.toAgencyDetailsDto() }
    otherAgencyRepository.findByIdOrNull(agencyId)?.let { return it.toAgencyDetailsDto() }

    throw ItemNotFoundException("Agency $agencyId not found")
  }

  fun getAgenciesByType(type: LegacyAgencyType, activeOnly: Boolean): List<AgencyDetailsDto> {
    fun Boolean.matchesActiveFilter() = if (activeOnly) this else true

    val agencies = when (type) {
      LegacyAgencyType.COURT ->
        courtRepository.findAll().filter { it.active.matchesActiveFilter() }.map { it.toAgencyDetailsDto() }

      LegacyAgencyType.HOSPITAL ->
        hospitalRepository.findAll().filter { !it.highSecurity && it.active.matchesActiveFilter() }.map { it.toAgencyDetailsDto() }

      LegacyAgencyType.SECURE_HOSPITAL ->
        hospitalRepository.findAll().filter { it.highSecurity && it.active.matchesActiveFilter() }.map { it.toAgencyDetailsDto() }

      LegacyAgencyType.POLICE_CUSTODY_SUITE ->
        policeCustodySuiteRepository.findAll().filter { it.active.matchesActiveFilter() }.map { it.toAgencyDetailsDto() }

      LegacyAgencyType.PROBATION_OFFICE ->
        probationOfficeRepository.findAll().filter { it.active.matchesActiveFilter() }.map { it.toAgencyDetailsDto() }

      LegacyAgencyType.APPROVED_PREMISES ->
        approvedPremisesRepository.findAll().filter { it.active.matchesActiveFilter() }.map { it.toAgencyDetailsDto() }

      LegacyAgencyType.PRISON ->
        prisonRepository.findAll().filter { it.active.matchesActiveFilter() }.map { it.toAgencyDetailsDto() }

      else -> {
        val otherAgencyType = OtherAgencyType.valueOf(type.name)
        otherAgencyRepository.findAll()
          .filter { it.otherAgencyType == otherAgencyType && it.active.matchesActiveFilter() }
          .map { it.toAgencyDetailsDto() }
      }
    }

    return agencies.sortedBy { it.agencyId }
  }
}

private fun Court.toAgencyDetailsDto() = AgencyDetailsDto(
  agencyId = courtId,
  description = name,
  longDescription = description,
  agencyType = LegacyAgencyType.COURT,
  active = active,
  courtType = courtType.code,
  courtTypeDescription = courtType.description,
  inactiveDate = inactiveDate,
  area = area?.toCodeDescription(),
  region = region?.toCodeDescription(),
  addresses = addresses.map(AgencyAddress::toAgencyAddressDto),
  emails = emailAddresses.map(EmailAddress::toAgencyEmailDto),
  phones = phoneNumbers.map(PhoneNumber::toAgencyPhoneDto),
)

private fun Hospital.toAgencyDetailsDto() = AgencyDetailsDto(
  agencyId = hospitalId,
  description = name,
  longDescription = description,
  agencyType = if (highSecurity) LegacyAgencyType.SECURE_HOSPITAL else LegacyAgencyType.HOSPITAL,
  active = active,
  inactiveDate = inactiveDate,
  area = area?.toCodeDescription(),
  region = region?.toCodeDescription(),
  addresses = addresses.map(AgencyAddress::toAgencyAddressDto),
  emails = emptyList(),
  phones = phoneNumbers.map(PhoneNumber::toAgencyPhoneDto),
)

private fun PoliceCustodySuite.toAgencyDetailsDto() = AgencyDetailsDto(
  agencyId = policeCustodySuiteId,
  description = name,
  longDescription = description,
  agencyType = LegacyAgencyType.POLICE_CUSTODY_SUITE,
  active = active,
  inactiveDate = inactiveDate,
  area = area?.toCodeDescription(),
  region = region?.toCodeDescription(),
  addresses = addresses.map(AgencyAddress::toAgencyAddressDto),
  emails = emailAddresses.map(EmailAddress::toAgencyEmailDto),
  phones = phoneNumbers.map(PhoneNumber::toAgencyPhoneDto),
)

private fun ProbationOffice.toAgencyDetailsDto() = AgencyDetailsDto(
  agencyId = probationOfficeId,
  description = name,
  longDescription = description,
  agencyType = LegacyAgencyType.PROBATION_OFFICE,
  active = active,
  inactiveDate = inactiveDate,
  area = area?.toCodeDescription(),
  region = region?.toCodeDescription(),
  addresses = addresses.map(AgencyAddress::toAgencyAddressDto),
  emails = emailAddresses.map(EmailAddress::toAgencyEmailDto),
  phones = phoneNumbers.map(PhoneNumber::toAgencyPhoneDto),
)

private fun ApprovedPremises.toAgencyDetailsDto() = AgencyDetailsDto(
  agencyId = approvedPremisesId,
  description = name,
  longDescription = description,
  agencyType = LegacyAgencyType.APPROVED_PREMISES,
  active = active,
  inactiveDate = inactiveDate,
  area = area?.toCodeDescription(),
  region = region?.toCodeDescription(),
  addresses = addresses.map(AgencyAddress::toAgencyAddressDto),
  emails = emailAddresses.map(EmailAddress::toAgencyEmailDto),
  phones = phoneNumbers.map(PhoneNumber::toAgencyPhoneDto),
)

private fun Prison.toAgencyDetailsDto() = AgencyDetailsDto(
  agencyId = prisonId,
  description = name,
  longDescription = description,
  agencyType = LegacyAgencyType.PRISON,
  active = active,
  inactiveDate = inactiveDate,
  addresses = addresses.map(Address::toAgencyAddressDto),
  emails = emptyList(),
  phones = emptyList(),
)

private fun OtherAgency.toAgencyDetailsDto() = AgencyDetailsDto(
  agencyId = agencyId,
  description = name,
  longDescription = description,
  agencyType = LegacyAgencyType.valueOf(otherAgencyType.name),
  active = active,
  inactiveDate = inactiveDate,
  area = area?.toCodeDescription(),
  region = region?.toCodeDescription(),
  addresses = addresses.map(AgencyAddress::toAgencyAddressDto),
  emails = emailAddresses.map(EmailAddress::toAgencyEmailDto),
  phones = phoneNumbers.map(PhoneNumber::toAgencyPhoneDto),
)

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
