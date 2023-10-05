package uk.gov.justice.digital.hmpps.prisonregister.integration

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.boot.test.mock.mockito.SpyBean
import org.springframework.http.HttpHeaders
import org.springframework.test.web.reactive.server.WebTestClient.ResponseSpec
import uk.gov.justice.digital.hmpps.prisonregister.model.ContactDetails
import uk.gov.justice.digital.hmpps.prisonregister.model.ContactPurposeType
import uk.gov.justice.digital.hmpps.prisonregister.model.EmailAddress
import uk.gov.justice.digital.hmpps.prisonregister.model.Prison
import uk.gov.justice.digital.hmpps.prisonregister.model.PrisonRepository
import uk.gov.justice.digital.hmpps.prisonregister.resource.OMU
import uk.gov.justice.digital.hmpps.prisonregister.resource.VCC
import java.nio.charset.StandardCharsets

class GetPrisonEmailResourceTest : IntegrationTest() {

  @SpyBean
  private lateinit var prisonRepository: PrisonRepository

  @Test
  fun `When correct details are given for existing email, then email is returned`() {
    // Given
    val prisonId = "BRI"
    val contactPurposeType = ContactPurposeType.SOCIAL_VISIT

    createDBData(prisonId, contactPurposeType)

    val endPoint = "/secure/prisons/id/$prisonId/type/${contactPurposeType.value}/email-address"

    // When
    val responseSpec = doStartAction(endPoint, createAnyRole())

    // Then
    responseSpec.expectStatus()
      .isOk

    val bodyText = getResponseBodyText(responseSpec)
    assertEquals("aled@moj.gov.uk", bodyText)
  }

  @Test
  fun `When correct details are given for existing email for offender-management-unit, then email is returned`() {
    // Given
    val prisonId = "BRI"
    val endPoint = "/secure/prisons/id/$prisonId/$OMU/email-address"
    createDBData(prisonId, ContactPurposeType.OFFENDER_MANAGEMENT_UNIT)

    // When
    val responseSpec = doStartAction(endPoint, createAnyRole())

    // Then
    responseSpec.expectStatus()
      .isOk

    val bodyText = getResponseBodyText(responseSpec)
    assertEquals("aled@moj.gov.uk", bodyText)
  }

  @Test
  fun `When correct details are given for existing email for videolink-conferencing-centre, then email is returned`() {
    // Given
    val prisonId = "BRI"
    val endPoint = "/secure/prisons/id/$prisonId/$VCC/email-address"
    createDBData(prisonId, ContactPurposeType.VIDEO_LINK_CONFERENCING)

    // When
    val responseSpec = doStartAction(endPoint, createAnyRole())

    // Then
    responseSpec.expectStatus()
      .isOk

    val bodyText = getResponseBodyText(responseSpec)
    assertEquals("aled@moj.gov.uk", bodyText)
  }

  @Test
  fun `When email is not found for offender-management-unit , then a not found error is returned`() {
    // Given
    val prisonId = "BRI"

    val endPoint = "/secure/prisons/id/$prisonId/$OMU/email-address"

    // When
    val responseSpec = doStartAction(endPoint, createAnyRole())

    // Then
    responseSpec.expectStatus()
      .isNotFound
      .expectBody().isEmpty
  }

  @Test
  fun `When email is not found for videolink-conferencing-centre, then a not found error is returned`() {
    // Given
    val prisonId = "BRI"

    val endPoint = "/secure/prisons/id/$prisonId/$VCC/email-address"

    // When
    val responseSpec = doStartAction(endPoint, createAnyRole())

    // Then
    responseSpec.expectStatus()
      .isNotFound
      .expectBody().isEmpty
  }

  @Test
  fun `When email address cannot be found for prison, then appropriate error is show`() {
    // Given
    val prisonId = "BRI"
    val contactPurposeType = ContactPurposeType.SOCIAL_VISIT
    val endPoint = "/secure/prisons/id/$prisonId/type/${contactPurposeType.value}/email-address"

    // When
    val responseSpec = doStartAction(endPoint, createAnyRole())

    // Then
    responseSpec.expectStatus()
      .isNotFound

    val bodyText = getResponseBodyText(responseSpec)
    assertEquals("Could not find email address for BRI and social-visit.", bodyText)
  }

  @Test
  fun `When purpose type does not exist, then appropriate error is show`() {
    // Given
    val prisonId = "BRI"
    val endPoint = "/secure/prisons/id/$prisonId/type/i-do-not-exist/email-address"

    // When
    val responseSpec = doStartAction(endPoint, createAnyRole())

    // Then
    responseSpec.expectStatus()
      .isBadRequest

    val bodyText = getResponseBodyText(responseSpec)
    assertEquals("Value for ContactPurposeType is not of a known type i-do-not-exist.", bodyText)
  }

  @Test
  fun `When no role is give to get email for videolink-conferencing-centre, status unauthorized is returned`() {
    // Given
    val prisonId = "BRI"
    val endPoint = "/secure/prisons/id/$prisonId/$VCC/email-address"

    // When
    val responseSpec = doStartActionNoRole(endPoint)

    // Then
    responseSpec.expectStatus()
      .isUnauthorized
  }

  @Test
  fun `When no role is give to get email for offender-management-unit, status unauthorized is returned`() {
    // Given
    val prisonId = "BRI"
    val endPoint = "/secure/prisons/id/$prisonId/$OMU/email-address"

    // When
    val responseSpec = doStartActionNoRole(endPoint)

    // Then
    responseSpec.expectStatus()
      .isUnauthorized
  }

  @Test
  fun `When no role is give to get email for given type, status unauthorized is returned`() {
    // Given
    val prisonId = "BRI"
    val contactPurposeType = ContactPurposeType.SOCIAL_VISIT
    val endPoint = "/secure/prisons/id/$prisonId/type/${contactPurposeType.value}/email-address"

    // When
    val responseSpec = doStartActionNoRole(endPoint)

    // Then
    responseSpec.expectStatus()
      .isUnauthorized
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

  private fun createDBData(prisonId: String, contactPurposeType: ContactPurposeType): Prison {
    val prison = Prison(prisonId, "$prisonId Prison", active = true)
    prisonRepository.save(prison)

    val emailAddress = emailAddressRepository.save(EmailAddress("aled@moj.gov.uk"))
    contactDetailsRepository.saveAndFlush(
      ContactDetails(
        prison.prisonId,
        prison,
        contactPurposeType,
        emailAddress,
      ),
    )

    return prison
  }
}
