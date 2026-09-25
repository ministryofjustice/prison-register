package uk.gov.justice.digital.hmpps.prisonregister.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.prisonregister.model.AgencyRepository
import uk.gov.justice.digital.hmpps.prisonregister.model.CourtRepository
import uk.gov.justice.digital.hmpps.prisonregister.model.HospitalRepository
import uk.gov.justice.digital.hmpps.prisonregister.model.PoliceCustodySuiteRepository
import uk.gov.justice.digital.hmpps.prisonregister.model.PrisonRepository
import uk.gov.justice.digital.hmpps.prisonregister.model.ProbationOfficeRepository
import uk.gov.justice.digital.hmpps.prisonregister.resource.AgencySummaryDto
import uk.gov.justice.digital.hmpps.prisonregister.resource.LegacyAgencyType

@Service
class PublicApiService(
  private val courtRepository: CourtRepository,
  private val hospitalRepository: HospitalRepository,
  private val policeCustodySuiteRepository: PoliceCustodySuiteRepository,
  private val probationOfficeRepository: ProbationOfficeRepository,
  private val prisonRepository: PrisonRepository,
  private val agencyRepository: AgencyRepository,
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

    val agencies = agencyRepository.findAll().map {
      AgencySummaryDto(agencyId = it.agencyId, description = it.description, agencyType = LegacyAgencyType.valueOf(it.agencyType.name), active = it.active)
    }

    return (courts + hospitals + policeCustodySuites + probationOffices + prisons + agencies)
      .sortedBy { it.agencyId }
  }
}
