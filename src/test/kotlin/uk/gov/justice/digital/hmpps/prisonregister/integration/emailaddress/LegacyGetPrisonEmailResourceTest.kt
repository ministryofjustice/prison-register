package uk.gov.justice.digital.hmpps.prisonregister.integration.emailaddress

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.prisonregister.integration.ContactDetailsBaseIntegrationTest
import uk.gov.justice.digital.hmpps.prisonregister.model.DepartmentType

class LegacyGetPrisonEmailResourceTest : ContactDetailsBaseIntegrationTest() {

  @Test
  fun `When correct details are given for existing email for offender-management-unit, then email is returned`() {
    // Given
    val prisonId = "BRI"
    val departmentType = DepartmentType.OFFENDER_MANAGEMENT_UNIT
    val endPoint = getLegacyEndPointEmail(prisonId, "offender-management-unit")
    createDBData(prisonId, departmentType, emailAddress = "aled@moj.gov.uk")

    // When
    val responseSpec = doGetAction(endPoint, createAnyRole())

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
    val departmentType = DepartmentType.VIDEOLINK_CONFERENCING_CENTRE
    val endPoint = getLegacyEndPointEmail(prisonId, "videolink-conferencing-centre")
    createDBData(prisonId, departmentType, emailAddress = "aled@moj.gov.uk")

    // When
    val responseSpec = doGetAction(endPoint, createAnyRole())

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
    val endPoint = getLegacyEndPointEmail(prisonId, "offender-management-unit")

    // When
    val responseSpec = doGetAction(endPoint, createAnyRole())

    // Then
    responseSpec.expectStatus()
      .isNotFound
      .expectBody().isEmpty
  }

  @Test
  fun `When email is not found for videolink-conferencing-centre, then a not found error is returned`() {
    // Given
    val prisonId = "BRI"
    val endPoint = getLegacyEndPointEmail(prisonId, "videolink-conferencing-centre")

    // When
    val responseSpec = doGetAction(endPoint, createAnyRole())

    // Then
    responseSpec.expectStatus()
      .isNotFound
      .expectBody().isEmpty
  }

  @Test
  fun `When no role is give to get email for videolink-conferencing-centre, status unauthorized is returned`() {
    // Given
    val prisonId = "BRI"
    val endPoint = getLegacyEndPointEmail(prisonId, "videolink-conferencing-centre")

    // When
    val responseSpec = doGetActionNoRole(endPoint)

    // Then
    responseSpec.expectStatus()
      .isUnauthorized
  }

  @Test
  fun `When no role is give to get email for offender-management-unit, status unauthorized is returned`() {
    // Given
    val prisonId = "BRI"
    val endPoint = getLegacyEndPointEmail(prisonId, "offender-management-unit")

    // When
    val responseSpec = doGetActionNoRole(endPoint)

    // Then
    responseSpec.expectStatus()
      .isUnauthorized
  }
}
