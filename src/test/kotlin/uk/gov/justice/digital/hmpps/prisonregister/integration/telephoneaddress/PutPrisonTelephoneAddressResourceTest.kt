package uk.gov.justice.digital.hmpps.prisonregister.integration.telephoneaddress

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.mockito.kotlin.verifyNoInteractions
import org.springframework.boot.test.mock.mockito.SpyBean
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient.ResponseSpec
import uk.gov.justice.digital.hmpps.prisonregister.integration.IntegrationTest
import uk.gov.justice.digital.hmpps.prisonregister.model.ContactDetails
import uk.gov.justice.digital.hmpps.prisonregister.model.DepartmentType
import uk.gov.justice.digital.hmpps.prisonregister.model.DepartmentType.SOCIAL_VISIT
import uk.gov.justice.digital.hmpps.prisonregister.model.DepartmentType.VIDEOLINK_CONFERENCING_CENTRE
import uk.gov.justice.digital.hmpps.prisonregister.model.Prison
import uk.gov.justice.digital.hmpps.prisonregister.model.PrisonRepository
import uk.gov.justice.digital.hmpps.prisonregister.model.TelephoneAddress
import java.nio.charset.StandardCharsets

class PutPrisonTelephoneAddressResourceTest : IntegrationTest() {

  private val prsonId = "LEI"

  @SpyBean
  private lateinit var prisonRepository: PrisonRepository

  @Test
  fun `When an telephone is updated, isNoContent is return and the data is persisted`() {
    // Given
    val prisonId = "BRI"
    val departmentType = SOCIAL_VISIT
    val endPoint = getEndPoint(prisonId, departmentType)
    val oldTelephoneAddress = "01348811540"
    val newTelephoneAddress = "07505902221"
    createDBData(prisonId, departmentType, oldTelephoneAddress)

    // When
    val responseSpec = doStartAction(endPoint, prisonId, headers = createMaintainRoleWithWriteScope(), telephoneAddress = newTelephoneAddress)

    // Then
    responseSpec.expectStatus().isNoContent
    assertDBValues(prisonId, newTelephoneAddress, departmentType)
  }

  @Test
  fun `When an telephone is created, isCreated is return and persisted`() {
    // Given
    val newTelephoneAddress = "07505902221"
    val prisonId = "BRI"
    val departmentType = SOCIAL_VISIT
    val endPoint = getEndPoint(prisonId, SOCIAL_VISIT)

    // When
    val responseSpec = doStartAction(endPoint, prisonId, headers = createMaintainRoleWithWriteScope(), telephoneAddress = newTelephoneAddress)

    // Then
    responseSpec.expectStatus().isCreated
    assertDBValues(prisonId, newTelephoneAddress, departmentType)
  }

  @Test
  fun `When an one telephone is added for more than one prison, only one telephone is persisted`() {
    // Given
    val newTelephoneAddress = "07505902221"
    val prisonId1 = "BRI"
    val prisonId2 = "CFI"

    val endPoint1 = getEndPoint(prisonId1, SOCIAL_VISIT)
    val endPoint2 = getEndPoint(prisonId2, SOCIAL_VISIT)
    val endPoint3 = getEndPoint(prisonId2, VIDEOLINK_CONFERENCING_CENTRE)

    // When
    val responseSpec1 = doStartAction(endPoint1, prisonId1, headers = createMaintainRoleWithWriteScope(), telephoneAddress = newTelephoneAddress)
    val responseSpec1Repeat = doStartAction(endPoint1, prisonId1, headers = createMaintainRoleWithWriteScope(), telephoneAddress = newTelephoneAddress)
    val responseSpec2 = doStartAction(endPoint2, prisonId2, headers = createMaintainRoleWithWriteScope(), telephoneAddress = newTelephoneAddress)
    val responseSpec3 = doStartAction(endPoint3, prisonId2, headers = createMaintainRoleWithWriteScope(), telephoneAddress = newTelephoneAddress)

    // Then
    responseSpec1.expectStatus().isCreated
    responseSpec1Repeat.expectStatus().isNoContent
    responseSpec2.expectStatus().isCreated
    responseSpec3.expectStatus().isCreated

    Assertions.assertThat(testTelephoneAddressRepository.getTelephoneAddressCount(newTelephoneAddress)).isEqualTo(1)
  }

  @Test
  fun `When department type does not exist, then appropriate error is show`() {
    // Given
    val prisonId = "BRI"
    val endPoint = "/secure/prisons/id/$prisonId/department/i-do-not-exist/telephone-address"

    // When
    val responseSpec = doStartAction(endPoint, headers = createMaintainRoleWithWriteScope())

    // Then
    responseSpec.expectStatus()
      .isBadRequest

    val bodyText = getResponseBodyText(responseSpec)
    org.junit.jupiter.api.Assertions.assertEquals(
      "Value for DepartmentType is not of a known type i-do-not-exist.",
      bodyText,
    )
  }

  @Test
  fun `When incorrect format for telephone address is used, then appropriate error is show`() {
    // Given
    val prisonId = "BRI"
    val endPoint = getEndPoint(prisonId, SOCIAL_VISIT)

    // When
    val responseSpec = doStartAction(endPoint, telephoneAddress = "im-not-a-telephone-number@moj.gov.uk", headers = createMaintainRoleWithWriteScope())

    // Then
    responseSpec.expectStatus()
      .isBadRequest

    responseSpec.expectBody()
      .jsonPath("$.developerMessage")
      .isEqualTo("putTelephoneAddress.telephoneAddress:  telephone address is an incorrect format")
  }

  @Test
  fun `When a new telephone request is sent with without a role, status Unauthorized is returned`() {
    // Given
    val prisonId = "BRI"
    val departmentType = SOCIAL_VISIT
    val endPoint = getEndPoint(prisonId, departmentType)

    // When
    val responseSpec = doStartActionNoRole(endPoint)

    // Then
    responseSpec.expectStatus().isUnauthorized
    verifyNoInteractions(contactDetailsRepository)
    verifyNoInteractions(telephoneAddressRepository)
    assertDBValuesAreNotPersisted(prisonId, departmentType)
  }

  @Test
  fun `When a new telephone request is sent with an incorrect role, status Forbidden is returned`() {
    // Given
    val prisonId = "BRI"
    val departmentType = SOCIAL_VISIT
    val endPoint = getEndPoint(prisonId, departmentType)

    // When
    val responseSpec = doStartAction(endPoint, prisonId, headers = createAnyRole())

    // Then
    responseSpec.expectStatus().isForbidden
    verifyNoInteractions(contactDetailsRepository)
    verifyNoInteractions(telephoneAddressRepository)
    assertDBValuesAreNotPersisted(prisonId, departmentType)
  }

  private fun getEndPoint(
    prisonId: String,
    departmentType: DepartmentType,
  ): String {
    return "/secure/prisons/id/$prisonId/department/${departmentType.pathVariable}/telephone-address"
  }

  private fun assertDBValues(prisonId: String, newTelephoneAddress: String, type: DepartmentType) {
    Assertions.assertThat(telephoneAddressRepository.getTelephoneAddress(newTelephoneAddress)).isNotNull

    val contactDetails = contactDetailsRepository.getByPrisonIdAndType(prisonId, type)
    Assertions.assertThat(contactDetails).isNotNull
    contactDetails?.let {
      with(it) {
        Assertions.assertThat(prisonId).isEqualTo(prisonId)
        Assertions.assertThat(telephoneAddress).isNotNull
        telephoneAddress?.let {
          Assertions.assertThat(it.value).isEqualTo(newTelephoneAddress)
        }
      }
    }
  }

  private fun assertDBValuesAreNotPersisted(prisonId: String, type: DepartmentType) {
    val contactDetails = contactDetailsRepository.getByPrisonIdAndType(prisonId, type)
    Assertions.assertThat(contactDetails).isNull()
  }
  private fun doStartActionNoRole(endPoint: String): ResponseSpec {
    return webTestClient
      .put()
      .uri(endPoint, prsonId)
      .contentType(MediaType.TEXT_PLAIN)
      .bodyValue("07505902221")
      .exchange()
  }

  private fun doStartAction(endPoint: String, prisonID: String? = prsonId, telephoneAddress: String ? = "07505902221", headers: (HttpHeaders) -> Unit): ResponseSpec {
    return webTestClient
      .put()
      .uri(endPoint, prisonID)
      .contentType(MediaType.TEXT_PLAIN)
      .bodyValue(telephoneAddress)
      .headers(headers)
      .exchange()
  }

  private fun createAnyRole(): (HttpHeaders) -> Unit = setAuthorisation(roles = listOf("ANY_ROLE"), scopes = listOf("something"))

  private fun createMaintainRoleWithWriteScope(): (HttpHeaders) -> Unit = setAuthorisation(roles = listOf("ROLE_MAINTAIN_REF_DATA"), scopes = listOf("write"))

  private fun getResponseBodyText(responseSpec: ResponseSpec): String {
    return String(responseSpec.expectBody().returnResult().responseBody, StandardCharsets.UTF_8)
  }

  private fun createDBData(prisonId: String, departmentType: DepartmentType, telephoneAddress: String = "07505902221"): Prison {
    val prison = Prison(prisonId, "$prisonId Prison", active = true)
    prisonRepository.save(prison)

    val persistedTelephoneAddress = telephoneAddressRepository.save(TelephoneAddress(telephoneAddress))
    contactDetailsRepository.saveAndFlush(
      ContactDetails(
        prison.prisonId,
        prison,
        departmentType,
        telephoneAddress = persistedTelephoneAddress,
      ),
    )

    return prison
  }
}
