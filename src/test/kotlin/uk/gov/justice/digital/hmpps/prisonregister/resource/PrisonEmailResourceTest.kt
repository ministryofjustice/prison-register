package uk.gov.justice.digital.hmpps.prisonregister.resource

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.prisonregister.model.ContactPurposeType.OFFENDER_MANAGEMENT_UNIT
import uk.gov.justice.digital.hmpps.prisonregister.model.SetOutcome
import uk.gov.justice.digital.hmpps.prisonregister.service.PrisonService

/**
 * Test logic in the PrisonResource class.  Doesn't need any Spring support.
 */
class PrisonEmailResourceTest {
  private val prisonService: PrisonService = mock()
  private val prisonEmailResource = PrisonEmailResource(prisonService)

  @Test
  fun `put email - create`() {
    whenever(prisonService.setEmailAddress("MDI", "a@b.com", OFFENDER_MANAGEMENT_UNIT)).thenReturn(SetOutcome.CREATED)
    val response = prisonEmailResource.putEmailAddressForOffenderManagementUnit("MDI", "a@b.com")
    assertThat(response.statusCode).isEqualTo(HttpStatus.CREATED)
    verify(prisonService).setEmailAddress("MDI", "a@b.com", OFFENDER_MANAGEMENT_UNIT)
  }

  @Test
  fun `put email - update`() {
    whenever(prisonService.setEmailAddress("MDI", "a@b.com", OFFENDER_MANAGEMENT_UNIT)).thenReturn(SetOutcome.UPDATED)
    val response = prisonEmailResource.putEmailAddressForOffenderManagementUnit("MDI", "a@b.com")
    assertThat(response.statusCode).isEqualTo(HttpStatus.NO_CONTENT)
    verify(prisonService).setEmailAddress("MDI", "a@b.com", OFFENDER_MANAGEMENT_UNIT)
  }
}
