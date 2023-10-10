package uk.gov.justice.digital.hmpps.prisonregister.integration.telephoneaddress

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.prisonregister.integration.ContactDetailsIntegrationTest
import uk.gov.justice.digital.hmpps.prisonregister.model.DepartmentType.SOCIAL_VISIT

class GetPrisonTelephoneNumberResourceTest : ContactDetailsIntegrationTest() {

  @Test
  fun `When correct details are given for existing telephone, then telephone is returned`() {
    // Given
    val prisonId = "BRI"
    val departmentType = SOCIAL_VISIT
    createDBData(prisonId, departmentType, telephoneAddress = "01348811540")

    val endPoint = getEndPointTelephoneAddress(prisonId, departmentType)

    // When
    val responseSpec = doGetAction(endPoint, createAnyRole())

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
    val endPoint = getEndPointTelephoneAddress(prisonId, SOCIAL_VISIT)

    // When
    val responseSpec = doGetAction(endPoint, createAnyRole())

    // Then
    responseSpec.expectStatus()
      .isNotFound

    val bodyText = getResponseBodyText(responseSpec)
    Assertions.assertEquals("Could not find telephone number for BRI and social-visit.", bodyText)
  }

  @Test
  fun `When department type does not exist, then appropriate error is show`() {
    // Given
    val prisonId = "BRI"
    val endPoint = "/secure/prisons/id/$prisonId/department/i-do-not-exist/telephone-number"

    // When
    val responseSpec = doGetAction(endPoint, createAnyRole())

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
    val endPoint = getEndPointTelephoneAddress(prisonId, SOCIAL_VISIT)

    // When
    val responseSpec = doGetActionNoRole(endPoint)

    // Then
    responseSpec.expectStatus()
      .isUnauthorized
  }
}
