@file:Suppress("ClassName")

package uk.gov.justice.digital.hmpps.prisonregister.service

import com.microsoft.applicationinsights.TelemetryClient
import jakarta.persistence.EntityExistsException
import jakarta.persistence.EntityNotFoundException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.anyString
import org.mockito.internal.matchers.apachecommons.ReflectionEquals
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.isNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.prisonregister.exceptions.ContactDetailsNotFoundException
import uk.gov.justice.digital.hmpps.prisonregister.exceptions.PrisonNotFoundException
import uk.gov.justice.digital.hmpps.prisonregister.model.Address
import uk.gov.justice.digital.hmpps.prisonregister.model.Category
import uk.gov.justice.digital.hmpps.prisonregister.model.ContactDetails
import uk.gov.justice.digital.hmpps.prisonregister.model.ContactDetailsRepository
import uk.gov.justice.digital.hmpps.prisonregister.model.DepartmentType
import uk.gov.justice.digital.hmpps.prisonregister.model.DepartmentType.VIDEOLINK_CONFERENCING_CENTRE
import uk.gov.justice.digital.hmpps.prisonregister.model.EmailAddressRepository
import uk.gov.justice.digital.hmpps.prisonregister.model.PhoneNumberRepository
import uk.gov.justice.digital.hmpps.prisonregister.model.Prison
import uk.gov.justice.digital.hmpps.prisonregister.model.PrisonFilter
import uk.gov.justice.digital.hmpps.prisonregister.model.PrisonRepository
import uk.gov.justice.digital.hmpps.prisonregister.model.PrisonType
import uk.gov.justice.digital.hmpps.prisonregister.model.Type
import uk.gov.justice.digital.hmpps.prisonregister.model.WebAddressRepository
import uk.gov.justice.digital.hmpps.prisonregister.resource.InsertPrisonDto
import uk.gov.justice.digital.hmpps.prisonregister.resource.PrisonDto
import uk.gov.justice.digital.hmpps.prisonregister.resource.PrisonTypeDto
import uk.gov.justice.digital.hmpps.prisonregister.resource.UpdateAddressDto
import uk.gov.justice.digital.hmpps.prisonregister.resource.UpdatePrisonDto
import uk.gov.justice.digital.hmpps.prisonregister.resource.dto.ContactDetailsDto
import java.util.Optional

class PrisonServiceTest {
  private val prisonRepository: PrisonRepository = mock()
  private val contactDetailsRepository: ContactDetailsRepository = mock()
  private val emailAddressRepository: EmailAddressRepository = mock()
  private val phoneNumberRepository: PhoneNumberRepository = mock()
  private val webAddressRepository: WebAddressRepository = mock()
  private val telemetryClient: TelemetryClient = mock()

  private val prisonService = PrisonService(prisonRepository, contactDetailsRepository, emailAddressRepository, phoneNumberRepository, webAddressRepository, telemetryClient)

  @BeforeEach
  fun resetMocks() {
    reset(contactDetailsRepository)
    reset(prisonRepository)
  }

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
        null,
        null,
        null,
        null,
        null,
        prison,
      )
      prison.addresses = setOf(address)
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
    fun `find prison includes Welsh names`() {
      val prison = Prison(
        prisonId = "CFI",
        name = "Cardiff Prison",
        active = true,
        male = true,
        female = false,
        contracted = true,
        categories = mutableSetOf(Category.C),
      )

      val address = Address(
        21,
        "Some Road",
        "Some Area",
        "Cardiff",
        "Some Area",
        "DN7 6BW",
        "Wales",
        "Some Welsh Road",
        "Some Welsh Area",
        "Some Welsh Town",
        "Some Welsh County",
        "Cymru",
        prison,
      )
      prison.addresses = setOf(address)
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
  inner class findByPrisonFilter {
    @Test
    fun `find prison by active and text search`() {
      val prison = Prison("MDI", "Moorland (HMP & YOI)", active = true)
      whenever(prisonRepository.findAll(any<PrisonFilter>())).thenReturn(listOf(prison))
      val results = prisonService.findByPrisonFilter(active = true, textSearch = "moorland")

      assertThat(results).containsOnly(PrisonDto(prison))
    }

    @Test
    fun `find prison by non active and non text search`() {
      val prison = Prison("MNI", "Thameside (HMP)", active = false)
      whenever(prisonRepository.findAll(any<PrisonFilter>())).thenReturn(listOf(prison))
      val results = prisonService.findByPrisonFilter()
      assertThat(results).containsOnly(PrisonDto(prison))
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
        prisonService.insertPrison(InsertPrisonDto("MDI", "A Prison 1", active = true, contracted = false))
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
      prison.addresses = mutableSetOf(address)
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
        prisonService.updatePrison("MDI", UpdatePrisonDto("A Prison 1", active = true))
      }
      verify(prisonRepository).findById("MDI")
      verifyNoInteractions(telemetryClient)
    }

    @Test
    fun `update a prison adding new prison type`() {
      whenever(prisonRepository.findById("MDI")).thenReturn(
        Optional.of(Prison("MDI", "A prison 1", active = true, lthse = false, female = true, male = false, contracted = false)),
      )

      val updatedPrison =
        prisonService.updatePrison("MDI", UpdatePrisonDto("A prison 1", active = true, female = false, male = true, contracted = true, prisonTypes = setOf(Type.YOI)))
      assertThat(updatedPrison).isEqualTo(PrisonDto("MDI", "A prison 1", active = true, lthse = false, male = true, female = false, contracted = true, types = listOf(PrisonTypeDto(Type.YOI, Type.YOI.description))))
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
          lthse = false,
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
          lthse = false,
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
        prisonService.updatePrison("MDI", UpdatePrisonDto("A prison 1", active = true, prisonTypes = setOf(Type.HMP, Type.YOI)))
      assertThat(updatedPrison).isEqualTo(
        PrisonDto(
          "MDI",
          "A prison 1",
          active = true,
          lthse = false,
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

    @Test
    fun `update a prison adding new Welsh name`() {
      whenever(prisonRepository.findById("CFI")).thenReturn(
        Optional.of(Prison("CFI", "HMP Cardiff", prisonNameInWelsh = null, active = true, lthse = false, female = true, male = false, contracted = false)),
      )

      val updatedPrison =
        prisonService.updatePrison("CFI", UpdatePrisonDto("HMP Cardiff", prisonNameInWelsh = "Carchar Caerdydd", active = true, female = false, male = true, contracted = true, prisonTypes = setOf(Type.YOI)))
      assertThat(updatedPrison).isEqualTo(PrisonDto("CFI", "HMP Cardiff", prisonNameInWelsh = "Carchar Caerdydd", active = true, lthse = false, male = true, female = false, contracted = true, types = listOf(PrisonTypeDto(Type.YOI, Type.YOI.description))))
      verify(prisonRepository).findById("CFI")
      verify(telemetryClient).trackEvent(eq("prison-register-update"), any(), isNull())
    }
  }

  @Nested
  inner class DeleteEmailAddress {

    @Test
    fun `delete email not found`() {
      val throwNotFound = true
      whenever(contactDetailsRepository.getByPrisonIdAndType(anyString(), any())).thenReturn(null)
      assertThatThrownBy { prisonService.deleteEmailAddress("XXX", DepartmentType.OFFENDER_MANAGEMENT_UNIT, throwNotFound) }
        .isInstanceOf(ContactDetailsNotFoundException::class.java)
    }
  }

  @Nested
  inner class CreateContactDetails {

    @Test
    fun `should throw PrisonNotFoundException when getReferenceById not found`() {
      val contactDetailDto = ContactDetailsDto(
        VIDEOLINK_CONFERENCING_CENTRE,
        emailAddress = "xxx@moj.gov.uk",
        phoneNumber = "01234567899",
        webAddress = "www.xxxmojdigital.blog.gov.uk",
      )
      whenever(prisonRepository.getReferenceById(any())).thenThrow(PrisonNotFoundException::class.java)
      assertThatThrownBy { prisonService.createContactDetails("XXX", contactDetailDto) }
        .isInstanceOf(PrisonNotFoundException::class.java)
    }

    @Test
    fun `CreateContactDetails ContactDetailsDto not found`() {
      val contactDetailDto = ContactDetailsDto(VIDEOLINK_CONFERENCING_CENTRE, null, null, null)
      val prison = Prison("BRI", "Bri Prison", active = true)
      val contactDetailEntity = ContactDetails(
        prison.prisonId,
        contactDetailDto.type,
        emailAddress = null,
        webAddress = null,
        phoneNumber = null,
      )

      whenever(prisonRepository.getReferenceById(any())).thenReturn(prison)
      whenever(contactDetailsRepository.save(any())).thenReturn(contactDetailEntity)
      val gotContactDetailDto = prisonService.createContactDetails(prison.prisonId, contactDetailDto)
      assertThat(gotContactDetailDto.type).isEqualTo(contactDetailDto.type)
      assertThat(gotContactDetailDto.emailAddress).isNull()
      assertThat(gotContactDetailDto.phoneNumber).isNull()
      assertThat(gotContactDetailDto.webAddress).isNull()
    }
  }

  @Nested
  inner class UpdateContactDetails {

    @Test
    fun `should throw ContactDetailsNotFoundException when getByPrisonIdAndType contactDetail not found`() {
      val removeIfNull = true
      val updateRequest = ContactDetailsDto(VIDEOLINK_CONFERENCING_CENTRE, emailAddress = "xxx@moj.gov.uk", phoneNumber = "01234567899", webAddress = "www.xxxmojdigital.blog.gov.uk")
      val prison = Prison("BRI", "Bri Prison", active = true)

      whenever(prisonRepository.getReferenceById(any())).thenReturn(prison)
      whenever(contactDetailsRepository.getByPrisonIdAndType(anyString(), any())).thenReturn(null)
      assertThatThrownBy { prisonService.updateContactDetails("XXX", updateRequest, removeIfNull) }
        .isInstanceOf(ContactDetailsNotFoundException::class.java)
    }

    @Test
    fun `should throw EntityNotFoundException when prisonId or reference is not found`() {
      val removeIfNull = true
      val updateRequest = ContactDetailsDto(VIDEOLINK_CONFERENCING_CENTRE, emailAddress = "xxx@moj.gov.uk", phoneNumber = "01234567899", webAddress = "www.xxxmojdigital.blog.gov.uk")

      whenever(prisonRepository.getReferenceById(any())).thenThrow(EntityNotFoundException::class.java)
      whenever(contactDetailsRepository.getByPrisonIdAndType(anyString(), any())).thenReturn(null)

      assertThatThrownBy { prisonService.updateContactDetails("XXX", updateRequest, removeIfNull) }
        .isInstanceOf(EntityNotFoundException::class.java)
    }
  }

  @Nested
  inner class SetEmailAddress {

    @Test
    fun `prison getReferenceById not found`() {
      whenever(contactDetailsRepository.getByPrisonIdAndType(anyString(), any())).thenReturn(null)
      whenever(prisonRepository.getReferenceById(anyString())).thenThrow(EntityNotFoundException::class.java)
      assertThatThrownBy { prisonService.setEmailAddress("XXX", "email@gov.uk", DepartmentType.OFFENDER_MANAGEMENT_UNIT) }
        .isInstanceOf(EntityNotFoundException::class.java)
    }
  }
}
