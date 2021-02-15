package uk.gov.justice.digital.hmpps.prisonregister.model

import com.nhaarman.mockito_kotlin.eq
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.verifyNoMoreInteractions
import com.nhaarman.mockito_kotlin.whenever
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
    val prisonId = "MDI"

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

      assertThat(prisonService.getVccEmailAddress(prisonId)).isEmpty
      verify(videoLinkConferencingCentreRepository).findById(prisonId)
    }

    @Test
    fun `set Email Address created`() {
      val prison = Prison(prisonId, "Test", true)
      whenever(videoLinkConferencingCentreRepository.findById(eq(prisonId))).thenReturn(Optional.empty())
      whenever(prisonRepository.findById(eq(prisonId))).thenReturn(Optional.of(prison))

      val outcome = prisonService.setVccEmailAddress(prisonId, "a@b.com")

      assertThat(outcome).isEqualTo(SetOutcome.CREATED)
      verify(videoLinkConferencingCentreRepository).save(eq(VideolinkConferencingCentre(prison, "a@b.com")))
    }

    @Test
    fun `set Email Address updated`() {
      val prison = Prison(prisonId, "Test", true)
      val persistentVcc = VideolinkConferencingCentre(prison, "p@q.com")

      whenever(videoLinkConferencingCentreRepository.findById(eq(prisonId)))
        .thenReturn(Optional.of(persistentVcc))
      whenever(prisonRepository.findById(eq(prisonId))).thenReturn(Optional.of(prison))

      val outcome = prisonService.setVccEmailAddress(prisonId, "a@b.com")

      assertThat(outcome).isEqualTo(SetOutcome.UPDATED)
      assertThat(persistentVcc.emailAddress).isEqualTo("a@b.com")
    }

    @Test
    fun `set Email Address no prison`() {
      whenever(prisonRepository.findById(eq(prisonId))).thenReturn(Optional.empty())

      assertThatThrownBy {
        prisonService.setVccEmailAddress(prisonId, "a@b.com")
      }
        .isInstanceOf(EntityNotFoundException::class.java)
    }

    @Test
    fun `delete Email Address, address exists`() {
      val prison = Prison(prisonId, "Test", true)
      val persistentVcc = VideolinkConferencingCentre(prison, "p@q.com")
      whenever(videoLinkConferencingCentreRepository.findById(prisonId))
        .thenReturn(Optional.of(persistentVcc))

      prisonService.deleteVccEmailAddress(prisonId)

      verify(videoLinkConferencingCentreRepository).deleteById(prisonId)
    }

    @Test
    fun `delete Email Address, address does not exist`() {
      whenever(videoLinkConferencingCentreRepository.findById(prisonId)).thenReturn(Optional.empty())

      prisonService.deleteVccEmailAddress(prisonId)

      verify(videoLinkConferencingCentreRepository).findById(prisonId)
      verifyNoMoreInteractions(videoLinkConferencingCentreRepository)
    }
  }

  @Nested
  inner class OMU {
    val prisonId = "MDI"

    @Test
    fun `get Email Address found`() {
      whenever(offenderManagementUnitRepository.findById(anyString()))
        .thenReturn(
          Optional.of(
            OffenderManagementUnit(
              prison = Prison(prisonId, "Test", true),
              emailAddress = "a@b.com"
            )
          )
        )

      assertThat(prisonService.getOmuEmailAddress(prisonId)).contains("a@b.com")
      verify(offenderManagementUnitRepository).findById(prisonId)
    }

    @Test
    fun `get Email Address not found`() {
      whenever(offenderManagementUnitRepository.findById(anyString()))
        .thenReturn(Optional.empty())

      assertThat(prisonService.getOmuEmailAddress(prisonId)).isEmpty
      verify(offenderManagementUnitRepository).findById(prisonId)
    }

    @Test
    fun `set Email Address created`() {
      val prison = Prison(prisonId, "Test", true)
      whenever(offenderManagementUnitRepository.findById(eq(prisonId))).thenReturn(Optional.empty())
      whenever(prisonRepository.findById(eq(prisonId))).thenReturn(Optional.of(prison))

      val outcome = prisonService.setOmuEmailAddress(prisonId, "a@b.com")

      assertThat(outcome).isEqualTo(SetOutcome.CREATED)
      verify(offenderManagementUnitRepository).save(eq(OffenderManagementUnit(prison, "a@b.com")))
    }

    @Test
    fun `set Email Address updated`() {
      val prison = Prison(prisonId, "Test", true)
      val persistentOmu = OffenderManagementUnit(prison, "p@q.com")

      whenever(offenderManagementUnitRepository.findById(eq(prisonId)))
        .thenReturn(Optional.of(persistentOmu))
      whenever(prisonRepository.findById(eq(prisonId))).thenReturn(Optional.of(prison))

      val outcome = prisonService.setOmuEmailAddress(prisonId, "a@b.com")

      assertThat(outcome).isEqualTo(SetOutcome.UPDATED)
      assertThat(persistentOmu.emailAddress).isEqualTo("a@b.com")
    }

    @Test
    fun `set Email Address no prison`() {
      whenever(prisonRepository.findById(eq(prisonId))).thenReturn(Optional.empty())

      assertThatThrownBy {
        prisonService.setOmuEmailAddress(prisonId, "a@b.com")
      }
        .isInstanceOf(EntityNotFoundException::class.java)
    }

    @Test
    fun `delete Email Address, address exists`() {
      val prison = Prison(prisonId, "Test", true)
      val persistentOmu = OffenderManagementUnit(prison, "p@q.com")
      whenever(offenderManagementUnitRepository.findById(prisonId))
        .thenReturn(Optional.of(persistentOmu))

      prisonService.deleteOmuEmailAddress(prisonId)

      verify(offenderManagementUnitRepository).deleteById(prisonId)
    }

    @Test
    fun `delete Email Address, address does not exist`() {
      whenever(offenderManagementUnitRepository.findById(prisonId)).thenReturn(Optional.empty())

      prisonService.deleteOmuEmailAddress(prisonId)

      verify(offenderManagementUnitRepository).findById(prisonId)
      verifyNoMoreInteractions(offenderManagementUnitRepository)
    }
  }
}
