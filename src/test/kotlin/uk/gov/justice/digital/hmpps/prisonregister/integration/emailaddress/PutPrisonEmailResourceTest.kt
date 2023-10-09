package uk.gov.justice.digital.hmpps.prisonregister.integration.emailaddress

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.mockito.kotlin.verifyNoInteractions
import uk.gov.justice.digital.hmpps.prisonregister.integration.ContactDetailsIntegrationTest
import uk.gov.justice.digital.hmpps.prisonregister.model.DepartmentType.OFFENDER_MANAGEMENT_UNIT
import uk.gov.justice.digital.hmpps.prisonregister.model.DepartmentType.SOCIAL_VISIT
import uk.gov.justice.digital.hmpps.prisonregister.model.DepartmentType.VIDEOLINK_CONFERENCING_CENTRE

class PutPrisonEmailResourceTest : ContactDetailsIntegrationTest() {

  @Test
  fun `When an email is updated, isNoContent is return and the data is persisted`() {
    // Given
    val prisonId = "BRI"
    val departmentType = SOCIAL_VISIT
    val endPoint = getEndPointEmail(prisonId, departmentType)
    val oldEmailAddress = "old.aled@moj.com"
    val newEmailAddress = "new.aled@moj.com"
    createDBData(prisonId, departmentType, emailAddress = oldEmailAddress)

    // When
    val responseSpec = doPutActionEmail(endPoint, prisonId, headers = createMaintainRoleWithWriteScope(), emailAddress = newEmailAddress)

    // Then
    responseSpec.expectStatus().isNoContent
    assertDbContactDetailsExist(prisonId, emailAddress = newEmailAddress, department = departmentType)
  }

  @Test
  fun `When an email is created, isCreated is return and persisted`() {
    // Given
    val newEmailAddress = "aled@moj.com"
    val prisonId = "BRI"
    val departmentType = SOCIAL_VISIT
    val endPoint = getEndPointEmail(prisonId, SOCIAL_VISIT)

    // When
    val responseSpec = doPutActionEmail(endPoint, prisonId, headers = createMaintainRoleWithWriteScope(), emailAddress = newEmailAddress)

    // Then
    responseSpec.expectStatus().isCreated
    assertDbContactDetailsExist(prisonId, emailAddress = newEmailAddress, department = departmentType)
  }

  @Test
  fun `When an one email is added for more than one prison, only one email is persisted`() {
    // Given
    val newEmailAddress = "new.aled@moj.com"
    val prisonId1 = "BRI"
    val prisonId2 = "CFI"

    val endPoint1 = getEndPointEmail(prisonId1, SOCIAL_VISIT)
    val endPoint2 = getEndPointEmail(prisonId2, SOCIAL_VISIT)
    val endPoint3 = getEndPointEmail(prisonId2, VIDEOLINK_CONFERENCING_CENTRE)

    // When
    val responseSpec1 = doPutActionEmail(endPoint1, prisonId1, headers = createMaintainRoleWithWriteScope(), emailAddress = newEmailAddress)
    val responseSpec1Repeat = doPutActionEmail(endPoint1, prisonId1, headers = createMaintainRoleWithWriteScope(), emailAddress = newEmailAddress)
    val responseSpec2 = doPutActionEmail(endPoint2, prisonId2, headers = createMaintainRoleWithWriteScope(), emailAddress = newEmailAddress)
    val responseSpec3 = doPutActionEmail(endPoint3, prisonId2, headers = createMaintainRoleWithWriteScope(), emailAddress = newEmailAddress)

    // Then
    responseSpec1.expectStatus().isCreated
    responseSpec1Repeat.expectStatus().isNoContent
    responseSpec2.expectStatus().isCreated
    responseSpec3.expectStatus().isCreated

    Assertions.assertThat(testEmailAddressRepository.getEmailCount(newEmailAddress)).isEqualTo(1)
  }

  @Test
  fun `When department type does not exist, then appropriate error is show`() {
    // Given
    val prisonId = "BRI"
    val endPoint = "/secure/prisons/id/$prisonId/department/i-do-not-exist/email-address"

    // When
    val responseSpec = doPutActionEmail(endPoint, headers = createMaintainRoleWithWriteScope())

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
  fun `When a new email request is sent with without a role, status Unauthorized is returned`() {
    // Given
    val prisonId = "BRI"
    val departmentType = SOCIAL_VISIT
    val endPoint = getEndPointEmail(prisonId, departmentType)

    // When
    val responseSpec = doPutActionEmailNoRole(endPoint)

    // Then
    responseSpec.expectStatus().isUnauthorized
    verifyNoInteractions(contactDetailsRepository)
    verifyNoInteractions(emailAddressRepository)
    assertContactDetailsHaveBeenDeleted(prisonId, department = departmentType)
  }

  @Test
  fun `When a new email request is sent with an incorrect role, status Forbidden is returned`() {
    // Given
    val prisonId = "BRI"
    val departmentType = SOCIAL_VISIT
    val endPoint = getEndPointEmail(prisonId, departmentType)

    // When
    val responseSpec = doPutActionEmail(endPoint, prisonId, headers = createAnyRole())

    // Then
    responseSpec.expectStatus().isForbidden
    verifyNoInteractions(contactDetailsRepository)
    verifyNoInteractions(emailAddressRepository)
    assertContactDetailsHaveBeenDeleted(prisonId, department = departmentType)
  }

  @Test
  fun `When an email is updated for offender-management-unit, isNoContent is return and the data is persisted`() {
    // Given
    val prisonId = "BRI"
    val departmentType = OFFENDER_MANAGEMENT_UNIT
    val oldEmailAddress = "aled@aled.com"
    val newEmailAddress = "aled@moj.com"
    createDBData(prisonId, departmentType, emailAddress = oldEmailAddress)
    val endPoint = getLegacyEndPointEmail(prisonId, departmentType)

    // When
    val responseSpec = doPutActionEmail(endPoint, prisonId, headers = createMaintainRoleWithWriteScope(), emailAddress = newEmailAddress)

    // Then
    responseSpec.expectStatus().isNoContent
    assertDbContactDetailsExist(prisonId, emailAddress = newEmailAddress, department = departmentType)
  }

  @Test
  fun `When an email is created for offender-management-unit, isCreated is return and the data is persisted`() {
    // Given
    val prisonId = "BRI"
    val departmentType = OFFENDER_MANAGEMENT_UNIT
    val newEmailAddress = "aled@moj.com"
    val endPoint = getLegacyEndPointEmail(prisonId, departmentType)
    // When
    val responseSpec = doPutActionEmail(endPoint, prisonId, headers = createMaintainRoleWithWriteScope(), emailAddress = newEmailAddress)

    // Then
    responseSpec.expectStatus().isCreated
    assertDbContactDetailsExist(prisonId, emailAddress = newEmailAddress, department = departmentType)
  }

  @Test
  fun `When a new email request is sent for offender-management-unit without a role, status unauthorized is returned`() {
    // Given
    val prisonId = "BRI"
    val departmentType = OFFENDER_MANAGEMENT_UNIT
    val endPoint = getLegacyEndPointEmail(prisonId, departmentType)
    // When
    val responseSpec = doPutActionEmailNoRole(endPoint)

    // Then
    responseSpec.expectStatus().isUnauthorized
    verifyNoInteractions(contactDetailsRepository)
    verifyNoInteractions(emailAddressRepository)
    assertContactDetailsHaveBeenDeleted(prisonId, department = departmentType)
  }

  @Test
  fun `When a new email request is sent for offender-management-unit with incorrect role, status forbidden is returned`() {
    // Given
    val prisonId = "BRI"
    val departmentType = OFFENDER_MANAGEMENT_UNIT
    val endPoint = getLegacyEndPointEmail(prisonId, departmentType)
    // When
    val responseSpec = doPutActionEmail(endPoint, prisonId, headers = createAnyRole())

    // Then
    responseSpec.expectStatus().isForbidden
    verifyNoInteractions(contactDetailsRepository)
    verifyNoInteractions(emailAddressRepository)
    assertContactDetailsHaveBeenDeleted(prisonId, department = departmentType)
  }

  @Test
  fun `When an email is updated for video-link-conferencing, isNoContent is return and the data is persisted`() {
    // Given
    val prisonId = "BRI"
    val departmentType = VIDEOLINK_CONFERENCING_CENTRE
    val oldEmailAddress = "aled@aled.com"
    val newEmailAddress = "aled@moj.com"
    createDBData(prisonId, departmentType, emailAddress = oldEmailAddress)
    val endPoint = getLegacyEndPointEmail(prisonId, departmentType)
    // When
    val responseSpec = doPutActionEmail(endPoint, prisonId, headers = createMaintainRoleWithWriteScope(), emailAddress = newEmailAddress)

    // Then
    responseSpec.expectStatus().isNoContent
    assertDbContactDetailsExist(prisonId, emailAddress = newEmailAddress, department = departmentType)
  }

  @Test
  fun `When an email is created for video-link-conferencing, isCreated is return and the data is persisted`() {
    // Given
    val prisonId = "BRI"
    val departmentType = VIDEOLINK_CONFERENCING_CENTRE
    val newEmailAddress = "aled@moj.com"
    val endPoint = getLegacyEndPointEmail(prisonId, departmentType)
    // When
    val responseSpec = doPutActionEmail(endPoint, prisonId, headers = createMaintainRoleWithWriteScope(), emailAddress = newEmailAddress)

    // Then
    responseSpec.expectStatus().isCreated

    assertDbContactDetailsExist(prisonId, emailAddress = newEmailAddress, department = departmentType)
  }

  @Test
  fun `When a new email request is sent for video-link-conferencing without a role, status unauthorized is returned`() {
    // Given
    val prisonId = "BRI"
    val departmentType = VIDEOLINK_CONFERENCING_CENTRE
    val endPoint = getLegacyEndPointEmail(prisonId, departmentType)
    // When
    val responseSpec = doPutActionEmailNoRole(endPoint)

    // Then
    responseSpec.expectStatus().isUnauthorized
    verifyNoInteractions(contactDetailsRepository)
    verifyNoInteractions(emailAddressRepository)
    assertContactDetailsHaveBeenDeleted(prisonId, department = departmentType)
  }

  @Test
  fun `When a new email request is sent for video-link-conferencing with incorrect role, status forbidden is returned`() {
    // Given
    val prisonId = "BRI"
    val departmentType = VIDEOLINK_CONFERENCING_CENTRE
    val endPoint = getLegacyEndPointEmail(prisonId, departmentType)
    // When
    val responseSpec = doPutActionEmail(endPoint, prisonId, headers = createAnyRole())

    // Then
    responseSpec.expectStatus().isForbidden
    verifyNoInteractions(contactDetailsRepository)
    verifyNoInteractions(emailAddressRepository)
    assertContactDetailsHaveBeenDeleted(prisonId, department = departmentType)
  }
}
