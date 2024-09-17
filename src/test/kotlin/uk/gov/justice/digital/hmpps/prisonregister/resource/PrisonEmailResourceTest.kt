package uk.gov.justice.digital.hmpps.prisonregister.resource

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.prisonregister.model.DepartmentType.OFFENDER_MANAGEMENT_UNIT
import uk.gov.justice.digital.hmpps.prisonregister.model.DepartmentType.VIDEOLINK_CONFERENCING_CENTRE
import uk.gov.justice.digital.hmpps.prisonregister.model.SetOutcome
import uk.gov.justice.digital.hmpps.prisonregister.service.PrisonService

/**
 * Test logic in the PrisonResource class.  Doesn't need any Spring support.
 */
class PrisonEmailResourceTest {
  private val prisonService: PrisonService = mock()
  private val prisonEmailResource = PrisonEmailResource(prisonService)
  private val prisonId = "MDI"
  private val emailAddress = "a@b.com"

  @Test
  fun `put email - create`() {
    whenever(prisonService.setEmailAddress(prisonId, emailAddress, OFFENDER_MANAGEMENT_UNIT)).thenReturn(SetOutcome.CREATED)
    val response = prisonEmailResource.putEmailAddressForOffenderManagementUnit(prisonId, emailAddress)
    assertThat(response.statusCode).isEqualTo(HttpStatus.CREATED)
    verify(prisonService).setEmailAddress(prisonId, emailAddress, OFFENDER_MANAGEMENT_UNIT)
  }

  @Test
  fun `put email - update`() {
    whenever(prisonService.setEmailAddress(prisonId, emailAddress, OFFENDER_MANAGEMENT_UNIT)).thenReturn(SetOutcome.UPDATED)
    val response = prisonEmailResource.putEmailAddressForOffenderManagementUnit(prisonId, emailAddress)
    assertThat(response.statusCode).isEqualTo(HttpStatus.NO_CONTENT)
    verify(prisonService).setEmailAddress(prisonId, emailAddress, OFFENDER_MANAGEMENT_UNIT)
  }

  @Test
  fun `get email - not found when email does not exist Videolink conferencing centre`() {
    whenever(prisonService.getEmailAddress(prisonId, VIDEOLINK_CONFERENCING_CENTRE))
      .thenReturn(null)
    val response = prisonEmailResource.getEmailForVideoConferencingCentre(prisonId)
    assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
    verify(prisonService).getEmailAddress(prisonId, VIDEOLINK_CONFERENCING_CENTRE)
  }

  @Test
  fun `get email - Videolink conferencing centre`() {
    whenever(prisonService.getEmailAddress(prisonId, VIDEOLINK_CONFERENCING_CENTRE))
      .thenReturn(emailAddress)
    val response = prisonEmailResource.getEmailForVideoConferencingCentre(prisonId)
    assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
    verify(prisonService).getEmailAddress(prisonId, VIDEOLINK_CONFERENCING_CENTRE)
  }

  @Test
  fun `get email - not found when email does not exist Offender management unit`() {
    whenever(prisonService.getEmailAddress(prisonId, OFFENDER_MANAGEMENT_UNIT))
      .thenReturn(null)
    val response = prisonEmailResource.getEmailForOffenderManagementUnit(prisonId)
    assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
    verify(prisonService).getEmailAddress(prisonId, OFFENDER_MANAGEMENT_UNIT)
  }
}
