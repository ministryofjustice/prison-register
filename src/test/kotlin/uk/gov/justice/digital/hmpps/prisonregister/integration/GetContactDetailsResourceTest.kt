package uk.gov.justice.digital.hmpps.prisonregister.integration

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.prisonregister.model.DepartmentType
import uk.gov.justice.digital.hmpps.prisonregister.model.DepartmentType.SOCIAL_VISIT
import uk.gov.justice.digital.hmpps.prisonregister.resource.dto.ContactDetailsDto

class GetContactDetailsResourceTest : ContactDetailsBaseIntegrationTest() {

  @Test
  fun `When correct request details are given, then contact details are returned`() {
    // Given
    val prisonId = "BRI"
    val departmentType = SOCIAL_VISIT
    createDBData(prisonId, departmentType, emailAddress = "aled@moj.gov.uk", phoneNumber = "01348811539", webAddress = "www.moj.gov.uk")

    val endPoint = getContactDetailsEndPoint(prisonId)

    // When
    val responseSpec = doGetAction(endPoint, departmentType, createAnyRole())

    // Then
    responseSpec.expectStatus()
      .isOk

    val contactDetailsDto = getResults(responseSpec.expectBody())
    Assertions.assertNotNull(contactDetailsDto)
    Assertions.assertEquals(contactDetailsDto.emailAddress, "aled@moj.gov.uk")
    Assertions.assertEquals(contactDetailsDto.phoneNumber, "01348811539")
    Assertions.assertEquals(contactDetailsDto.webAddress, "www.moj.gov.uk")
  }

  @Test
  fun `When email only exists, then contact details are returned`() {
    // Given
    val prisonId = "BRI"
    val departmentType = DepartmentType.SOCIAL_VISIT
    createDBData(prisonId, departmentType, emailAddress = "aled@moj.gov.uk")

    val endPoint = getContactDetailsEndPoint(prisonId)

    // When
    val responseSpec = doGetAction(endPoint, departmentType, createAnyRole())

    // Then
    responseSpec.expectStatus()
      .isOk

    val contactDetailsDto = getResults(responseSpec.expectBody())
    assertNotNull(contactDetailsDto)
    assertEquals(contactDetailsDto.emailAddress, "aled@moj.gov.uk")
    assertNull(contactDetailsDto.phoneNumber)
    assertNull(contactDetailsDto.webAddress)
  }

  @Test
  fun `When web address only exists, then contact details are returned`() {
    // Given
    val prisonId = "BRI"
    val departmentType = DepartmentType.SOCIAL_VISIT
    createDBData(prisonId, departmentType, webAddress = "www.moj.gov.uk")

    val endPoint = getContactDetailsEndPoint(prisonId)

    // When
    val responseSpec = doGetAction(endPoint, departmentType, createAnyRole())

    // Then
    responseSpec.expectStatus()
      .isOk

    val contactDetailsDto = getResults(responseSpec.expectBody())
    assertNotNull(contactDetailsDto)
    assertNull(contactDetailsDto.emailAddress)
    assertNull(contactDetailsDto.phoneNumber)
    assertEquals(contactDetailsDto.webAddress, "www.moj.gov.uk")
  }

  @Test
  fun `When phone number only exists, then contact details are returned`() {
    // Given
    val prisonId = "BRI"
    val departmentType = SOCIAL_VISIT
    createDBData(prisonId, departmentType, phoneNumber = "01348811539")

    val endPoint = getContactDetailsEndPoint(prisonId)

    // When
    val responseSpec = doGetAction(endPoint, departmentType, createAnyRole())

    // Then
    responseSpec.expectStatus()
      .isOk

    val contactDetailsDto = getResults(responseSpec.expectBody())
    assertNotNull(contactDetailsDto)
    assertNull(contactDetailsDto.emailAddress)
    assertEquals(contactDetailsDto.phoneNumber, "01348811539")
    assertNull(contactDetailsDto.webAddress)
  }

  @Test
  fun `When contact details cannot be found for prison, then appropriate error is show`() {
    // Given
    val prisonId = "BRI"
    val endPoint = getContactDetailsEndPoint(prisonId)

    // When
    val responseSpec = doGetAction(endPoint, SOCIAL_VISIT, createAnyRole())

    // Then
    responseSpec.expectStatus()
      .isNotFound

    assertDeveloperMessage(responseSpec, "Contact details not found for prison ID BRI for social visit department.")
  }

  @Test
  fun `When no role is give to get phone for given type, status unauthorized is returned`() {
    // Given
    val prisonId = "BRI"
    val endPoint = getContactDetailsEndPoint(prisonId)

    // When
    val responseSpec = doGetActionNoRole(endPoint)

    // Then
    responseSpec.expectStatus()
      .isUnauthorized
  }

  private fun getResults(returnResult: WebTestClient.BodyContentSpec): ContactDetailsDto {
    return objectMapper.readValue(returnResult.returnResult().responseBody, ContactDetailsDto::class.java)
  }
}
