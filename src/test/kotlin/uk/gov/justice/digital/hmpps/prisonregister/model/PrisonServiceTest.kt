@file:Suppress("ClassName")

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
import java.util.Optional
import javax.persistence.EntityNotFoundException

class PrisonServiceTest {
  private val prisonRepository: PrisonRepository = mock()
  private val offenderManagementUnitRepository: OffenderManagementUnitRepository = mock()
  private val videoLinkConferencingCentreRepository: VideoLinkConferencingCentreRepository = mock()

  private val prisonService =
    PrisonService(prisonRepository, videoLinkConferencingCentreRepository, offenderManagementUnitRepository)

  @Nested
  inner class findById {
    @Test
    fun `find prison`() {
      val prison = Prison("MDI", "A Prison", true)
      whenever(prisonRepository.findById(anyString())).thenReturn(
        Optional.of(prison)
      )
      val actual = prisonService.findById("MDI")
      assertThat(actual).isEqualTo(prison)
      verify(prisonRepository).findById("MDI")
    }

    @Test
    fun `find prison not found`() {
      assertThatThrownBy { prisonService.findById("MDI") }
        .isInstanceOf(EntityNotFoundException::class.java).hasMessage("Prison MDI not found")
    }
  }

  @Nested
  inner class findByGpPractice {
    @Test
    fun `find prison from gp practice`() {
      val prison = Prison("MDI", "Name", true)
      prison.gpPractice = PrisonGpPractice("MDI", "A12345")
      whenever(prisonRepository.findByGpPracticeGpPracticeCode(anyString())).thenReturn(prison)
      val prisonDto = prisonService.findByGpPractice("MDI")
      assertThat(prisonDto).isEqualTo(prison)
    }

    @Test
    fun `find prison from gp practice not found`() {
      assertThatThrownBy { prisonService.findByGpPractice("A12345") }
        .isInstanceOf(EntityNotFoundException::class.java).hasMessage("Prison with gp practice A12345 not found")
    }
  }

  @Nested
  inner class VCC {
    private val prisonId = "MDI"

    @Test
    fun `get Email Address found`() {
      whenever(videoLinkConferencingCentreRepository.findById(anyString()))
        .thenReturn(
          Optional.of(
            VideolinkConferencingCentre(
              prison = Prison(prisonId, "Test", true),
              emailAddress = "a@b.com"
            )
          )
        )

      assertThat(prisonService.getVccEmailAddress(prisonId)).contains("a@b.com")
      verify(videoLinkConferencingCentreRepository).findById(prisonId)
    }

    @Test
    fun `get Email Address not found`() {
      whenever(videoLinkConferencingCentreRepository.findById(anyString()))
        .thenReturn(Optional.empty())

      assertThat(prisonService.getVccEmailAddress(prisonId)).isNull()
      verify(videoLinkConferencingCentreRepository).findById(prisonId)
    }

    @Test
    fun `set Email Address created`() {
      val prison = Prison(prisonId, "Test", true)
      whenever(videoLinkConferencingCentreRepository.findById(eq(prisonId))).thenReturn(Optional.empty())
      whenever(prisonRepository.findById(eq(prisonId))).thenReturn(Optional.of(prison))

      val outcome = prisonService.setVccEmailAddress(prisonId, "a@b.com")

      assertThat(outcome).isEqualTo(SetOutcome.CREATED)
      verify(videoLinkConferencingCentreRepository).findById(prisonId)
      verify(videoLinkConferencingCentreRepository).save(eq(VideolinkConferencingCentre(prison, "a@b.com")))
      verify(prisonRepository).findById(prisonId)
    }

    @Test
    fun `set Email Address updated`() {
      val prison = Prison(prisonId, "Test", true)
      val persistentVcc = VideolinkConferencingCentre(prison, "p@q.com")

      whenever(videoLinkConferencingCentreRepository.findById(anyString()))
        .thenReturn(Optional.of(persistentVcc))
      whenever(prisonRepository.findById(anyString())).thenReturn(Optional.of(prison))

      val outcome = prisonService.setVccEmailAddress(prisonId, "a@b.com")

      assertThat(outcome).isEqualTo(SetOutcome.UPDATED)
      assertThat(persistentVcc.emailAddress).isEqualTo("a@b.com")
    }

    @Test
    fun `set Email Address no prison`() {
      whenever(prisonRepository.findById(anyString())).thenReturn(Optional.empty())

      assertThatThrownBy { prisonService.setVccEmailAddress(prisonId, "a@b.com") }
        .isInstanceOf(EntityNotFoundException::class.java)
    }

    @Test
    fun `delete Email Address, address exists`() {
      whenever(videoLinkConferencingCentreRepository.existsById(anyString())).thenReturn(true)
      prisonService.deleteVccEmailAddress(prisonId)
      verify(videoLinkConferencingCentreRepository).deleteById(prisonId)
    }

    @Test
    fun `delete Email Address, address does not exist`() {
      whenever(videoLinkConferencingCentreRepository.existsById(anyString())).thenReturn(false)
      prisonService.deleteVccEmailAddress(prisonId)
      verify(videoLinkConferencingCentreRepository).existsById(prisonId)
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
