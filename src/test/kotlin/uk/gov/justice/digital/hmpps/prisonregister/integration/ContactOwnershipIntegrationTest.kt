package uk.gov.justice.digital.hmpps.prisonregister.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.prisonregister.dsl.Root
import uk.gov.justice.digital.hmpps.prisonregister.model.ContactDetails
import uk.gov.justice.digital.hmpps.prisonregister.model.CourtRepository
import uk.gov.justice.digital.hmpps.prisonregister.model.DepartmentType
import uk.gov.justice.digital.hmpps.prisonregister.model.DepartmentType.OFFICIAL_VISIT
import uk.gov.justice.digital.hmpps.prisonregister.model.DepartmentType.SOCIAL_VISIT
import uk.gov.justice.digital.hmpps.prisonregister.model.SetOutcome
import uk.gov.justice.digital.hmpps.prisonregister.resource.dto.ContactDetailsDto
import uk.gov.justice.digital.hmpps.prisonregister.service.PrisonService
import uk.gov.justice.digital.hmpps.prisonregister.service.WithMaintenanceMockUser

@WithMaintenanceMockUser
class ContactOwnershipIntegrationTest : ContactDetailsBaseIntegrationTest() {
  @Autowired
  lateinit var prisonService: PrisonService

  @Autowired
  lateinit var dsl: Root

  @Autowired
  lateinit var courtRepository: CourtRepository

  private val email = "ownership@justice.gov.uk"
  private val phone = "0114 555 1234"
  private val web = "https://www.justice.gov.uk"

  @AfterEach
  fun removeCourt() {
    if (courtRepository.existsById("OWNCRT")) courtRepository.deleteById("OWNCRT")
  }

  @Test
  fun `updating and clearing owned contacts removes only their orphan rows`() {
    createContacts()
    val original = contacts(SOCIAL_VISIT)
    val other = contacts(OFFICIAL_VISIT)
    assertThat(original.emailAddress?.id).isNotEqualTo(other.emailAddress?.id)
    assertThat(original.phoneNumber?.id).isNotEqualTo(other.phoneNumber?.id)
    assertThat(original.webAddress?.id).isNotEqualTo(other.webAddress?.id)

    prisonService.updateContactDetails("MDI", dto(SOCIAL_VISIT))
    prisonService.setEmailAddress("MDI", email, SOCIAL_VISIT)
    assertSameChildren(original, contacts(SOCIAL_VISIT))
    prisonService.updateContactDetails("MDI", ContactDetailsDto(SOCIAL_VISIT), removeIfNull = false)
    assertSameChildren(original, contacts(SOCIAL_VISIT))

    prisonService.updateContactDetails(
      "MDI",
      ContactDetailsDto(SOCIAL_VISIT, "new@justice.gov.uk", "0114 555 5678", "https://www.gov.uk"),
    )
    assertChildrenDeleted(original)
    assertSameChildren(other, contacts(OFFICIAL_VISIT))
    val replaced = contacts(SOCIAL_VISIT)

    prisonService.updateContactDetails("MDI", ContactDetailsDto(SOCIAL_VISIT, emailAddress = "new@justice.gov.uk"))
    val partial = contacts(SOCIAL_VISIT)
    assertThat(partial.emailAddress?.id).isEqualTo(replaced.emailAddress?.id)
    assertThat(partial.phoneNumber).isNull()
    assertThat(partial.webAddress).isNull()
    assertThat(phoneNumberRepository.findById(requireNotNull(replaced.phoneNumber).id)).isEmpty
    assertThat(webAddressRepository.findById(requireNotNull(replaced.webAddress).id)).isEmpty

    prisonService.deleteEmailAddress("MDI", SOCIAL_VISIT, throwNotFound = true)
    assertThat(contactDetailsRepository.getByPrisonIdAndType("MDI", SOCIAL_VISIT)).isNull()
    assertThat(emailAddressRepository.findById(requireNotNull(partial.emailAddress).id)).isEmpty
    assertSameChildren(other, contacts(OFFICIAL_VISIT))
  }

  @Test
  fun `agency and department deletion leave independently owned identical contacts intact`() {
    createContacts()
    val original = contacts(SOCIAL_VISIT)
    val other = contacts(OFFICIAL_VISIT)
    var courtEmailId = -1L
    var courtPhoneId = -1L
    dsl.court("OWNCRT", "Ownership Court") {
      courtEmailId = email(email).id
      courtPhoneId = phoneNumber(phone).id
    }

    courtRepository.deleteById("OWNCRT")

    assertThat(emailAddressRepository.findById(courtEmailId)).isEmpty
    assertThat(phoneNumberRepository.findById(courtPhoneId)).isEmpty
    assertSameChildren(original, contacts(SOCIAL_VISIT))
    prisonService.deleteContactDetails("MDI", SOCIAL_VISIT)
    assertChildrenDeleted(original)
    assertSameChildren(other, contacts(OFFICIAL_VISIT))
    assertThat(prisonService.getContactDetails("MDI", OFFICIAL_VISIT)).usingRecursiveComparison().isEqualTo(dto(OFFICIAL_VISIT))
  }

  @Test
  fun `optional children can be added and removed while empty parent behaviour is preserved`() {
    prisonService.createContactDetails("MDI", ContactDetailsDto(SOCIAL_VISIT))
    assertThat(contacts(SOCIAL_VISIT).emailAddress).isNull()
    assertThat(prisonService.setEmailAddress("MDI", email, SOCIAL_VISIT)).isEqualTo(SetOutcome.UPDATED)
    val emailOnly = contacts(SOCIAL_VISIT)
    prisonService.updateContactDetails("MDI", dto(SOCIAL_VISIT))
    assertThat(contacts(SOCIAL_VISIT).emailAddress?.id).isEqualTo(emailOnly.emailAddress?.id)
    val full = contacts(SOCIAL_VISIT)
    prisonService.updateContactDetails("MDI", ContactDetailsDto(SOCIAL_VISIT))
    assertChildrenDeleted(full)
    val empty = contacts(SOCIAL_VISIT)
    assertThat(empty.emailAddress).isNull()
    assertThat(empty.phoneNumber).isNull()
    assertThat(empty.webAddress).isNull()
    prisonService.deleteContactDetails("MDI", SOCIAL_VISIT)
    assertThat(prisonService.setEmailAddress("MDI", email, SOCIAL_VISIT)).isEqualTo(SetOutcome.CREATED)
  }

  private fun createContacts() {
    prisonService.createContactDetails("MDI", dto(SOCIAL_VISIT))
    prisonService.createContactDetails("MDI", dto(OFFICIAL_VISIT))
  }

  private fun dto(type: DepartmentType) = ContactDetailsDto(type, email, phone, web)

  private fun contacts(type: DepartmentType) = requireNotNull(contactDetailsRepository.getByPrisonIdAndType("MDI", type))

  private fun assertSameChildren(expected: ContactDetails, actual: ContactDetails) {
    assertThat(actual.emailAddress?.id).isEqualTo(expected.emailAddress?.id)
    assertThat(actual.phoneNumber?.id).isEqualTo(expected.phoneNumber?.id)
    assertThat(actual.webAddress?.id).isEqualTo(expected.webAddress?.id)
    assertThat(actual.emailAddress?.value).isEqualTo(expected.emailAddress?.value)
    assertThat(actual.phoneNumber?.value).isEqualTo(expected.phoneNumber?.value)
    assertThat(actual.webAddress?.value).isEqualTo(expected.webAddress?.value)
  }

  private fun assertChildrenDeleted(contact: ContactDetails) {
    assertThat(emailAddressRepository.findById(requireNotNull(contact.emailAddress).id)).isEmpty
    assertThat(phoneNumberRepository.findById(requireNotNull(contact.phoneNumber).id)).isEmpty
    assertThat(webAddressRepository.findById(requireNotNull(contact.webAddress).id)).isEmpty
  }
}
