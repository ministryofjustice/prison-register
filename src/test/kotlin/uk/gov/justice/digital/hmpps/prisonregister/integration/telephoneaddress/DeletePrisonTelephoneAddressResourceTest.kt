package uk.gov.justice.digital.hmpps.prisonregister.integration.telephoneaddress

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.mockito.kotlin.verifyNoInteractions
import org.springframework.boot.test.mock.mockito.SpyBean
import org.springframework.http.HttpHeaders
import org.springframework.test.web.reactive.server.WebTestClient.ResponseSpec
import uk.gov.justice.digital.hmpps.prisonregister.integration.IntegrationTest
import uk.gov.justice.digital.hmpps.prisonregister.model.ContactDetails
import uk.gov.justice.digital.hmpps.prisonregister.model.DepartmentType
import uk.gov.justice.digital.hmpps.prisonregister.model.DepartmentType.SOCIAL_VISIT
import uk.gov.justice.digital.hmpps.prisonregister.model.Prison
import uk.gov.justice.digital.hmpps.prisonregister.model.PrisonRepository
import uk.gov.justice.digital.hmpps.prisonregister.model.TelephoneAddress
import java.nio.charset.StandardCharsets

class DeletePrisonTelephoneAddressResourceTest : IntegrationTest() {

  private val prisonId = "LEI"

  @SpyBean
  private lateinit var prisonRepository: PrisonRepository

  @Test
  fun `When telephone address cannot be found for prison, then appropriate error is show`() {
    // Given
    val prisonId = "BRI"
    val departmentType = SOCIAL_VISIT
    val endPoint = getEndPoint(prisonId, departmentType)

    // When
    val responseSpec = doStartAction(endPoint, headers = createMaintainRoleWithWriteScope())

    // Then
    responseSpec.expectStatus().isNotFound

    val bodyText = getResponseBodyText(responseSpec)
    Assertions.assertEquals("Contact not found for prison ID BRI type social-visit.", bodyText)
  }

  @Test
  fun `When an telephone is deleted, isNoContent is returned and data is deleted`() {
    // Given
    val prisonId = "BRI"
    val departmentType = SOCIAL_VISIT
    val telephoneAddress = "01348811540"
    val endPoint = getEndPoint(prisonId, departmentType)

    createDBData(prisonId, departmentType, telephoneAddress)

    // When
    val responseSpec = doStartAction(endPoint, prisonId, headers = createMaintainRoleWithWriteScope())

    // Then
    responseSpec.expectStatus().isNoContent
    assertDBValuesHaveBeenDeleted(prisonId, telephoneAddress, departmentType)
  }

  @Test
  fun `When an telephone deletion has been requested without a role, status unauthorized is returned`() {
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
  }

  @Test
  fun `When an telephone deletion has been requested with an incorrect role, status forbidden is returned`() {
    // Given
    val prisonId = "BRI"
    val departmentType = SOCIAL_VISIT
    // When
    val endPoint = getEndPoint(prisonId, departmentType)

    val responseSpec = doStartAction(endPoint, prisonId, headers = createAnyRole())

    // Then
    responseSpec.expectStatus().isForbidden
    verifyNoInteractions(contactDetailsRepository)
    verifyNoInteractions(telephoneAddressRepository)
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
    Assertions.assertEquals(
      "Value for DepartmentType is not of a known type i-do-not-exist.",
      bodyText,
    )
  }

  private fun assertDBValuesHaveBeenDeleted(prisonId: String, newTelephoneAddress: String, type: DepartmentType) {
    val contactDetails = contactDetailsRepository.getByPrisonIdAndType(prisonId, type)
    org.assertj.core.api.Assertions.assertThat(contactDetails).isNull()
    val telephoneAddress = telephoneAddressRepository.getTelephoneAddress(newTelephoneAddress)
    org.assertj.core.api.Assertions.assertThat(telephoneAddress).isNull()
  }

  private fun doStartActionNoRole(endPoint: String): ResponseSpec {
    return webTestClient
      .delete()
      .uri(endPoint, prisonId)
      .exchange()
  }

  private fun doStartAction(endPoint: String, prisonID: String? = prisonId, headers: (HttpHeaders) -> Unit): ResponseSpec {
    return webTestClient
      .delete()
      .uri(endPoint, prisonID)
      .headers(headers)
      .exchange()
  }

  private fun createAnyRole(): (HttpHeaders) -> Unit = setAuthorisation(roles = listOf("ANY_ROLE"), scopes = listOf("something"))

  private fun createMaintainRoleWithWriteScope(): (HttpHeaders) -> Unit = setAuthorisation(roles = listOf("ROLE_MAINTAIN_REF_DATA"), scopes = listOf("write"))

  private fun getResponseBodyText(responseSpec: ResponseSpec): String {
    val responseBody = responseSpec.expectBody().returnResult().responseBody
    org.assertj.core.api.Assertions.assertThat(responseBody).isNotNull
    return String(responseBody, StandardCharsets.UTF_8)
  }

  private fun getEndPoint(
    prisonId: String,
    departmentType: DepartmentType,
  ): String {
    return "/secure/prisons/id/$prisonId/department/${departmentType.pathVariable}/telephone-address"
  }

  private fun createDBData(prisonId: String, departmentType: DepartmentType, telephoneAddress: String = "01348811540"): Prison {
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
