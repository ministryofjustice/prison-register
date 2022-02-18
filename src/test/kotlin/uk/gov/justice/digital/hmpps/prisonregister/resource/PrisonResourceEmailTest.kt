package uk.gov.justice.digital.hmpps.prisonregister.resource

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.prisonregister.model.SetOutcome
import uk.gov.justice.digital.hmpps.prisonregister.services.PrisonService

/**
 * Test logic in the PrisonResource class.  Doesn't need any Spring support.
 */
class PrisonResourceEmailTest {
  private val prisonService: PrisonService = mock()
  private val prisonResource = PrisonResource(prisonService)

  @Test
  fun `VCC email address found`() {
    whenever(prisonService.getVccEmailAddress(anyString())).thenReturn("p@q.com")
    val response = prisonResource.getEmailForVideoConferencingCentre("MDI")
    with(response) {
      assertThat(statusCode).isEqualTo(HttpStatus.OK)
      assertThat(body).isEqualTo("p@q.com")
    }
    verify(prisonService).getVccEmailAddress("MDI")
  }

  @Test
  fun `VCC email address not found`() {
    whenever(prisonService.getVccEmailAddress(anyString())).thenReturn(null)
    val response = prisonResource.getEmailForVideoConferencingCentre("MDI")
    assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
    verify(prisonService).getVccEmailAddress("MDI")
  }

  @Test
  fun `OMU email address found`() {
    whenever(prisonService.getOmuEmailAddress(anyString())).thenReturn("p@q.com")
    val response = prisonResource.getEmailForOffenderManagementUnit("MDI")
    with(response) {
      assertThat(statusCode).isEqualTo(HttpStatus.OK)
      assertThat(body).isEqualTo("p@q.com")
    }
    verify(prisonService).getOmuEmailAddress("MDI")
  }

  @Test
  fun `OMU email address not found`() {
    whenever(prisonService.getOmuEmailAddress(anyString())).thenReturn(null)
    val response = prisonResource.getEmailForOffenderManagementUnit("MDI")
    assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
    verify(prisonService).getOmuEmailAddress("MDI")
  }

  @Test
  fun `put OMU email - create`() {
    whenever(prisonService.setOmuEmailAddress(anyString(), anyString())).thenReturn(SetOutcome.CREATED)
    val response = prisonResource.putEmailAddressForOffenderManagementUnit("MDI", "a@b.com")
    assertThat(response.statusCode).isEqualTo(HttpStatus.CREATED)
    verify(prisonService).setOmuEmailAddress("MDI", "a@b.com")
  }

  @Test
  fun `put OMU email - update`() {
    whenever(prisonService.setOmuEmailAddress(anyString(), anyString())).thenReturn(SetOutcome.UPDATED)
    val response = prisonResource.putEmailAddressForOffenderManagementUnit("MDI", "a@b.com")
    assertThat(response.statusCode).isEqualTo(HttpStatus.NO_CONTENT)
    verify(prisonService).setOmuEmailAddress("MDI", "a@b.com")
  }

  @Test
  fun `put VCC email - create`() {
    whenever(prisonService.setVccEmailAddress(anyString(), anyString())).thenReturn(SetOutcome.CREATED)
    val response = prisonResource.putEmailAddressForVideolinkConferencingCentre("MDI", "a@b.com")
    assertThat(response.statusCode).isEqualTo(HttpStatus.CREATED)
    verify(prisonService).setVccEmailAddress("MDI", "a@b.com")
  }

  @Test
  fun `put VCC email - update`() {
    whenever(prisonService.setVccEmailAddress(anyString(), anyString())).thenReturn(SetOutcome.UPDATED)
    val response = prisonResource.putEmailAddressForVideolinkConferencingCentre("MDI", "a@b.com")
    assertThat(response.statusCode).isEqualTo(HttpStatus.NO_CONTENT)
    verify(prisonService).setVccEmailAddress("MDI", "a@b.com")
  }
}
