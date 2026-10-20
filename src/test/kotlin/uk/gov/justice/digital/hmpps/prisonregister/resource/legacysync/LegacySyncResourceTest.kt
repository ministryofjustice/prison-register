package uk.gov.justice.digital.hmpps.prisonregister.resource.legacysync

import com.microsoft.applicationinsights.TelemetryClient
import jakarta.persistence.EntityNotFoundException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.prisonregister.resource.LegacyAgencyDto
import uk.gov.justice.digital.hmpps.prisonregister.resource.LegacyAgencyResponse
import uk.gov.justice.digital.hmpps.prisonregister.resource.LegacyAgencyType
import uk.gov.justice.digital.hmpps.prisonregister.resource.LegacySyncResource
import uk.gov.justice.digital.hmpps.prisonregister.service.LegacySyncService
import uk.gov.justice.digital.hmpps.prisonregister.service.SnsService
import java.time.Instant

class LegacySyncResourceTest {
  private val telemetry: TelemetryClient = mock()
  private val legacySyncService: LegacySyncService = mock()
  private val snsService: SnsService = mock()
  private val resource = LegacySyncResource(telemetry, legacySyncService, snsService)
  private val agencyId = "AGENCY"

  @ParameterizedTest
  @EnumSource(LegacyAgencyType::class, names = ["PRISON"], mode = EnumSource.Mode.EXCLUDE)
  fun `publishes inserted event for the agency type`(agencyType: LegacyAgencyType) {
    val request = agencyDto(agencyType)
    val response = LegacyAgencyResponse(updated = false)
    whenever(legacySyncService.createOrUpdateAgency(agencyId, request)).thenReturn(response)
    val before = Instant.now()

    assertThat(resource.createOrUpdateAgency(agencyId, request)).isSameAs(response)

    val occurredAt = argumentCaptor<Instant>()
    when (agencyType) {
      LegacyAgencyType.COURT -> verify(snsService).sendCourtRegisterInsertedEvent(eq(agencyId), occurredAt.capture(), eq("NOMIS"))
      LegacyAgencyType.HOSPITAL, LegacyAgencyType.SECURE_HOSPITAL -> verify(snsService).sendHospitalRegisterInsertedEvent(eq(agencyId), occurredAt.capture(), eq("NOMIS"))
      LegacyAgencyType.PROBATION_OFFICE -> verify(snsService).sendProbationOfficeRegisterInsertedEvent(eq(agencyId), occurredAt.capture(), eq("NOMIS"))
      LegacyAgencyType.APPROVED_PREMISES -> verify(snsService).sendApprovedPremisesRegisterInsertedEvent(eq(agencyId), occurredAt.capture(), eq("NOMIS"))
      LegacyAgencyType.POLICE_CUSTODY_SUITE -> verify(snsService).sendPoliceCustodySuiteRegisterInsertedEvent(eq(agencyId), occurredAt.capture(), eq("NOMIS"))
      else -> verify(snsService).sendAgencyRegisterInsertedEvent(eq(agencyId), occurredAt.capture(), eq("NOMIS"))
    }
    assertThat(occurredAt.firstValue).isBetween(before, Instant.now())
    verifyNoMoreInteractions(snsService)
    verify(telemetry).trackEvent("legacy-sync-agency-created", mapOf("agencyId" to agencyId), null)
  }

  @ParameterizedTest
  @EnumSource(LegacyAgencyType::class, names = ["PRISON"], mode = EnumSource.Mode.EXCLUDE)
  fun `publishes amended event for the agency type`(agencyType: LegacyAgencyType) {
    val request = agencyDto(agencyType)
    val response = LegacyAgencyResponse(updated = true)
    whenever(legacySyncService.createOrUpdateAgency(agencyId, request)).thenReturn(response)
    val before = Instant.now()

    assertThat(resource.createOrUpdateAgency(agencyId, request)).isSameAs(response)

    val occurredAt = argumentCaptor<Instant>()
    when (agencyType) {
      LegacyAgencyType.COURT -> verify(snsService).sendCourtRegisterAmendedEvent(eq(agencyId), occurredAt.capture(), eq("NOMIS"))
      LegacyAgencyType.HOSPITAL, LegacyAgencyType.SECURE_HOSPITAL -> verify(snsService).sendHospitalRegisterAmendedEvent(eq(agencyId), occurredAt.capture(), eq("NOMIS"))
      LegacyAgencyType.PROBATION_OFFICE -> verify(snsService).sendProbationOfficeRegisterAmendedEvent(eq(agencyId), occurredAt.capture(), eq("NOMIS"))
      LegacyAgencyType.APPROVED_PREMISES -> verify(snsService).sendApprovedPremisesRegisterAmendedEvent(eq(agencyId), occurredAt.capture(), eq("NOMIS"))
      LegacyAgencyType.POLICE_CUSTODY_SUITE -> verify(snsService).sendPoliceCustodySuiteRegisterAmendedEvent(eq(agencyId), occurredAt.capture(), eq("NOMIS"))
      else -> verify(snsService).sendAgencyRegisterAmendedEvent(eq(agencyId), occurredAt.capture(), eq("NOMIS"))
    }
    assertThat(occurredAt.firstValue).isBetween(before, Instant.now())
    verifyNoMoreInteractions(snsService)
    verify(telemetry).trackEvent("legacy-sync-agency-updated", mapOf("agencyId" to agencyId), null)
  }

  @ParameterizedTest
  @ValueSource(booleans = [false, true])
  fun `migration does not publish domain events`(updated: Boolean) {
    val request = agencyDto(LegacyAgencyType.COURT)
    val response = LegacyAgencyResponse(updated)
    whenever(legacySyncService.createOrUpdateAgency(agencyId, request)).thenReturn(response)

    assertThat(resource.migrateAgency(agencyId, request)).isSameAs(response)

    verifyNoInteractions(snsService)
    val action = if (updated) "updated" else "created"
    verify(telemetry).trackEvent("legacy-migration-agency-$action", mapOf("agencyId" to agencyId), null)
  }

  @Test
  fun `does not publish an event when synchronisation fails`() {
    val request = agencyDto(LegacyAgencyType.COURT)
    whenever(legacySyncService.createOrUpdateAgency(agencyId, request)).thenThrow(EntityNotFoundException("Invalid agency"))

    assertThatThrownBy { resource.createOrUpdateAgency(agencyId, request) }
      .isInstanceOf(EntityNotFoundException::class.java)

    verifyNoInteractions(snsService, telemetry)
  }

  private fun agencyDto(agencyType: LegacyAgencyType) = LegacyAgencyDto(
    agencyType = agencyType,
    name = "Agency",
    description = null,
    active = true,
    inactiveDate = null,
    cjitCode = null,
    areaCode = null,
    regionCode = null,
    geographicalAreaCode = null,
    payrollRegionCode = null,
    localAuthorityCode = null,
    courtTypeCode = null,
    accessibleAccess = null,
    contact = null,
    addresses = emptyList(),
    emailAddresses = emptyList(),
    phoneNumbers = emptyList(),
  )
}
