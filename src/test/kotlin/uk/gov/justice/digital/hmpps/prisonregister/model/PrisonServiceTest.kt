package uk.gov.justice.digital.hmpps.prisonregister.model

import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyNoMoreInteractions
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyString
import uk.gov.justice.digital.hmpps.prisonregister.resource.PrisonDto
import java.util.Optional
import javax.persistence.EntityNotFoundException

class PrisonServiceTest {
  private val prisonRepository: PrisonRepository = mock()
  private val offenderManagementUnitRepository: OffenderManagementUnitRepository = mock()
  private val videoLinkConferencingCentreRepository: VideoLinkConferencingCentreRepository = mock()

  private val prisonService =
    PrisonService(prisonRepository, videoLinkConferencingCentreRepository, offenderManagementUnitRepository)

  @Suppress("ClassName")
  @Nested
  inner class findById {
    @Test
    fun `find prison`() {
      whenever(prisonRepository.findById(anyString())).thenReturn(
        Optional.of(Prison("MDI", "A Prison", true))
      )
      val prisonDto = prisonService.findById("MDI")
      assertThat(prisonDto).isEqualTo(PrisonDto("MDI", "A Prison", true))
      verify(prisonRepository).findById("MDI")
    }
  }

  @Nested
  inner class VCC {
    private val PRISON_ID = "MDI"

    @Test
    fun `get Email Address found`() {
      whenever(videoLinkConferencingCentreRepository.findById(anyString()))
        .thenReturn(
          Optional.of(
            VideolinkConferencingCentre(
              prison = Prison(PRISON_ID, "Test", true),
              emailAddress = "a@b.com"
            )
          )
        )

      assertThat(prisonService.getVccEmailAddress(PRISON_ID)).contains("a@b.com")
      verify(videoLinkConferencingCentreRepository).findById(PRISON_ID)
    }

    @Test
    fun `get Email Address not found`() {
      whenever(videoLinkConferencingCentreRepository.findById(anyString()))
        .thenReturn(Optional.empty())

      assertThat(prisonService.getVccEmailAddress(PRISON_ID)).isNull()
      verify(videoLinkConferencingCentreRepository).findById(PRISON_ID)
    }

    @Test
    fun `set Email Address created`() {
      val prison = Prison(PRISON_ID, "Test", true)
      whenever(videoLinkConferencingCentreRepository.findById(eq(PRISON_ID))).thenReturn(Optional.empty())
      whenever(prisonRepository.findById(eq(PRISON_ID))).thenReturn(Optional.of(prison))

      val outcome = prisonService.setVccEmailAddress(PRISON_ID, "a@b.com")

      assertThat(outcome).isEqualTo(SetOutcome.CREATED)
      verify(videoLinkConferencingCentreRepository).findById(PRISON_ID)
      verify(videoLinkConferencingCentreRepository).save(eq(VideolinkConferencingCentre(prison, "a@b.com")))
      verify(prisonRepository).findById(PRISON_ID)
    }

    @Test
    fun `set Email Address updated`() {
      val prison = Prison(PRISON_ID, "Test", true)
      val persistentVcc = VideolinkConferencingCentre(prison, "p@q.com")

      whenever(videoLinkConferencingCentreRepository.findById(anyString()))
        .thenReturn(Optional.of(persistentVcc))
      whenever(prisonRepository.findById(anyString())).thenReturn(Optional.of(prison))

      val outcome = prisonService.setVccEmailAddress(PRISON_ID, "a@b.com")

      assertThat(outcome).isEqualTo(SetOutcome.UPDATED)
      assertThat(persistentVcc.emailAddress).isEqualTo("a@b.com")
    }

    @Test
    fun `set Email Address no prison`() {
      whenever(prisonRepository.findById(anyString())).thenReturn(Optional.empty())

      assertThatThrownBy { prisonService.setVccEmailAddress(PRISON_ID, "a@b.com") }
        .isInstanceOf(EntityNotFoundException::class.java)
    }

    @Test
    fun `delete Email Address, address exists`() {
      whenever(videoLinkConferencingCentreRepository.existsById(anyString())).thenReturn(true)
      prisonService.deleteVccEmailAddress(PRISON_ID)
      verify(videoLinkConferencingCentreRepository).deleteById(PRISON_ID)
    }

    @Test
    fun `delete Email Address, address does not exist`() {
      whenever(videoLinkConferencingCentreRepository.existsById(anyString())).thenReturn(false)
      prisonService.deleteVccEmailAddress(PRISON_ID)
      verify(videoLinkConferencingCentreRepository).existsById(PRISON_ID)
      verifyNoMoreInteractions(videoLinkConferencingCentreRepository)
    }
  }

  @Nested
  inner class OMU {
    private val PRISON_ID = "MDI"

    @Test
    fun `get Email Address found`() {
      whenever(offenderManagementUnitRepository.findById(anyString()))
        .thenReturn(
          Optional.of(
            OffenderManagementUnit(
              prison = Prison(PRISON_ID, "Test", true),
              emailAddress = "a@b.com"
            )
          )
        )

      assertThat(prisonService.getOmuEmailAddress(PRISON_ID)).contains("a@b.com")
      verify(offenderManagementUnitRepository).findById(PRISON_ID)
    }

    @Test
    fun `get Email Address not found`() {
      whenever(offenderManagementUnitRepository.findById(anyString()))
        .thenReturn(Optional.empty())

      assertThat(prisonService.getOmuEmailAddress(PRISON_ID)).isNull()
      verify(offenderManagementUnitRepository).findById(PRISON_ID)
    }

    @Test
    fun `set Email Address created`() {
      val prison = Prison(PRISON_ID, "Test", true)
      whenever(offenderManagementUnitRepository.findById(anyString())).thenReturn(Optional.empty())
      whenever(prisonRepository.findById(anyString())).thenReturn(Optional.of(prison))

      val outcome = prisonService.setOmuEmailAddress(PRISON_ID, "a@b.com")

      assertThat(outcome).isEqualTo(SetOutcome.CREATED)
      verify(offenderManagementUnitRepository).save(eq(OffenderManagementUnit(prison, "a@b.com")))
    }

    @Test
    fun `set Email Address updated`() {
      val prison = Prison(PRISON_ID, "Test", true)
      val persistentOmu = OffenderManagementUnit(prison, "p@q.com")

      whenever(offenderManagementUnitRepository.findById(anyString())).thenReturn(Optional.of(persistentOmu))
      whenever(prisonRepository.findById(anyString())).thenReturn(Optional.of(prison))

      val outcome = prisonService.setOmuEmailAddress(PRISON_ID, "a@b.com")

      assertThat(outcome).isEqualTo(SetOutcome.UPDATED)
      assertThat(persistentOmu.emailAddress).isEqualTo("a@b.com")
    }

    @Test
    fun `set Email Address no prison`() {
      whenever(prisonRepository.findById(anyString())).thenReturn(Optional.empty())
      assertThatThrownBy { prisonService.setOmuEmailAddress(PRISON_ID, "a@b.com") }
        .isInstanceOf(EntityNotFoundException::class.java)
    }

    @Test
    fun `delete Email Address, address exists`() {
      whenever(offenderManagementUnitRepository.existsById(anyString())).thenReturn(true)
      prisonService.deleteOmuEmailAddress(PRISON_ID)
      verify(offenderManagementUnitRepository).deleteById(PRISON_ID)
    }

    @Test
    fun `delete Email Address, address does not exist`() {
      whenever(offenderManagementUnitRepository.existsById(anyString())).thenReturn(false)
      prisonService.deleteOmuEmailAddress(PRISON_ID)
      verify(offenderManagementUnitRepository).existsById(PRISON_ID)
      verifyNoMoreInteractions(offenderManagementUnitRepository)
    }
  }
}
