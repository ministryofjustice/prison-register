package uk.gov.justice.digital.hmpps.prisonregister.integration.telephoneaddress

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.mockito.kotlin.verifyNoInteractions
import uk.gov.justice.digital.hmpps.prisonregister.integration.ContactDetailsIntegrationTest
import uk.gov.justice.digital.hmpps.prisonregister.model.DepartmentType.OFFENDER_MANAGEMENT_UNIT
import uk.gov.justice.digital.hmpps.prisonregister.model.DepartmentType.SOCIAL_VISIT

class DeletePrisonTelephoneAddressResourceTest : ContactDetailsIntegrationTest() {

  @Test
  fun `When telephone address cannot be found for prison, then appropriate error is show`() {
    // Given
    val prisonId = "BRI"
    val departmentType = SOCIAL_VISIT
    val endPoint = getEndPointTelephoneAddress(prisonId, departmentType)

    // When
    val responseSpec = doDeleteAction(endPoint, headers = createMaintainRoleWithWriteScope())

    // Then
    responseSpec.expectStatus().isNotFound

    val bodyText = getResponseBodyText(responseSpec)
    Assertions.assertEquals("Contact not found for prison ID BRI type social-visit.", bodyText)
  }

  @Test
  fun `When an telephone is deleted that has other contact information, only telephone is deleted`() {
    // Given
    val prisonId = "BRI"
    val departmentType = OFFENDER_MANAGEMENT_UNIT
    val emailAddress = "aled@aled.com"
    val telephoneAddress = "01348811540"

    createDBData(prisonId, departmentType, emailAddress = emailAddress, telephoneAddress = telephoneAddress)
    val endPoint = getEndPointTelephoneAddress(prisonId, departmentType)
    // When
    val responseSpec = doDeleteAction(endPoint, prisonId, headers = createMaintainRoleWithWriteScope())

    // Then
    responseSpec.expectStatus().isNoContent
    assertOnlyTelephoneHasBeenDeleted(prisonId, emailAddress = emailAddress, telephoneAddress = telephoneAddress, department = departmentType)
  }

  @Test
  fun `When an telephone is deleted, isNoContent is returned and data is deleted`() {
    // Given
    val prisonId = "BRI"
    val departmentType = SOCIAL_VISIT
    val telephoneAddress = "01348811540"
    val endPoint = getEndPointTelephoneAddress(prisonId, departmentType)

    createDBData(prisonId, departmentType, telephoneAddress = telephoneAddress)

    // When
    val responseSpec = doDeleteAction(endPoint, prisonId, headers = createMaintainRoleWithWriteScope())

    // Then
    responseSpec.expectStatus().isNoContent
    assertContactDetailsHaveBeenDeleted(prisonId, telephoneAddress = telephoneAddress, department = departmentType)
  }

  @Test
  fun `When an telephone deletion has been requested without a role, status unauthorized is returned`() {
    // Given
    val prisonId = "BRI"
    val departmentType = SOCIAL_VISIT
    val endPoint = getEndPointTelephoneAddress(prisonId, departmentType)
    // When
    val responseSpec = doDeleteActionNoRole(endPoint)

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
    val endPoint = getEndPointTelephoneAddress(prisonId, departmentType)

    val responseSpec = doDeleteAction(endPoint, prisonId, headers = createAnyRole())

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
    val responseSpec = doDeleteAction(endPoint, headers = createMaintainRoleWithWriteScope())

    // Then
    responseSpec.expectStatus()
      .isBadRequest

    val bodyText = getResponseBodyText(responseSpec)
    Assertions.assertEquals(
      "Value for DepartmentType is not of a known type i-do-not-exist.",
      bodyText,
    )
  }
}
