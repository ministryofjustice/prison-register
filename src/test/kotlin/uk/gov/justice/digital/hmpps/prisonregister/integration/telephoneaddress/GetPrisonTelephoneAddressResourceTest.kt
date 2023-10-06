package uk.gov.justice.digital.hmpps.prisonregister.integration.telephoneaddress

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
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

class GetPrisonTelephoneAddressResourceTest : IntegrationTest() {

  @SpyBean
  private lateinit var prisonRepository: PrisonRepository

  @Test
  fun `When correct details are given for existing telephone, then telephone is returned`() {
    // Given
    val prisonId = "BRI"
    val departmentType = SOCIAL_VISIT
    createDBData(prisonId, departmentType)

    val endPoint = getEndPoint(prisonId, departmentType)

    // When
    val responseSpec = doStartAction(endPoint, createAnyRole())

    // Then
    responseSpec.expectStatus()
      .isOk

    val bodyText = getResponseBodyText(responseSpec)
    Assertions.assertEquals("01348811540", bodyText)
  }

  @Test
  fun `When telephone address cannot be found for prison, then appropriate error is show`() {
    // Given
    val prisonId = "BRI"
    val endPoint = getEndPoint(prisonId, SOCIAL_VISIT)

    // When
    val responseSpec = doStartAction(endPoint, createAnyRole())

    // Then
    responseSpec.expectStatus()
      .isNotFound

    val bodyText = getResponseBodyText(responseSpec)
    Assertions.assertEquals("Could not find telephone address for BRI and social-visit.", bodyText)
  }

  @Test
  fun `When department type does not exist, then appropriate error is show`() {
    // Given
    val prisonId = "BRI"
    val endPoint = "/secure/prisons/id/$prisonId/department/i-do-not-exist/telephone-address"

    // When
    val responseSpec = doStartAction(endPoint, createAnyRole())

    // Then
    responseSpec.expectStatus()
      .isBadRequest

    val bodyText = getResponseBodyText(responseSpec)
    Assertions.assertEquals("Value for DepartmentType is not of a known type i-do-not-exist.", bodyText)
  }

  @Test
  fun `When no role is give to get telephone for given type, status unauthorized is returned`() {
    // Given
    val prisonId = "BRI"
    val endPoint = getEndPoint(prisonId, SOCIAL_VISIT)

    // When
    val responseSpec = doStartActionNoRole(endPoint)

    // Then
    responseSpec.expectStatus()
      .isUnauthorized
  }

  private fun getEndPoint(
    prisonId: String,
    departmentType: DepartmentType,
  ): String {
    return "/secure/prisons/id/$prisonId/department/${departmentType.pathVariable}/telephone-address"
  }

  private fun doStartActionNoRole(endPoint: String): ResponseSpec {
    return webTestClient.get()
      .uri(endPoint).exchange()
  }

  private fun doStartAction(endPoint: String, headers: (HttpHeaders) -> Unit): ResponseSpec {
    return webTestClient.get()
      .uri(endPoint)
      .headers(headers)
      .exchange()
  }

  private fun createAnyRole(): (HttpHeaders) -> Unit = setAuthorisation(roles = listOf("ANY_ROLE"), scopes = listOf("something"))

  private fun getResponseBodyText(responseSpec: ResponseSpec): String {
    return String(responseSpec.expectBody().returnResult().responseBody, StandardCharsets.UTF_8)
  }

  private fun createDBData(prisonId: String, departmentType: DepartmentType): Prison {
    val prison = Prison(prisonId, "$prisonId Prison", active = true)
    prisonRepository.save(prison)

    val telephoneAddress = telephoneAddressRepository.save(TelephoneAddress("01348811540"))
    contactDetailsRepository.saveAndFlush(
      ContactDetails(
        prison.prisonId,
        prison,
        departmentType,
        telephoneAddress = telephoneAddress,
      ),
    )

    return prison
  }
}
