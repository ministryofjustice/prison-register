package uk.gov.justice.digital.hmpps.prisonregister.integration.emailaddress

import org.junit.jupiter.api.Test
import org.mockito.kotlin.verifyNoInteractions
import uk.gov.justice.digital.hmpps.prisonregister.integration.ContactDetailsBaseIntegrationTest
import uk.gov.justice.digital.hmpps.prisonregister.model.DepartmentType.OFFENDER_MANAGEMENT_UNIT
import uk.gov.justice.digital.hmpps.prisonregister.model.DepartmentType.VIDEOLINK_CONFERENCING_CENTRE

class LegacyPutPrisonEmailResourceTest : ContactDetailsBaseIntegrationTest() {

  @Test
  fun `When an email is updated for offender-management-unit, isNoContent is return and the data is persisted`() {
    // Given
    val prisonId = "BRI"
    val departmentType = OFFENDER_MANAGEMENT_UNIT
    val oldEmailAddress = "aled@aled.com"
    val newEmailAddress = "aled@moj.com"
    createDBData(prisonId, departmentType, emailAddress = oldEmailAddress)
    val endPoint = getLegacyEndPointEmail(prisonId, "offender-management-unit")

    // When
    val responseSpec = doPutActionLegacyEmail(endPoint, prisonId, headers = createMaintainRefRoleWithWriteScope(), emailAddress = newEmailAddress)

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
    val endPoint = getLegacyEndPointEmail(prisonId, "offender-management-unit")
    // When
    val responseSpec = doPutActionLegacyEmail(endPoint, prisonId, headers = createMaintainRefRoleWithWriteScope(), emailAddress = newEmailAddress)

    // Then
    responseSpec.expectStatus().isCreated
    assertDbContactDetailsExist(prisonId, emailAddress = newEmailAddress, department = departmentType)
  }

  @Test
  fun `When a new email request is sent for offender-management-unit without a role, status unauthorized is returned`() {
    // Given
    val prisonId = "BRI"
    val departmentType = OFFENDER_MANAGEMENT_UNIT
    val endPoint = getLegacyEndPointEmail(prisonId, "offender-management-unit")
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
    val endPoint = getLegacyEndPointEmail(prisonId, "offender-management-unit")
    // When
    val responseSpec = doPutActionLegacyEmail(endPoint, prisonId, headers = createAnyRole())

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
    val endPoint = getLegacyEndPointEmail(prisonId, "videolink-conferencing-centre")
    // When
    val responseSpec = doPutActionLegacyEmail(endPoint, prisonId, headers = createMaintainRefRoleWithWriteScope(), emailAddress = newEmailAddress)

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
    val endPoint = getLegacyEndPointEmail(prisonId, "videolink-conferencing-centre")
    // When
    val responseSpec = doPutActionLegacyEmail(endPoint, prisonId, headers = createMaintainRefRoleWithWriteScope(), emailAddress = newEmailAddress)

    // Then
    responseSpec.expectStatus().isCreated

    assertDbContactDetailsExist(prisonId, emailAddress = newEmailAddress, department = departmentType)
  }

  @Test
  fun `When a new email request is sent for video-link-conferencing without a role, status unauthorized is returned`() {
    // Given
    val prisonId = "BRI"
    val departmentType = VIDEOLINK_CONFERENCING_CENTRE
    val endPoint = getLegacyEndPointEmail(prisonId, "videolink-conferencing-centre")
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
    val endPoint = getLegacyEndPointEmail(prisonId, "videolink-conferencing-centre")
    // When
    val responseSpec = doPutActionLegacyEmail(endPoint, prisonId, headers = createAnyRole())

    // Then
    responseSpec.expectStatus().isForbidden
    verifyNoInteractions(contactDetailsRepository)
    verifyNoInteractions(emailAddressRepository)
    assertContactDetailsHaveBeenDeleted(prisonId, department = departmentType)
  }
}
