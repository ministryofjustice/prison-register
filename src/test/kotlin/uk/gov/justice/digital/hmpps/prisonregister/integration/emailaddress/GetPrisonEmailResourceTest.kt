package uk.gov.justice.digital.hmpps.prisonregister.integration.emailaddress

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.prisonregister.integration.ContactDetailsIntegrationTest
import uk.gov.justice.digital.hmpps.prisonregister.model.DepartmentType

class GetPrisonEmailResourceTest : ContactDetailsIntegrationTest() {

  @Test
  fun `When correct details are given for existing email, then email is returned`() {
    // Given
    val prisonId = "BRI"
    val departmentType = DepartmentType.SOCIAL_VISIT
    createDBData(prisonId, departmentType, emailAddress = "aled@moj.gov.uk")

    val endPoint = getEndPointEmail(prisonId, departmentType)

    // When
    val responseSpec = doGetAction(endPoint, createAnyRole())

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
    val departmentType = DepartmentType.OFFENDER_MANAGEMENT_UNIT
    val endPoint = getLegacyEndPointEmail(prisonId, departmentType)
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
    val endPoint = getLegacyEndPointEmail(prisonId, departmentType)
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
    val endPoint = getLegacyEndPointEmail(prisonId, DepartmentType.OFFENDER_MANAGEMENT_UNIT)

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
    val endPoint = getLegacyEndPointEmail(prisonId, DepartmentType.VIDEOLINK_CONFERENCING_CENTRE)

    // When
    val responseSpec = doGetAction(endPoint, createAnyRole())

    // Then
    responseSpec.expectStatus()
      .isNotFound
      .expectBody().isEmpty
  }

  @Test
  fun `When email address cannot be found for prison, then appropriate error is show`() {
    // Given
    val prisonId = "BRI"
    val endPoint = getEndPointEmail(prisonId, DepartmentType.SOCIAL_VISIT)

    // When
    val responseSpec = doGetAction(endPoint, createAnyRole())

    // Then
    responseSpec.expectStatus()
      .isNotFound

    val bodyText = getResponseBodyText(responseSpec)
    assertEquals("Could not find email address for BRI and social-visit.", bodyText)
  }

  @Test
  fun `When department type does not exist, then appropriate error is show`() {
    // Given
    val prisonId = "BRI"
    val endPoint = "/secure/prisons/id/$prisonId/department/i-do-not-exist/email-address"

    // When
    val responseSpec = doGetAction(endPoint, createAnyRole())

    // Then
    responseSpec.expectStatus()
      .isBadRequest

    val bodyText = getResponseBodyText(responseSpec)
    assertEquals("Value for DepartmentType is not of a known type i-do-not-exist.", bodyText)
  }

  @Test
  fun `When no role is give to get email for videolink-conferencing-centre, status unauthorized is returned`() {
    // Given
    val prisonId = "BRI"
    val endPoint = getLegacyEndPointEmail(prisonId, DepartmentType.VIDEOLINK_CONFERENCING_CENTRE)

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
    val departmentType = DepartmentType.OFFENDER_MANAGEMENT_UNIT
    val endPoint = getLegacyEndPointEmail(prisonId, departmentType)

    // When
    val responseSpec = doGetActionNoRole(endPoint)

    // Then
    responseSpec.expectStatus()
      .isUnauthorized
  }

  @Test
  fun `When no role is give to get email for given type, status unauthorized is returned`() {
    // Given
    val prisonId = "BRI"
    val endPoint = getEndPointEmail(prisonId, DepartmentType.SOCIAL_VISIT)

    // When
    val responseSpec = doGetActionNoRole(endPoint)

    // Then
    responseSpec.expectStatus()
      .isUnauthorized
  }
}
