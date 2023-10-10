package uk.gov.justice.digital.hmpps.prisonregister.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.SpyBean
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.reactive.server.WebTestClient.ResponseSpec
import uk.gov.justice.digital.hmpps.prisonregister.model.ContactDetails
import uk.gov.justice.digital.hmpps.prisonregister.model.ContactDetailsRepository
import uk.gov.justice.digital.hmpps.prisonregister.model.DepartmentType
import uk.gov.justice.digital.hmpps.prisonregister.model.EmailAddress
import uk.gov.justice.digital.hmpps.prisonregister.model.EmailAddressRepository
import uk.gov.justice.digital.hmpps.prisonregister.model.PhoneNumber
import uk.gov.justice.digital.hmpps.prisonregister.model.PhoneNumberRepository
import uk.gov.justice.digital.hmpps.prisonregister.model.Prison
import uk.gov.justice.digital.hmpps.prisonregister.model.PrisonRepository
import uk.gov.justice.digital.hmpps.prisonregister.utilities.TestEmailAddressRepository
import uk.gov.justice.digital.hmpps.prisonregister.utilities.TestPhoneNumberRepository
import java.nio.charset.StandardCharsets

@Suppress("SpringJavaInjectionPointsAutowiringInspection")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
abstract class ContactDetailsIntegrationTest : IntegrationTest() {

  val prisonId = "LEI"

  @SpyBean
  lateinit var prisonRepository: PrisonRepository

  @SpyBean
  lateinit var contactDetailsRepository: ContactDetailsRepository

  @SpyBean
  lateinit var emailAddressRepository: EmailAddressRepository

  @SpyBean
  lateinit var phoneNumberRepository: PhoneNumberRepository

  @SpyBean
  lateinit var testEmailAddressRepository: TestEmailAddressRepository

  @SpyBean
  lateinit var testPhoneNumberRepository: TestPhoneNumberRepository

  @AfterEach
  fun `clean up tests`() {
    contactDetailsRepository.deleteAll()
    contactDetailsRepository.flush()
    emailAddressRepository.deleteAll()
    emailAddressRepository.flush()
    phoneNumberRepository.deleteAll()
    phoneNumberRepository.flush()
  }

  fun getResponseBodyText(responseSpec: ResponseSpec): String {
    val responseBody = responseSpec.expectBody().returnResult().responseBody
    assertThat(responseBody).isNotNull
    return String(responseBody!!, StandardCharsets.UTF_8)
  }

  fun createAnyRole(): (HttpHeaders) -> Unit = setAuthorisation(roles = listOf("ANY_ROLE"), scopes = listOf("something"))

  fun createMaintainRoleWithWriteScope(): (HttpHeaders) -> Unit = setAuthorisation(roles = listOf("ROLE_MAINTAIN_REF_DATA"), scopes = listOf("write"))

  fun createDBData(prisonId: String, departmentType: DepartmentType, phoneNumber: String? = null, emailAddress: String? = null): Prison {
    val prison = createOrGetDbPrison(prisonId)
    val persistedPhoneNumber = phoneNumber?.let {
      phoneNumberRepository.getPhoneNumber(phoneNumber) ?: phoneNumberRepository.save(PhoneNumber(phoneNumber))
    }
    val persistedEmailAddress = emailAddress?.let {
      emailAddressRepository.getEmailAddress(emailAddress) ?: emailAddressRepository.save(EmailAddress(emailAddress))
    }

    val contactDetails = contactDetailsRepository.saveAndFlush(
      ContactDetails(
        prison.prisonId,
        prison,
        departmentType,
        phoneNumber = persistedPhoneNumber,
        emailAddress = persistedEmailAddress,
      ),
    )

    prison.contactDetails.add(contactDetails)

    return prison
  }

  private fun createOrGetDbPrison(prisonId: String): Prison {
    return prisonRepository.findByPrisonId(prisonId) ?: prisonRepository.saveAndFlush(
      Prison(
        prisonId,
        "$prisonId Prison",
        active = true,
      ),
    )
  }

  fun getEndPointEmail(
    prisonId: String,
    departmentType: DepartmentType,
  ): String {
    return "/secure/prisons/id/$prisonId/department/${departmentType.pathVariable}/email-address"
  }

  fun getEndPointPhoneNumber(
    prisonId: String,
    departmentType: DepartmentType,
  ): String {
    return "/secure/prisons/id/$prisonId/department/${departmentType.pathVariable}/phone-number"
  }

  fun getLegacyEndPointEmail(
    prisonId: String,
    departmentType: DepartmentType,
  ): String {
    return "/secure/prisons/id/$prisonId/${departmentType.pathVariable}/email-address"
  }

  fun doDeleteActionNoRole(endPoint: String): ResponseSpec {
    return webTestClient
      .delete()
      .uri(endPoint, prisonId)
      .exchange()
  }

  fun doDeleteAction(endPoint: String, prisonID: String? = prisonId, headers: (HttpHeaders) -> Unit): ResponseSpec {
    return webTestClient
      .delete()
      .uri(endPoint, prisonID)
      .headers(headers)
      .exchange()
  }

  fun doGetActionNoRole(endPoint: String): ResponseSpec {
    return webTestClient.get()
      .uri(endPoint).exchange()
  }

  fun doGetAction(endPoint: String, headers: (HttpHeaders) -> Unit): ResponseSpec {
    return webTestClient.get()
      .uri(endPoint)
      .headers(headers)
      .exchange()
  }

  fun doPutActionTelephoneNoRole(endPoint: String): ResponseSpec {
    return webTestClient
      .put()
      .uri(endPoint, prisonId)
      .contentType(MediaType.TEXT_PLAIN)
      .bodyValue("07505902221")
      .exchange()
  }

  fun doPutActionEmailNoRole(endPoint: String): ResponseSpec {
    return webTestClient
      .put()
      .uri(endPoint, prisonId)
      .contentType(MediaType.TEXT_PLAIN)
      .bodyValue("aled@moj.gov.uk")
      .exchange()
  }

  fun doPutActionEmail(endPoint: String, prisonID: String? = prisonId, emailAddress: String ? = "a@a.com", headers: (HttpHeaders) -> Unit): ResponseSpec {
    return webTestClient
      .put()
      .uri(endPoint, prisonID)
      .contentType(MediaType.TEXT_PLAIN)
      .bodyValue(emailAddress)
      .headers(headers)
      .exchange()
  }

  fun doPutActionTelephone(endPoint: String, prisonID: String? = prisonId, phoneNumber: String ? = "07505902221", headers: (HttpHeaders) -> Unit): ResponseSpec {
    return webTestClient
      .put()
      .uri(endPoint, prisonID)
      .contentType(MediaType.TEXT_PLAIN)
      .bodyValue(phoneNumber)
      .headers(headers)
      .exchange()
  }

  fun assertContactDetailsHaveBeenDeleted(prisonId: String, phoneNumber: String ? = null, emailAddress: String ? = null, department: DepartmentType) {
    val contactDetails = contactDetailsRepository.getByPrisonIdAndType(prisonId, department)
    assertThat(contactDetails).isNull()

    phoneNumber?.let {
      val phoneNumberEntity = phoneNumberRepository.getPhoneNumber(phoneNumber)
      assertThat(phoneNumberEntity).isNull()
    }

    emailAddress?.let {
      val emailAddressEntity = emailAddressRepository.getEmailAddress(emailAddress)
      assertThat(emailAddressEntity).isNull()
    }
  }

  fun assertOnlyEmailHasBeenDeleted(prisonId: String, phoneNumber: String, emailAddress: String, department: DepartmentType) {
    val contactDetails = contactDetailsRepository.getByPrisonIdAndType(prisonId, department)
    assertThat(contactDetails).isNotNull

    val phoneNumberEntity = phoneNumberRepository.getPhoneNumber(phoneNumber)
    assertThat(phoneNumberEntity).isNotNull

    val emailAddressEntity = emailAddressRepository.getEmailAddress(emailAddress)
    assertThat(emailAddressEntity).isNull()
  }

  fun assertOnlyTelephoneHasBeenDeleted(prisonId: String, phoneNumber: String, emailAddress: String, department: DepartmentType) {
    val contactDetails = contactDetailsRepository.getByPrisonIdAndType(prisonId, department)
    assertThat(contactDetails).isNotNull

    val phoneNumberEntity = phoneNumberRepository.getPhoneNumber(phoneNumber)
    assertThat(phoneNumberEntity).isNull()

    val emailAddressEntity = emailAddressRepository.getEmailAddress(emailAddress)
    assertThat(emailAddressEntity).isNotNull
  }

  fun assertDbContactDetailsExist(prisonId: String, emailAddress: String? = null, phoneNumber: String? = null, department: DepartmentType) {
    phoneNumber?.let {
      assertThat(phoneNumberRepository.getPhoneNumber(phoneNumber)).isNotNull
    }

    emailAddress?.let {
      assertThat(emailAddressRepository.getEmailAddress(emailAddress)).isNotNull
    }

    val contactDetails = contactDetailsRepository.getByPrisonIdAndType(prisonId, department)
    assertThat(contactDetails).isNotNull
    assertThat(contactDetails!!.prisonId).isEqualTo(prisonId)
    phoneNumber?.let {
      assertThat(contactDetails.phoneNumber).isNotNull
      assertThat(contactDetails.phoneNumber?.value).isEqualTo(it)
    }
    emailAddress?.let {
      assertThat(contactDetails.emailAddress).isNotNull
      assertThat(contactDetails.emailAddress?.value).isEqualTo(it)
    }
  }
}
