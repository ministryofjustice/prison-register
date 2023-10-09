package uk.gov.justice.digital.hmpps.prisonregister.integration.telephoneaddress

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.kotlin.verifyNoInteractions
import uk.gov.justice.digital.hmpps.prisonregister.integration.ContactDetailsIntegrationTest
import uk.gov.justice.digital.hmpps.prisonregister.model.DepartmentType.SOCIAL_VISIT
import uk.gov.justice.digital.hmpps.prisonregister.model.DepartmentType.VIDEOLINK_CONFERENCING_CENTRE

class PutPrisonTelephoneAddressResourceTest : ContactDetailsIntegrationTest() {

  @Test
  fun `When an telephone is updated, isNoContent is return and the data is persisted`() {
    // Given
    val prisonId = "BRI"
    val departmentType = SOCIAL_VISIT
    val endPoint = getEndPointTelephoneAddress(prisonId, departmentType)
    val oldTelephoneAddress = "01348811540"
    val newTelephoneAddress = "07505902221"
    createDBData(prisonId, departmentType, telephoneAddress = oldTelephoneAddress)

    // When
    val responseSpec = doPutActionTelephone(endPoint, prisonId, headers = createMaintainRoleWithWriteScope(), telephoneAddress = newTelephoneAddress)

    // Then
    responseSpec.expectStatus().isNoContent
    assertDbContactDetailsExist(prisonId, telephoneAddress = newTelephoneAddress, department = departmentType)
  }

  @Test
  fun `When an telephone is created, isCreated is return and persisted`() {
    // Given
    val newTelephoneAddress = "07505902221"
    val prisonId = "BRI"
    val departmentType = SOCIAL_VISIT
    val endPoint = getEndPointTelephoneAddress(prisonId, SOCIAL_VISIT)

    // When
    val responseSpec = doPutActionTelephone(endPoint, prisonId, headers = createMaintainRoleWithWriteScope(), telephoneAddress = newTelephoneAddress)

    // Then
    responseSpec.expectStatus().isCreated
    assertDbContactDetailsExist(prisonId, telephoneAddress = newTelephoneAddress, department = departmentType)
  }

  @Test
  fun `When an one telephone is added for more than one prison, only one telephone is persisted`() {
    // Given
    val newTelephoneAddress = "07505902221"
    val prisonId1 = "BRI"
    val prisonId2 = "CFI"

    val endPoint1 = getEndPointTelephoneAddress(prisonId1, SOCIAL_VISIT)
    val endPoint2 = getEndPointTelephoneAddress(prisonId2, SOCIAL_VISIT)
    val endPoint3 = getEndPointTelephoneAddress(prisonId2, VIDEOLINK_CONFERENCING_CENTRE)

    // When
    val responseSpec1 = doPutActionTelephone(endPoint1, prisonId1, headers = createMaintainRoleWithWriteScope(), telephoneAddress = newTelephoneAddress)
    val responseSpec1Repeat = doPutActionTelephone(endPoint1, prisonId1, headers = createMaintainRoleWithWriteScope(), telephoneAddress = newTelephoneAddress)
    val responseSpec2 = doPutActionTelephone(endPoint2, prisonId2, headers = createMaintainRoleWithWriteScope(), telephoneAddress = newTelephoneAddress)
    val responseSpec3 = doPutActionTelephone(endPoint3, prisonId2, headers = createMaintainRoleWithWriteScope(), telephoneAddress = newTelephoneAddress)

    // Then
    responseSpec1.expectStatus().isCreated
    responseSpec1Repeat.expectStatus().isNoContent
    responseSpec2.expectStatus().isCreated
    responseSpec3.expectStatus().isCreated

    Assertions.assertThat(testTelephoneAddressRepository.getTelephoneAddressCount(newTelephoneAddress)).isEqualTo(1)
  }

  @Test
  fun `When department type does not exist, then appropriate error is show`() {
    // Given
    val prisonId = "BRI"
    val endPoint = "/secure/prisons/id/$prisonId/department/i-do-not-exist/telephone-address"

    // When
    val responseSpec = doPutActionTelephone(endPoint, headers = createMaintainRoleWithWriteScope())

    // Then
    responseSpec.expectStatus()
      .isBadRequest

    val bodyText = getResponseBodyText(responseSpec)
    assertEquals(
      "Value for DepartmentType is not of a known type i-do-not-exist.",
      bodyText,
    )
  }

  @Test
  fun `When incorrect format for telephone address is used, then appropriate error is show`() {
    // Given
    val prisonId = "BRI"
    val endPoint = getEndPointTelephoneAddress(prisonId, SOCIAL_VISIT)

    // When
    val responseSpec = doPutActionTelephone(endPoint, telephoneAddress = "im-not-a-telephone-number@moj.gov.uk", headers = createMaintainRoleWithWriteScope())

    // Then
    responseSpec.expectStatus()
      .isBadRequest

    responseSpec.expectBody()
      .jsonPath("$.developerMessage")
      .isEqualTo("putTelephoneAddress.telephoneAddress:  telephone address is an incorrect format")
  }

  @Test
  fun `When a new telephone request is sent with without a role, status Unauthorized is returned`() {
    // Given
    val prisonId = "BRI"
    val departmentType = SOCIAL_VISIT
    val endPoint = getEndPointTelephoneAddress(prisonId, departmentType)

    // When
    val responseSpec = doPutActionTelephoneNoRole(endPoint)

    // Then
    responseSpec.expectStatus().isUnauthorized
    verifyNoInteractions(contactDetailsRepository)
    verifyNoInteractions(telephoneAddressRepository)
    assertContactDetailsHaveBeenDeleted(prisonId, department = departmentType)
  }

  @Test
  fun `When a new telephone request is sent with an incorrect role, status Forbidden is returned`() {
    // Given
    val prisonId = "BRI"
    val departmentType = SOCIAL_VISIT
    val endPoint = getEndPointTelephoneAddress(prisonId, departmentType)

    // When
    val responseSpec = doPutActionTelephone(endPoint, prisonId, headers = createAnyRole())

    // Then
    responseSpec.expectStatus().isForbidden
    verifyNoInteractions(contactDetailsRepository)
    verifyNoInteractions(telephoneAddressRepository)
    assertContactDetailsHaveBeenDeleted(prisonId, department = departmentType)
  }
}
