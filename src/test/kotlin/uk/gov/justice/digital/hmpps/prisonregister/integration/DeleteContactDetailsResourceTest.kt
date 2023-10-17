package uk.gov.justice.digital.hmpps.prisonregister.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.verifyNoInteractions
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.prisonregister.model.DepartmentType
import uk.gov.justice.digital.hmpps.prisonregister.model.DepartmentType.OFFENDER_MANAGEMENT_UNIT
import uk.gov.justice.digital.hmpps.prisonregister.model.DepartmentType.SOCIAL_VISIT
import uk.gov.justice.digital.hmpps.prisonregister.resource.dto.ContactDetailsDto

class DeleteContactDetailsResourceTest : ContactDetailsBaseIntegrationTest() {

  @Test
  fun `When contact details cannot be found for prison, then appropriate error is shown`() {
    // Given
    val prisonId = "BRI"
    val endPoint = getContactDetailsEndPoint(prisonId)

    // When
    val responseSpec = doDeleteAction(endPoint, prisonId, DepartmentType.SOCIAL_VISIT, headers = createMaintainRoleWithWriteScope())

    // Then
    responseSpec.expectStatus().isNotFound

    assertDeveloperMessage(responseSpec, "Contact details not found for prison ID BRI for social visit department.")
  }

  @Test
  fun `When an contact details are deleted and is being used other departments, then it is only deleted for that department`() {
    // Given
    val prisonId = "BRI"
    val departmentType = SOCIAL_VISIT
    val otherDepartmentType = OFFENDER_MANAGEMENT_UNIT
    val phoneNumber = "01348811539"

    createDBData(prisonId, departmentType, phoneNumber = phoneNumber)
    createDBData(prisonId, otherDepartmentType, phoneNumber = phoneNumber)

    val endPoint = getContactDetailsEndPoint(prisonId)

    // When
    val responseSpec = doDeleteAction(endPoint, prisonId, departmentType, headers = createMaintainRoleWithWriteScope())

    // Then
    responseSpec.expectStatus().isNoContent

    val contactDetails = contactDetailsRepository.getByPrisonIdAndType(prisonId, departmentType)
    assertThat(contactDetails).isNull()
    assertDbContactDetailsExist(prisonId, phoneNumber = phoneNumber, department = otherDepartmentType)
  }

  @Test
  fun `When contact details are deleted that has other contact information, only phone is deleted`() {
    // Given
    val prisonId = "BRI"
    val departmentType = OFFENDER_MANAGEMENT_UNIT
    val emailAddress = "aled@aled.com"
    val phoneNumber = "01348811540"

    createDBData(prisonId, departmentType, emailAddress = emailAddress, phoneNumber = phoneNumber, webAddress = "www.test.com")
    createDBData(prisonId, SOCIAL_VISIT, emailAddress = emailAddress, webAddress = "www.test.com")

    val endPoint = getContactDetailsEndPoint(prisonId)

    // When
    val responseSpec = doDeleteAction(endPoint, prisonId, departmentType, headers = createMaintainRoleWithWriteScope())

    // Then
    responseSpec.expectStatus().isNoContent
    assertOnlyPhoneHasBeenDeleted(prisonId, emailAddress = emailAddress, phoneNumber = phoneNumber, webAddress = "www.test.com", department = departmentType)
  }

  @Test
  fun `When contact details have been deleted, isNoContent is returned and data is deleted`() {
    // Given
    val prisonId = "BRI"
    val departmentType = SOCIAL_VISIT
    val phoneNumber = "01348811540"

    createDBData(prisonId, departmentType, phoneNumber = phoneNumber)
    val endPoint = getContactDetailsEndPoint(prisonId)

    // When
    val responseSpec = doDeleteAction(endPoint, prisonId, departmentType, headers = createMaintainRoleWithWriteScope())

    // Then
    responseSpec.expectStatus().isNoContent
    assertContactDetailsHaveBeenDeleted(prisonId, phoneNumber = phoneNumber, department = departmentType)
  }

  @Test
  fun `When contact details are requested to be deleted without a role, status unauthorized is returned`() {
    // Given
    val prisonId = "BRI"
    val departmentType = SOCIAL_VISIT
    val endPoint = getContactDetailsEndPoint(prisonId)

    // When
    val responseSpec = doDeleteActionNoRole(endPoint, prisonId, departmentType)

    // Then
    responseSpec.expectStatus().isUnauthorized
    verifyNoInteractions(contactDetailsRepository)
    verifyNoInteractions(phoneNumberRepository)
  }

  @Test
  fun `When contact details are requested to be deleted with an incorrect role, status forbidden is returned`() {
    // Given
    val prisonId = "BRI"
    val departmentType = SOCIAL_VISIT

    val endPoint = getContactDetailsEndPoint(prisonId)

    // When
    val responseSpec = doDeleteAction(endPoint, prisonId, departmentType, headers = createAnyRole())

    // Then
    responseSpec.expectStatus().isForbidden
    verifyNoInteractions(contactDetailsRepository)
    verifyNoInteractions(phoneNumberRepository)
  }

  private fun getResults(returnResult: WebTestClient.BodyContentSpec): ContactDetailsDto {
    return objectMapper.readValue(returnResult.returnResult().responseBody, ContactDetailsDto::class.java)
  }
}
