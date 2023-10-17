package uk.gov.justice.digital.hmpps.prisonregister.integration

import com.fasterxml.jackson.databind.ObjectMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.SpyBean
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.WebTestClient.ResponseSpec
import org.springframework.web.reactive.function.BodyInserters
import uk.gov.justice.digital.hmpps.prisonregister.model.ContactDetails
import uk.gov.justice.digital.hmpps.prisonregister.model.ContactDetailsRepository
import uk.gov.justice.digital.hmpps.prisonregister.model.DepartmentType
import uk.gov.justice.digital.hmpps.prisonregister.model.EmailAddress
import uk.gov.justice.digital.hmpps.prisonregister.model.EmailAddressRepository
import uk.gov.justice.digital.hmpps.prisonregister.model.PhoneNumber
import uk.gov.justice.digital.hmpps.prisonregister.model.PhoneNumberRepository
import uk.gov.justice.digital.hmpps.prisonregister.model.Prison
import uk.gov.justice.digital.hmpps.prisonregister.model.PrisonRepository
import uk.gov.justice.digital.hmpps.prisonregister.model.WebAddress
import uk.gov.justice.digital.hmpps.prisonregister.model.WebAddressRepository
import uk.gov.justice.digital.hmpps.prisonregister.resource.dto.ContactDetailsDto
import uk.gov.justice.digital.hmpps.prisonregister.utilities.TestEmailAddressRepository
import uk.gov.justice.digital.hmpps.prisonregister.utilities.TestPhoneNumberRepository
import uk.gov.justice.digital.hmpps.prisonregister.utilities.TestWebAddressRepository
import java.nio.charset.StandardCharsets

@Suppress("SpringJavaInjectionPointsAutowiringInspection")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
abstract class ContactDetailsBaseIntegrationTest : IntegrationTest() {

  val prisonId = "LEI"

  @Autowired
  protected lateinit var objectMapper: ObjectMapper

  @SpyBean
  lateinit var prisonRepository: PrisonRepository

  @SpyBean
  lateinit var contactDetailsRepository: ContactDetailsRepository

  @SpyBean
  lateinit var emailAddressRepository: EmailAddressRepository

  @SpyBean
  lateinit var webAddressRepository: WebAddressRepository

  @SpyBean
  lateinit var phoneNumberRepository: PhoneNumberRepository

  @SpyBean
  lateinit var testEmailAddressRepository: TestEmailAddressRepository

  @SpyBean
  lateinit var testPhoneNumberRepository: TestPhoneNumberRepository

  @SpyBean
  lateinit var testWebAddressRepository: TestWebAddressRepository

  @AfterEach
  fun `clean up tests`() {
    contactDetailsRepository.deleteAll()
    contactDetailsRepository.flush()
    emailAddressRepository.deleteAll()
    emailAddressRepository.flush()
    phoneNumberRepository.deleteAll()
    phoneNumberRepository.flush()
    testWebAddressRepository.deleteAll()
    testWebAddressRepository.flush()
  }

  fun getResponseBodyText(responseSpec: ResponseSpec): String {
    val responseBody = responseSpec.expectBody().returnResult().responseBody
    assertThat(responseBody).isNotNull
    return String(responseBody!!, StandardCharsets.UTF_8)
  }

  fun assertDeveloperMessage(responseSpec: ResponseSpec, developersMessage: String) {
    responseSpec.expectBody()
      .jsonPath("$.developerMessage").isEqualTo(developersMessage)
  }

  fun createAnyRole(): (HttpHeaders) -> Unit = setAuthorisation(roles = listOf("ANY_ROLE"), scopes = listOf("something"))

  fun createMaintainRoleWithWriteScope(): (HttpHeaders) -> Unit = setAuthorisation(roles = listOf("ROLE_MAINTAIN_REF_DATA"), scopes = listOf("write"))

  fun createDBData(prisonId: String, dto: ContactDetailsDto): ContactDetailsDto {
    return with(dto) {
      createDBData(prisonId, departmentType = type, phoneNumber = phoneNumber, emailAddress = emailAddress, webAddress = webAddress)
    }
  }

  fun createDBData(prisonId: String, departmentType: DepartmentType, phoneNumber: String? = null, emailAddress: String? = null, webAddress: String? = null): ContactDetailsDto {
    val prison = createOrGetDbPrison(prisonId)
    val persistedPhoneNumber = phoneNumber?.let {
      phoneNumberRepository.getPhoneNumber(phoneNumber) ?: phoneNumberRepository.save(PhoneNumber(phoneNumber))
    }
    val persistedEmailAddress = emailAddress?.let {
      emailAddressRepository.getEmailAddress(emailAddress) ?: emailAddressRepository.save(EmailAddress(emailAddress))
    }
    val webAddressRepository = webAddress?.let {
      webAddressRepository.get(webAddress) ?: webAddressRepository.save(WebAddress(webAddress))
    }

    val contactDetails = contactDetailsRepository.saveAndFlush(
      ContactDetails(
        prison.prisonId,
        prison,
        departmentType,
        phoneNumber = persistedPhoneNumber,
        emailAddress = persistedEmailAddress,
        webAddress = webAddressRepository,
      ),
    )

    prison.contactDetails.add(contactDetails)

    return ContactDetailsDto(contactDetails)
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

  fun getContactDetailsEndPoint(
    prisonId: String,
    removeIfNull: Boolean? = null,
  ): String {
    return removeIfNull?.let {
      "/secure/prisons/id/$prisonId/department/contact-details?removeIfNull=$removeIfNull"
    } ?: "/secure/prisons/id/$prisonId/department/contact-details"
  }

  fun getLegacyEndPointEmail(
    prisonId: String,
    departmentType: String,
  ): String {
    return "/secure/prisons/id/$prisonId/$departmentType/email-address"
  }

  fun doDeleteActionNoRole(endPoint: String, prisonID: String? = prisonId, departmentType: DepartmentType): ResponseSpec {
    return webTestClient
      .delete()
      .uri(endPoint + "?departmentType=${departmentType.name}", prisonID)
      .exchange()
  }

  fun doDeleteActionNoRoleLegacy(endPoint: String): ResponseSpec {
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

  fun doDeleteAction(endPoint: String, prisonID: String? = prisonId, departmentType: DepartmentType, headers: (HttpHeaders) -> Unit): ResponseSpec {
    return webTestClient
      .delete()
      .uri(endPoint + "?departmentType=${departmentType.name}", prisonID)
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

  fun doGetAction(endPoint: String, departmentType: DepartmentType, headers: (HttpHeaders) -> Unit): ResponseSpec {
    return webTestClient.get()
      .uri(endPoint + "?departmentType=${departmentType.name}")
      .headers(headers)
      .exchange()
  }

  fun doCreateContactDetailsAction(endPoint: String, prisonID: String? = prisonId, bodyValue: ContactDetailsDto, headers: (HttpHeaders) -> Unit): ResponseSpec {
    return webTestClient
      .post()
      .uri(endPoint, prisonID)
      .contentType(MediaType.APPLICATION_JSON)
      .body(BodyInserters.fromValue(bodyValue))
      .headers(headers)
      .exchange()
  }

  fun doUpdateContactDetailsAction(endPoint: String, bodyValue: ContactDetailsDto, headers: (HttpHeaders) -> Unit): ResponseSpec {
    return webTestClient
      .put()
      .uri(endPoint)
      .contentType(MediaType.APPLICATION_JSON)
      .body(BodyInserters.fromValue(bodyValue))
      .headers(headers)
      .exchange()
  }

  fun doCreateContactDetailsAction(endPoint: String, prisonID: String? = prisonId, bodyValue: ContactDetailsDto): ResponseSpec {
    return webTestClient
      .post()
      .uri(endPoint, prisonID)
      .contentType(MediaType.APPLICATION_JSON)
      .body(BodyInserters.fromValue(bodyValue))
      .exchange()
  }

  fun doUpdateContactDetailsAction(endPoint: String, bodyValue: ContactDetailsDto): ResponseSpec {
    return webTestClient
      .post()
      .uri(endPoint)
      .contentType(MediaType.APPLICATION_JSON)
      .body(BodyInserters.fromValue(bodyValue))
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

  fun doPutActionLegacyEmail(endPoint: String, prisonID: String? = prisonId, emailAddress: String ? = "a@a.com", headers: (HttpHeaders) -> Unit): ResponseSpec {
    return webTestClient
      .put()
      .uri(endPoint, prisonID)
      .contentType(MediaType.TEXT_PLAIN)
      .bodyValue(emailAddress)
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

  fun assertOnlyEmailHasBeenDeleted(prisonId: String, phoneNumber: String, emailAddress: String, webAddress: String, department: DepartmentType) {
    val contactDetails = contactDetailsRepository.getByPrisonIdAndType(prisonId, department)
    assertThat(contactDetails).isNull()

    val phoneNumberEntity = phoneNumberRepository.getPhoneNumber(phoneNumber)
    assertThat(phoneNumberEntity).isNotNull

    val emailAddressEntity = emailAddressRepository.getEmailAddress(emailAddress)
    assertThat(emailAddressEntity).isNull()

    val webAddressAddressEntity = webAddressRepository.get(webAddress)
    assertThat(webAddressAddressEntity).isNotNull
  }

  fun assertOnlyPhoneHasBeenDeleted(prisonId: String, phoneNumber: String, emailAddress: String, webAddress: String, department: DepartmentType) {
    val contactDetails = contactDetailsRepository.getByPrisonIdAndType(prisonId, department)
    assertThat(contactDetails).isNull()

    val phoneNumberEntity = phoneNumberRepository.getPhoneNumber(phoneNumber)
    assertThat(phoneNumberEntity).isNull()

    val emailAddressEntity = emailAddressRepository.getEmailAddress(emailAddress)
    assertThat(emailAddressEntity).isNotNull

    val webAddressAddressEntity = webAddressRepository.get(webAddress)
    assertThat(webAddressAddressEntity).isNotNull
  }

  fun assertOnlyWebAddressHasBeenDeleted(prisonId: String, phoneNumber: String, emailAddress: String, webAddress: String, department: DepartmentType) {
    val contactDetails = contactDetailsRepository.getByPrisonIdAndType(prisonId, department)
    assertThat(contactDetails).isNull()

    val phoneNumberEntity = phoneNumberRepository.getPhoneNumber(phoneNumber)
    assertThat(phoneNumberEntity).isNotNull

    val emailAddressEntity = emailAddressRepository.getEmailAddress(emailAddress)
    assertThat(emailAddressEntity).isNotNull

    val webAddressAddressEntity = webAddressRepository.get(webAddress)
    assertThat(webAddressAddressEntity).isNull()
  }

  fun assertDbContactDetailsExist(prisonId: String, dto: ContactDetailsDto) {
    with(dto) {
      assertDbContactDetailsExist(prisonId, emailAddress, phoneNumber, webAddress, type)
    }
  }

  fun assertDbContactDetailsExist(prisonId: String, emailAddress: String? = null, phoneNumber: String? = null, webAddress: String? = null, department: DepartmentType) {
    phoneNumber?.let {
      assertThat(phoneNumberRepository.getPhoneNumber(phoneNumber)).isNotNull
    }

    emailAddress?.let {
      assertThat(emailAddressRepository.getEmailAddress(emailAddress)).isNotNull
    }

    webAddress?.let {
      assertThat(webAddressRepository.get(webAddress)).isNotNull
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
    webAddress?.let {
      assertThat(contactDetails.webAddress).isNotNull
      assertThat(contactDetails.webAddress?.value).isEqualTo(it)
    }
  }

  fun getContactDetailsDtoResults(returnResult: WebTestClient.BodyContentSpec): ContactDetailsDto {
    return objectMapper.readValue(returnResult.returnResult().responseBody, ContactDetailsDto::class.java)
  }

  fun assertContactDetailsEquals(dto1: ContactDetailsDto, dto2: ContactDetailsDto) {
    assertEquals(dto1.type, dto2.type)
    assertEquals(dto1.emailAddress, dto2.emailAddress)
    assertEquals(dto1.webAddress, dto2.webAddress)
    assertEquals(dto1.phoneNumber, dto2.phoneNumber)
  }
}
