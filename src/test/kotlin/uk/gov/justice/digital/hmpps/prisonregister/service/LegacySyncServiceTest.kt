package uk.gov.justice.digital.hmpps.prisonregister.service

import jakarta.persistence.EntityNotFoundException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.mockito.kotlin.inOrder
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.prisonregister.model.AllAgency
import uk.gov.justice.digital.hmpps.prisonregister.model.AllAgencyRepository
import uk.gov.justice.digital.hmpps.prisonregister.resource.LegacyAgencyDto
import uk.gov.justice.digital.hmpps.prisonregister.resource.LegacyAgencyType

class LegacySyncServiceTest {
  private val courtService: CourtService = mock()
  private val hospitalService: HospitalService = mock()
  private val probationOfficeService: ProbationOfficeService = mock()
  private val approvedPremisesService: ApprovedPremisesService = mock()
  private val policeCustodySuiteService: PoliceCustodySuiteService = mock()
  private val otherAgencyService: OtherAgencyService = mock()
  private val prisonService: PrisonService = mock()
  private val allAgencyRepository: AllAgencyRepository = mock()
  private val services = arrayOf(courtService, hospitalService, probationOfficeService, approvedPremisesService, policeCustodySuiteService, otherAgencyService, prisonService)

  private val service = LegacySyncService(courtService, hospitalService, probationOfficeService, approvedPremisesService, policeCustodySuiteService, otherAgencyService, prisonService, allAgencyRepository)

  @ParameterizedTest
  @EnumSource(LegacyAgencyType::class)
  fun `loads details only from the service identified by the view`(agencyType: LegacyAgencyType) {
    val details: LegacyAgencyDto = mock()
    whenever(allAgencyRepository.findFirstByAgencyId("AGENCY")).thenReturn(AllAgency("AGENCY", "Agency", agencyType))
    when (agencyType) {
      LegacyAgencyType.COURT -> whenever(courtService.tryFindById("AGENCY")).thenReturn(details)
      LegacyAgencyType.HOSPITAL, LegacyAgencyType.SECURE_HOSPITAL -> whenever(hospitalService.tryFindById("AGENCY")).thenReturn(details)
      LegacyAgencyType.PROBATION_OFFICE -> whenever(probationOfficeService.tryFindById("AGENCY")).thenReturn(details)
      LegacyAgencyType.APPROVED_PREMISES -> whenever(approvedPremisesService.tryFindById("AGENCY")).thenReturn(details)
      LegacyAgencyType.POLICE_CUSTODY_SUITE -> whenever(policeCustodySuiteService.tryFindById("AGENCY")).thenReturn(details)
      LegacyAgencyType.PRISON -> whenever(prisonService.tryFindById("AGENCY")).thenReturn(details)
      else -> whenever(otherAgencyService.tryFindById("AGENCY")).thenReturn(details)
    }

    assertThat(service.getDetails("AGENCY")).isSameAs(details)

    verifyLookup(agencyType)
  }

  @Test
  fun `throws not found without calling services when agency is absent from the view`() {
    assertThatThrownBy { service.getDetails("AGENCY") }
      .isInstanceOf(EntityNotFoundException::class.java)
      .hasMessage("Agency AGENCY not found")

    verify(allAgencyRepository).findFirstByAgencyId("AGENCY")
    verifyNoMoreInteractions(allAgencyRepository)
    verifyNoInteractions(*services)
  }

  @ParameterizedTest
  @EnumSource(LegacyAgencyType::class)
  fun `throws not found without falling back when the selected service cannot find the agency`(agencyType: LegacyAgencyType) {
    whenever(allAgencyRepository.findFirstByAgencyId("AGENCY")).thenReturn(AllAgency("AGENCY", "Agency", agencyType))

    assertThatThrownBy { service.getDetails("AGENCY") }
      .isInstanceOf(EntityNotFoundException::class.java)
      .hasMessage("Agency AGENCY not found")

    verifyLookup(agencyType)
  }

  private fun verifyLookup(agencyType: LegacyAgencyType) {
    inOrder(allAgencyRepository, *services) {
      verify(allAgencyRepository).findFirstByAgencyId("AGENCY")
      when (agencyType) {
        LegacyAgencyType.COURT -> verify(courtService).tryFindById("AGENCY")
        LegacyAgencyType.HOSPITAL, LegacyAgencyType.SECURE_HOSPITAL -> verify(hospitalService).tryFindById("AGENCY")
        LegacyAgencyType.PROBATION_OFFICE -> verify(probationOfficeService).tryFindById("AGENCY")
        LegacyAgencyType.APPROVED_PREMISES -> verify(approvedPremisesService).tryFindById("AGENCY")
        LegacyAgencyType.POLICE_CUSTODY_SUITE -> verify(policeCustodySuiteService).tryFindById("AGENCY")
        LegacyAgencyType.PRISON -> verify(prisonService).tryFindById("AGENCY")
        else -> verify(otherAgencyService).tryFindById("AGENCY")
      }
    }
    verifyNoMoreInteractions(allAgencyRepository, *services)
  }
}
