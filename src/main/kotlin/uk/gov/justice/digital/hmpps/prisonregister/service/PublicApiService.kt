package uk.gov.justice.digital.hmpps.prisonregister.service

import jakarta.validation.ValidationException
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.prisonregister.exceptions.ItemNotFoundException
import uk.gov.justice.digital.hmpps.prisonregister.model.Address
import uk.gov.justice.digital.hmpps.prisonregister.model.AgencyAddress
import uk.gov.justice.digital.hmpps.prisonregister.model.AllAgencyRepository
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
@Transactional(readOnly = true)
class PublicApiService(
  private val courtRepository: CourtRepository,
  private val hospitalRepository: HospitalRepository,
  private val policeCustodySuiteRepository: PoliceCustodySuiteRepository,
  private val probationOfficeRepository: ProbationOfficeRepository,
  private val approvedPremisesRepository: ApprovedPremisesRepository,
  private val prisonRepository: PrisonRepository,
  private val otherAgencyRepository: OtherAgencyRepository,
  private val allAgencyRepository: AllAgencyRepository,
) {
  fun getAll(activeOnly: Boolean = true): List<AgencySummaryDto> {
    val courts = courts(activeOnly).map {
      AgencySummaryDto(agencyId = it.courtId, description = it.description, agencyType = LegacyAgencyType.COURT, active = it.active)
    }

    val hospitals = (if (activeOnly) hospitalRepository.findAllByActiveTrue() else hospitalRepository.findAll()).map {
      val agencyType = if (it.highSecurity) LegacyAgencyType.SECURE_HOSPITAL else LegacyAgencyType.HOSPITAL
      AgencySummaryDto(agencyId = it.hospitalId, description = it.description, agencyType = agencyType, active = it.active)
    }

    val policeCustodySuites = policeCustodySuites(activeOnly).map {
      AgencySummaryDto(agencyId = it.policeCustodySuiteId, description = it.description, agencyType = LegacyAgencyType.POLICE_CUSTODY_SUITE, active = it.active)
    }

    val probationOffices = probationOffices(activeOnly).map {
      AgencySummaryDto(agencyId = it.probationOfficeId, description = it.description, agencyType = LegacyAgencyType.PROBATION_OFFICE, active = it.active)
    }

    val prisons = prisons(activeOnly).map {
      AgencySummaryDto(agencyId = it.prisonId, description = it.description, agencyType = LegacyAgencyType.PRISON, active = it.active)
    }

    val agencies = (if (activeOnly) otherAgencyRepository.findAllByActiveTrue() else otherAgencyRepository.findAll()).map {
      AgencySummaryDto(agencyId = it.agencyId, description = it.description, agencyType = LegacyAgencyType.valueOf(it.otherAgencyType.name), active = it.active)
    }

    return (courts + hospitals + policeCustodySuites + probationOffices + prisons + agencies)
      .sortedBy { it.agencyId }
  }

  // Find which table the agency is in with one lookup on the all_agencies view, then load it from
  // that table only, rather than trying each agency table in turn (MAPB-1127)
  @Transactional(readOnly = true)
  fun getAgency(agencyId: String): AgencyDetailsDto = when (allAgencyRepository.findFirstByAgencyId(agencyId)?.legacyType) {
    null -> null
    LegacyAgencyType.COURT -> courtRepository.findWithDetailsByCourtId(agencyId)?.toAgencyDetailsDto()
    LegacyAgencyType.HOSPITAL, LegacyAgencyType.SECURE_HOSPITAL -> hospitalRepository.findWithDetailsByHospitalId(agencyId)?.toAgencyDetailsDto()
    LegacyAgencyType.POLICE_CUSTODY_SUITE -> policeCustodySuiteRepository.findWithDetailsByPoliceCustodySuiteId(agencyId)?.toAgencyDetailsDto()
    LegacyAgencyType.PROBATION_OFFICE -> probationOfficeRepository.findWithDetailsByProbationOfficeId(agencyId)?.toAgencyDetailsDto()
    LegacyAgencyType.APPROVED_PREMISES -> approvedPremisesRepository.findWithDetailsByApprovedPremisesId(agencyId)?.toAgencyDetailsDto()
    LegacyAgencyType.PRISON -> prisonRepository.findByIdOrNull(agencyId)?.toAgencyDetailsDto()
    else -> otherAgencyRepository.findWithDetailsByAgencyId(agencyId)?.toAgencyDetailsDto()
  } ?: throw ItemNotFoundException("Agency $agencyId not found")

  fun getAgenciesByType(type: LegacyAgencyType, activeOnly: Boolean): List<AgencyDetailsDto> {
    val agencies = when (type) {
      LegacyAgencyType.COURT -> courts(activeOnly).map { it.toAgencyDetailsDto() }

      LegacyAgencyType.HOSPITAL -> hospitals(highSecurity = false, activeOnly).map { it.toAgencyDetailsDto() }

      LegacyAgencyType.SECURE_HOSPITAL -> hospitals(highSecurity = true, activeOnly).map { it.toAgencyDetailsDto() }

      LegacyAgencyType.POLICE_CUSTODY_SUITE -> policeCustodySuites(activeOnly).map { it.toAgencyDetailsDto() }

      LegacyAgencyType.PROBATION_OFFICE -> probationOffices(activeOnly).map { it.toAgencyDetailsDto() }

      LegacyAgencyType.APPROVED_PREMISES ->
        (if (activeOnly) approvedPremisesRepository.findAllByActiveTrue() else approvedPremisesRepository.findAll()).map { it.toAgencyDetailsDto() }

      LegacyAgencyType.PRISON -> prisons(activeOnly).map { it.toAgencyDetailsDto() }

      else -> {
        val otherAgencyType = OtherAgencyType.valueOf(type.name)
        (
          if (activeOnly) {
            otherAgencyRepository.findAllByOtherAgencyTypeAndActiveTrue(otherAgencyType)
          } else {
            otherAgencyRepository.findAllByOtherAgencyType(otherAgencyType)
          }
          ).map { it.toAgencyDetailsDto() }
      }
    }

    return agencies.sortedBy { it.agencyId }
  }

  // Filter on active (and type) in the database rather than loading every row and filtering in memory
  private fun courts(activeOnly: Boolean) = if (activeOnly) courtRepository.findAllByActiveTrue() else courtRepository.findAll()

  private fun hospitals(highSecurity: Boolean, activeOnly: Boolean) = if (activeOnly) {
    hospitalRepository.findAllByHighSecurityAndActiveTrue(highSecurity)
  } else {
    hospitalRepository.findAllByHighSecurity(highSecurity)
  }

  private fun policeCustodySuites(activeOnly: Boolean) = if (activeOnly) policeCustodySuiteRepository.findAllByActiveTrue() else policeCustodySuiteRepository.findAll()

  private fun probationOffices(activeOnly: Boolean) = if (activeOnly) probationOfficeRepository.findAllByActiveTrue() else probationOfficeRepository.findAll()

  private fun prisons(activeOnly: Boolean) = if (activeOnly) prisonRepository.findAllByActiveTrue() else prisonRepository.findAll()

  fun getAgenciesByLegacyType(legacyType: String, activeOnly: Boolean): List<AgencyDetailsDto> = getAgenciesByType(legacyType.toLegacyAgencyType(), activeOnly)
}

private fun String.toLegacyAgencyType(): LegacyAgencyType = when (this) {
  "INST" -> LegacyAgencyType.PRISON
  "CRT" -> LegacyAgencyType.COURT
  "HOSPITAL" -> LegacyAgencyType.HOSPITAL
  "HSHOSP" -> LegacyAgencyType.SECURE_HOSPITAL
  "COMM" -> LegacyAgencyType.PROBATION_OFFICE
  "CRC" -> LegacyAgencyType.PROBATION_CRC
  "POLICE", "POLSTN" -> LegacyAgencyType.POLICE_CUSTODY_SUITE
  "APPR" -> LegacyAgencyType.APPROVED_PREMISES
  "AIRPORT" -> LegacyAgencyType.AIRPORT
  "HOST" -> LegacyAgencyType.VOLUNTARY_HOSTEL
  "IMDC" -> LegacyAgencyType.IMMIGRATION_DETENTION_CENTRE
  "OUT" -> LegacyAgencyType.OUTSIDE
  "PECS" -> LegacyAgencyType.PECS
  "PSY" -> LegacyAgencyType.PSYCHIATRIC_CARE
  "SCH" -> LegacyAgencyType.CHILDREN_SECURE_HOME
  "STC" -> LegacyAgencyType.SECURE_TRAINING_CENTRE
  "YOT" -> LegacyAgencyType.YOT
  "FNP" -> LegacyAgencyType.FOREIGN_NATIONAL_PRISON
  else -> throw ValidationException("Unknown nomis agency type $this")
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
