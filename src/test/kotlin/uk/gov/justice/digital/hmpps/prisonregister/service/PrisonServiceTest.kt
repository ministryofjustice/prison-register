@file:Suppress("ClassName")

package uk.gov.justice.digital.hmpps.prisonregister.service

import com.microsoft.applicationinsights.TelemetryClient
import jakarta.persistence.EntityExistsException
import jakarta.persistence.EntityNotFoundException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.anyString
import org.mockito.internal.matchers.apachecommons.ReflectionEquals
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.isNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.prisonregister.model.Address
import uk.gov.justice.digital.hmpps.prisonregister.model.Category
import uk.gov.justice.digital.hmpps.prisonregister.model.OffenderManagementUnit
import uk.gov.justice.digital.hmpps.prisonregister.model.OffenderManagementUnitRepository
import uk.gov.justice.digital.hmpps.prisonregister.model.Prison
import uk.gov.justice.digital.hmpps.prisonregister.model.PrisonGpPractice
import uk.gov.justice.digital.hmpps.prisonregister.model.PrisonRepository
import uk.gov.justice.digital.hmpps.prisonregister.model.PrisonType
import uk.gov.justice.digital.hmpps.prisonregister.model.SetOutcome
import uk.gov.justice.digital.hmpps.prisonregister.model.Type
import uk.gov.justice.digital.hmpps.prisonregister.model.VideoLinkConferencingCentreRepository
import uk.gov.justice.digital.hmpps.prisonregister.model.VideolinkConferencingCentre
import uk.gov.justice.digital.hmpps.prisonregister.resource.GpDto
import uk.gov.justice.digital.hmpps.prisonregister.resource.InsertPrisonDto
import uk.gov.justice.digital.hmpps.prisonregister.resource.PrisonDto
import uk.gov.justice.digital.hmpps.prisonregister.resource.PrisonTypeDto
import uk.gov.justice.digital.hmpps.prisonregister.resource.UpdateAddressDto
import uk.gov.justice.digital.hmpps.prisonregister.resource.UpdatePrisonDto
import java.util.Optional

class PrisonServiceTest {
  private val prisonRepository: PrisonRepository = mock()
  private val offenderManagementUnitRepository: OffenderManagementUnitRepository = mock()
  private val videoLinkConferencingCentreRepository: VideoLinkConferencingCentreRepository = mock()
  private val telemetryClient: TelemetryClient = mock()

  private val prisonService =
    PrisonService(prisonRepository, videoLinkConferencingCentreRepository, offenderManagementUnitRepository, telemetryClient)

  @Nested
  inner class findById {
    @Test
    fun `find prison`() {
      val prison = Prison(
        prisonId = "MDI",
        name = "A Prison",
        active = true,
        male = true,
        female = false,
        contracted = true,
        categories = mutableSetOf(Category.C),
      )

      val address = Address(
        21,
        "Bawtry Road",
        "Hatfield Woodhouse",
        "Doncaster",
        "South Yorkshire",
        "DN7 6BW",
        "England",
        prison,
      )
      prison.addresses = listOf(address)
      whenever(prisonRepository.findById(anyString())).thenReturn(
        Optional.of(prison),
      )
      val prisonDto = PrisonDto(prison)

      val actual = prisonService.findById("MDI")
      assertThat(actual).isEqualTo(prisonDto)
      assertThat(actual.categories).containsExactly(Category.C)
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
      val prison = Prison("MDI", "Name", active = true)
      prison.gpPractice = PrisonGpPractice("MDI", "A12345")
      val prisonGpPracticeDto = GpDto(prison)
      whenever(prisonRepository.findByGpPracticeGpPracticeCode(anyString())).thenReturn(prison)
      val prisonDto = prisonService.findByGpPractice("MDI")
      assertThat(prisonDto).isEqualTo(prisonGpPracticeDto)
    }

    @Test
    fun `find prison from gp practice not found`() {
      assertThatThrownBy { prisonService.findByGpPractice("A12345") }
        .isInstanceOf(EntityNotFoundException::class.java).hasMessage("Prison with gp practice A12345 not found")
    }
  }

  @Nested
  inner class findByPrisonFilter {
    @Test
    fun `find prison by active and text search`() {
      val prison = Prison("MDI", "Moorland (HMP & YOI)", active = true)
      whenever(prisonRepository.findAll(any())).thenReturn(listOf(prison))
      val results = prisonService.findByPrisonFilter(true, "moorland")

      assertThat(results).containsOnly(PrisonDto(prison))
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
              prison = Prison(prisonId, "Test", active = true),
              emailAddress = "a@b.com",
            ),
          ),
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
      val prison = Prison(prisonId, "Test", active = true)
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
      val prison = Prison(prisonId, "Test", active = true)
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
              prison = Prison(PRISON_ID, "Test", active = true),
              emailAddress = "a@b.com",
            ),
          ),
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
      val prison = Prison(PRISON_ID, "Test", active = true)
      whenever(offenderManagementUnitRepository.findById(anyString())).thenReturn(Optional.empty())
      whenever(prisonRepository.findById(anyString())).thenReturn(Optional.of(prison))

      val outcome = prisonService.setOmuEmailAddress(PRISON_ID, "a@b.com")

      assertThat(outcome).isEqualTo(SetOutcome.CREATED)
      verify(offenderManagementUnitRepository).save(eq(OffenderManagementUnit(prison, "a@b.com")))
    }

    @Test
    fun `set Email Address updated`() {
      val prison = Prison(PRISON_ID, "Test", active = true)
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

  @Nested
  inner class CreatePrison {

    @Test
    fun `try to create a prison that already exists`() {
      whenever(prisonRepository.findById("MDI")).thenReturn(
        Optional.of(Prison("MDI", "A Prison 1", active = true)),
      )
      assertThrows(EntityExistsException::class.java) {
        prisonService.insertPrison(InsertPrisonDto("MDI", "A Prison 1", true, contracted = false))
      }
      verify(prisonRepository).findById("MDI")
      verifyNoInteractions(telemetryClient)
    }

    @Test
    fun `create a prison with minimal data`() {
      val prison = Prison("MDI", "A Prison 1", description = "A Prison for testing", active = true)

      whenever(prisonRepository.findById("MDI")).thenReturn(Optional.empty())
      whenever(prisonRepository.save(prison)).thenReturn(prison)

      val createdPrisonId = prisonService.insertPrison(InsertPrisonDto("MDI", "A Prison 1", contracted = false))
      assertThat(createdPrisonId).isEqualTo("MDI")
      verify(prisonRepository).findById("MDI")
      verify(telemetryClient).trackEvent(eq("prison-register-insert"), any(), isNull())
    }

    @Test
    fun `create a prison`() {
      val prison = givenAPrison()

      whenever(prisonRepository.findById("MDI")).thenReturn(Optional.empty())
      whenever(prisonRepository.save(prison)).thenReturn(prison)

      val createdPrisonId = prisonService.insertPrison(givenAPrisonToInsert())
      assertThat(createdPrisonId).isEqualTo("MDI")
      verify(prisonRepository).findById("MDI")
      verify(telemetryClient).trackEvent(eq("prison-register-insert"), any(), isNull())
    }

    @Test
    fun `persists all fields on Prison create`() {
      val expectedPrison = givenAPrison()

      whenever(prisonRepository.findById("MDI")).thenReturn(Optional.empty())
      whenever(prisonRepository.save(expectedPrison)).thenReturn(expectedPrison)
      val newPrison = givenAPrisonToInsert()

      prisonService.insertPrison(newPrison)

      val prisonArgumentCaptor = ArgumentCaptor.forClass(Prison::class.java)
      verify(prisonRepository).save(prisonArgumentCaptor.capture())
      val actualPrison = prisonArgumentCaptor.value
      assertTrue(ReflectionEquals(expectedPrison).matches(actualPrison))
    }

    private fun givenAPrisonToInsert(): InsertPrisonDto {
      val address = UpdateAddressDto(
        addressLine1 = "Bawtry Road",
        addressLine2 = "Hatfield Woodhouse",
        town = "Doncaster",
        county = "South Yorkshire",
        postcode = "DN7 6BW",
        country = "England",
      )

      return InsertPrisonDto(
        prisonId = "MDI",
        prisonName = "A Prison 1",
        active = true,
        female = true,
        male = true,
        contracted = true,
        prisonTypes = mutableSetOf(Type.YOI, Type.HMP),
        addresses = listOf(address),
        categories = mutableSetOf(Category.A, Category.D),
      )
    }

    private fun givenAPrison(): Prison {
      val prison = Prison("MDI", "A Prison 1", active = true, female = true, male = true, contracted = true)
      val prisonTypes = mutableSetOf(PrisonType(prison = prison, type = Type.HMP), PrisonType(prison = prison, type = Type.YOI))
      val address = Address(
        addressLine1 = "Bawtry Road",
        addressLine2 = "Hatfield Woodhouse",
        town = "Doncaster",
        county = "South Yorkshire",
        postcode = "DN7 6BW",
        country = "England",
        prison = prison,
      )
      prison.prisonTypes = prisonTypes
      prison.addresses = mutableListOf(address)
      prison.categories = mutableSetOf(Category.A, Category.D)

      return prison
    }
  }

  @Nested
  inner class MaintainPrisons {

    @Test
    fun `try to update a prison that doesn't exist`() {
      whenever(prisonRepository.findById("MDI")).thenReturn(
        Optional.empty(),
      )

      assertThrows(EntityNotFoundException::class.java) {
        prisonService.updatePrison("MDI", UpdatePrisonDto("A Prison 1", true))
      }
      verify(prisonRepository).findById("MDI")
      verifyNoInteractions(telemetryClient)
    }

    @Test
    fun `update a prison adding new prison type`() {
      whenever(prisonRepository.findById("MDI")).thenReturn(
        Optional.of(Prison("MDI", "A prison 1", active = true, female = true, male = false, contracted = false)),
      )

      val updatedPrison =
        prisonService.updatePrison("MDI", UpdatePrisonDto("A prison 1", true, female = false, male = true, contracted = true, prisonTypes = setOf(Type.YOI)))
      assertThat(updatedPrison).isEqualTo(PrisonDto("MDI", "A prison 1", active = true, male = true, female = false, contracted = true, types = listOf(PrisonTypeDto(Type.YOI, Type.YOI.description))))
      verify(prisonRepository).findById("MDI")
      verify(telemetryClient).trackEvent(eq("prison-register-update"), any(), isNull())
    }

    @Test
    fun `update a prison adding new category`() {
      whenever(prisonRepository.findById("MDI")).thenReturn(
        Optional.of(Prison(prisonId = "MDI", name = "A prison 1", categories = mutableSetOf(Category.C), active = true, male = true, contracted = true)),
      )

      val updatedPrison = prisonService.updatePrison(
        "MDI",
        UpdatePrisonDto(
          prisonName = "A prison 1",
          active = true,
          male = true,
          contracted = true,
          categories = setOf(Category.D),
        ),
      )
      assertThat(updatedPrison).isEqualTo(
        PrisonDto(
          prisonId = "MDI",
          prisonName = "A prison 1",
          active = true,
          male = true,
          female = false,
          contracted = true,
          categories = setOf(Category.D),
        ),
      )

      verify(prisonRepository).findById("MDI")
      verify(telemetryClient).trackEvent(eq("prison-register-update"), any(), isNull())
    }

    @Test
    fun `update a prison replacing existing prison type`() {
      val prison = Prison("MDI", "A prison 1", active = true)
      val prisonTypes = mutableSetOf(PrisonType(prison = prison, type = Type.HMP), PrisonType(prison = prison, type = Type.IRC))
      prison.prisonTypes = prisonTypes

      whenever(prisonRepository.findById("MDI")).thenReturn(
        Optional.of(prison),
      )

      val updatedPrison =
        prisonService.updatePrison("MDI", UpdatePrisonDto("A prison 1", true, prisonTypes = setOf(Type.HMP, Type.YOI)))
      assertThat(updatedPrison).isEqualTo(
        PrisonDto(
          "MDI",
          "A prison 1",
          active = true,
          male = false,
          female = false,
          contracted = false,
          types = listOf(
            PrisonTypeDto(Type.HMP, Type.HMP.description),
            PrisonTypeDto(Type.YOI, Type.YOI.description),
          ),
        ),
      )

      verify(prisonRepository).findById("MDI")
      verify(telemetryClient).trackEvent(eq("prison-register-update"), any(), isNull())
    }
  }
}
