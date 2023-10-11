package uk.gov.justice.digital.hmpps.prisonregister.integration.phonenumber

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.mockito.kotlin.verifyNoInteractions
import uk.gov.justice.digital.hmpps.prisonregister.integration.ContactDetailsIntegrationTest
import uk.gov.justice.digital.hmpps.prisonregister.model.DepartmentType.OFFENDER_MANAGEMENT_UNIT
import uk.gov.justice.digital.hmpps.prisonregister.model.DepartmentType.SOCIAL_VISIT

class DeletePrisonPhoneNumberResourceTest : ContactDetailsIntegrationTest() {

  @Test
  fun `When phone number cannot be found for prison, then appropriate error is shown`() {
    // Given
    val prisonId = "BRI"
    val departmentType = SOCIAL_VISIT
    val endPoint = getEndPointPhoneNumber(prisonId, departmentType)

    // When
    val responseSpec = doDeleteAction(endPoint, headers = createMaintainRoleWithWriteScope())

    // Then
    responseSpec.expectStatus().isNotFound

    val bodyText = getResponseBodyText(responseSpec)
    Assertions.assertEquals("Contact not found for prison ID BRI type social-visit.", bodyText)
  }

  @Test
  fun `When an departments phone is deleted but it is being used other departments, then it is only deleted for that department`() {
    // Given
    val prisonId = "BRI"
    val departmentType = SOCIAL_VISIT
    val otherDepartmentType = OFFENDER_MANAGEMENT_UNIT
    val phoneNumber = "01348811539"

    createDBData(prisonId, departmentType, phoneNumber = phoneNumber)
    createDBData(prisonId, otherDepartmentType, phoneNumber = phoneNumber)

    val endPoint = getEndPointPhoneNumber(prisonId, departmentType)
    // When
    val responseSpec = doDeleteAction(endPoint, prisonId, headers = createMaintainRoleWithWriteScope())

    // Then
    responseSpec.expectStatus().isNoContent

    val contactDetails = contactDetailsRepository.getByPrisonIdAndType(prisonId, departmentType)
    assertThat(contactDetails).isNull()
    assertDbContactDetailsExist(prisonId, phoneNumber = phoneNumber, department = otherDepartmentType)
  }

  @Test
  fun `When an phone is deleted that has other contact information, only phone is deleted`() {
    // Given
    val prisonId = "BRI"
    val departmentType = OFFENDER_MANAGEMENT_UNIT
    val emailAddress = "aled@aled.com"
    val phoneNumber = "01348811540"

    createDBData(prisonId, departmentType, emailAddress = emailAddress, phoneNumber = phoneNumber)
    val endPoint = getEndPointPhoneNumber(prisonId, departmentType)
    // When
    val responseSpec = doDeleteAction(endPoint, prisonId, headers = createMaintainRoleWithWriteScope())

    // Then
    responseSpec.expectStatus().isNoContent
    assertOnlyPhoneHasBeenDeleted(prisonId, emailAddress = emailAddress, phoneNumber = phoneNumber, department = departmentType)
  }

  @Test
  fun `When an phone is deleted, isNoContent is returned and data is deleted`() {
    // Given
    val prisonId = "BRI"
    val departmentType = SOCIAL_VISIT
    val phoneNumber = "01348811540"
    val endPoint = getEndPointPhoneNumber(prisonId, departmentType)

    createDBData(prisonId, departmentType, phoneNumber = phoneNumber)

    // When
    val responseSpec = doDeleteAction(endPoint, prisonId, headers = createMaintainRoleWithWriteScope())

    // Then
    responseSpec.expectStatus().isNoContent
    assertContactDetailsHaveBeenDeleted(prisonId, phoneNumber = phoneNumber, department = departmentType)
  }

  @Test
  fun `When an phone deletion has been requested without a role, status unauthorized is returned`() {
    // Given
    val prisonId = "BRI"
    val departmentType = SOCIAL_VISIT
    val endPoint = getEndPointPhoneNumber(prisonId, departmentType)
    // When
    val responseSpec = doDeleteActionNoRole(endPoint)

    // Then
    responseSpec.expectStatus().isUnauthorized
    verifyNoInteractions(contactDetailsRepository)
    verifyNoInteractions(phoneNumberRepository)
  }

  @Test
  fun `When an phone deletion has been requested with an incorrect role, status forbidden is returned`() {
    // Given
    val prisonId = "BRI"
    val departmentType = SOCIAL_VISIT
    // When
    val endPoint = getEndPointPhoneNumber(prisonId, departmentType)

    val responseSpec = doDeleteAction(endPoint, prisonId, headers = createAnyRole())

    // Then
    responseSpec.expectStatus().isForbidden
    verifyNoInteractions(contactDetailsRepository)
    verifyNoInteractions(phoneNumberRepository)
  }

  @Test
  fun `When department type does not exist, then appropriate error is show`() {
    // Given
    val prisonId = "BRI"
    val endPoint = "/secure/prisons/id/$prisonId/department/i-do-not-exist/phone-number"

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
