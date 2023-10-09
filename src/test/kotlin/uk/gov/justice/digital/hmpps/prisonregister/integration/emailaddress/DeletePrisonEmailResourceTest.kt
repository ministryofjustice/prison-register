package uk.gov.justice.digital.hmpps.prisonregister.integration.emailaddress

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.mockito.kotlin.verifyNoInteractions
import uk.gov.justice.digital.hmpps.prisonregister.integration.ContactDetailsIntegrationTest
import uk.gov.justice.digital.hmpps.prisonregister.model.DepartmentType.OFFENDER_MANAGEMENT_UNIT
import uk.gov.justice.digital.hmpps.prisonregister.model.DepartmentType.SOCIAL_VISIT
import uk.gov.justice.digital.hmpps.prisonregister.model.DepartmentType.VIDEOLINK_CONFERENCING_CENTRE

class DeletePrisonEmailResourceTest : ContactDetailsIntegrationTest() {

  @Test
  fun `When an email is deleted for offender-management-unit, isNoContent is returned and data is deleted`() {
    // Given
    val prisonId = "BRI"
    val departmentType = OFFENDER_MANAGEMENT_UNIT
    val emailAddress = "aled@aled.com"
    createDBData(prisonId, departmentType, emailAddress = emailAddress)
    val endPoint = getLegacyEndPointEmail(prisonId, departmentType)
    // When
    val responseSpec = doDeleteAction(endPoint, prisonId, headers = createMaintainRoleWithWriteScope())

    // Then
    responseSpec.expectStatus().isNoContent
    assertContactDetailsHaveBeenDeleted(prisonId, emailAddress = emailAddress, department = departmentType)
  }

  @Test
  fun `When an offender-management-unit email is deleted that has other contact information, only email is deleted`() {
    // Given
    val prisonId = "BRI"
    val departmentType = OFFENDER_MANAGEMENT_UNIT
    val emailAddress = "aled@aled.com"
    val telephoneAddress = "01348811540"

    createDBData(prisonId, departmentType, emailAddress = emailAddress, telephoneAddress = telephoneAddress)
    val endPoint = getLegacyEndPointEmail(prisonId, departmentType)
    // When
    val responseSpec = doDeleteAction(endPoint, prisonId, headers = createMaintainRoleWithWriteScope())

    // Then
    responseSpec.expectStatus().isNoContent
    assertOnlyEmailHasBeenDeleted(prisonId, emailAddress = emailAddress, telephoneAddress = telephoneAddress, department = departmentType)
  }

  @Test
  fun `When an email is deleted that has other contact information, only email is deleted`() {
    // Given
    val prisonId = "BRI"
    val departmentType = SOCIAL_VISIT
    val emailAddress = "aled@aled.com"
    val telephoneAddress = "01348811540"

    createDBData(prisonId, departmentType, emailAddress = emailAddress, telephoneAddress = telephoneAddress)
    val endPoint = getEndPointEmail(prisonId, departmentType)
    // When
    val responseSpec = doDeleteAction(endPoint, prisonId, headers = createMaintainRoleWithWriteScope())

    // Then
    responseSpec.expectStatus().isNoContent
    assertOnlyEmailHasBeenDeleted(prisonId, emailAddress = emailAddress, telephoneAddress = telephoneAddress, department = departmentType)
  }

  @Test
  fun `When an email deletion has been requested for offender-management-unit without a role, status unauthorized is returned`() {
    // Given
    val endPoint = getLegacyEndPointEmail(prisonId, OFFENDER_MANAGEMENT_UNIT)
    // When
    val responseSpec = doDeleteActionNoRole(endPoint)

    // Then
    responseSpec.expectStatus().isUnauthorized
    verifyNoInteractions(contactDetailsRepository)
    verifyNoInteractions(emailAddressRepository)
  }

  @Test
  fun `When an email deletion has been requested for offender-management-unit with an incorrect role, status forbidden is returned`() {
    // Given
    val prisonId = "BRI"
    val endPoint = getLegacyEndPointEmail(prisonId, OFFENDER_MANAGEMENT_UNIT)
    // When
    val responseSpec = doDeleteAction(endPoint, prisonId, headers = createAnyRole())

    // Then
    responseSpec.expectStatus().isForbidden
    verifyNoInteractions(contactDetailsRepository)
    verifyNoInteractions(emailAddressRepository)
  }

  @Test
  fun `When email address cannot be found for prison, then appropriate error is show`() {
    // Given
    val prisonId = "BRI"
    val departmentType = SOCIAL_VISIT
    val endPoint = getEndPointEmail(prisonId, departmentType)
    // When
    val responseSpec = doDeleteAction(endPoint, headers = createMaintainRoleWithWriteScope())

    // Then
    responseSpec.expectStatus().isNotFound

    val bodyText = getResponseBodyText(responseSpec)
    Assertions.assertEquals("Contact not found for prison ID BRI type social-visit.", bodyText)
  }

  @Test
  fun `When an email is deleted for video-link-conferencing, isNoContent is returned and data is deleted`() {
    // Given
    val prisonId = "BRI"
    val departmentType = VIDEOLINK_CONFERENCING_CENTRE
    val emailAddress = "aled@aled.com"
    createDBData(prisonId, departmentType, emailAddress = emailAddress)
    val endPoint = getLegacyEndPointEmail(prisonId, departmentType)

    // When
    val responseSpec = doDeleteAction(endPoint, prisonId, headers = createMaintainRoleWithWriteScope())

    // Then
    responseSpec.expectStatus().isNoContent
    assertContactDetailsHaveBeenDeleted(prisonId, emailAddress, department = departmentType)
  }

  @Test
  fun `When an email deletion has been requested for video-link-conferencing without a role, status unauthorized is returned`() {
    // Given
    val endPoint = getLegacyEndPointEmail(prisonId, VIDEOLINK_CONFERENCING_CENTRE)
    // When
    val responseSpec = doDeleteActionNoRole(endPoint)

    // Then
    responseSpec.expectStatus().isUnauthorized
    verifyNoInteractions(contactDetailsRepository)
    verifyNoInteractions(emailAddressRepository)
  }

  @Test
  fun `When an email deletion has been requested for video-link-conferencing with an incorrect role, status forbidden is returned`() {
    // Given
    val prisonId = "BRI"
    val endPoint = getLegacyEndPointEmail(prisonId, VIDEOLINK_CONFERENCING_CENTRE)
    // When
    val responseSpec = doDeleteAction(endPoint, prisonId, headers = createAnyRole())

    // Then
    responseSpec.expectStatus().isForbidden
    verifyNoInteractions(contactDetailsRepository)
    verifyNoInteractions(emailAddressRepository)
  }

  @Test
  fun `When an email is deleted, isNoContent is returned and data is deleted`() {
    // Given
    val prisonId = "BRI"
    val departmentType = SOCIAL_VISIT
    val emailAddress = "aled@aled.com"
    val endPoint = getEndPointEmail(prisonId, departmentType)
    createDBData(prisonId, departmentType, emailAddress = emailAddress)

    // When
    val responseSpec = doDeleteAction(endPoint, prisonId, headers = createMaintainRoleWithWriteScope())

    // Then
    responseSpec.expectStatus().isNoContent
    assertContactDetailsHaveBeenDeleted(prisonId, emailAddress = emailAddress, department = departmentType)
  }

  @Test
  fun `When an email deletion has been requested without a role, status unauthorized is returned`() {
    // Given
    val prisonId = "BRI"
    val departmentType = SOCIAL_VISIT
    val endPoint = getEndPointEmail(prisonId, departmentType)

    // When
    val responseSpec = doDeleteActionNoRole(endPoint)

    // Then
    responseSpec.expectStatus().isUnauthorized
    verifyNoInteractions(contactDetailsRepository)
    verifyNoInteractions(emailAddressRepository)
  }

  @Test
  fun `When an email deletion has been requested with an incorrect role, status forbidden is returned`() {
    // Given
    val prisonId = "BRI"
    val departmentType = SOCIAL_VISIT
    val endPoint = getEndPointEmail(prisonId, departmentType)

    // When
    val responseSpec = doDeleteAction(endPoint, prisonId, headers = createAnyRole())

    // Then
    responseSpec.expectStatus().isForbidden
    verifyNoInteractions(contactDetailsRepository)
    verifyNoInteractions(emailAddressRepository)
  }

  @Test
  fun `When department type does not exist, then appropriate error is show`() {
    // Given
    val prisonId = "BRI"
    val endPoint = "/secure/prisons/id/$prisonId/department/i-do-not-exist/email-address"

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
