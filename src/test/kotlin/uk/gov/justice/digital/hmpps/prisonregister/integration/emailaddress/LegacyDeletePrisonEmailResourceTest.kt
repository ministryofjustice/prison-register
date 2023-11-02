package uk.gov.justice.digital.hmpps.prisonregister.integration.emailaddress

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.verifyNoInteractions
import uk.gov.justice.digital.hmpps.prisonregister.integration.ContactDetailsBaseIntegrationTest
import uk.gov.justice.digital.hmpps.prisonregister.model.DepartmentType.OFFENDER_MANAGEMENT_UNIT
import uk.gov.justice.digital.hmpps.prisonregister.model.DepartmentType.SOCIAL_VISIT
import uk.gov.justice.digital.hmpps.prisonregister.model.DepartmentType.VIDEOLINK_CONFERENCING_CENTRE

class LegacyDeletePrisonEmailResourceTest : ContactDetailsBaseIntegrationTest() {

  @Test
  fun `When an email is deleted for offender-management-unit, isNoContent is returned and data is deleted`() {
    // Given
    val prisonId = "BRI"
    val departmentType = OFFENDER_MANAGEMENT_UNIT
    val emailAddress = "aled@aled.com"
    createDBData(prisonId, departmentType, emailAddress = emailAddress)
    val endPoint = getLegacyEndPointEmail(prisonId, "offender-management-unit")
    // When
    val responseSpec = doDeleteAction(endPoint, prisonId, headers = createMaintainRefRoleWithWriteScope())

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
    val phoneNumber = "01234567880"
    val webAddress = "www.aled.com"

    createDBData(prisonId, departmentType, emailAddress = emailAddress, phoneNumber = phoneNumber, webAddress = webAddress)
    val endPoint = getLegacyEndPointEmail(prisonId, "offender-management-unit")
    // When
    val responseSpec = doDeleteAction(endPoint, prisonId, headers = createMaintainRefRoleWithWriteScope())

    // Then
    responseSpec.expectStatus().isNoContent

    val phoneNumberEntity = phoneNumberRepository.getPhoneNumber(phoneNumber)
    assertThat(phoneNumberEntity).isNotNull

    val emailAddressEntity = emailAddressRepository.getEmailAddress(emailAddress)
    assertThat(emailAddressEntity).isNull()

    val webAddressAddressEntity = webAddressRepository.get(webAddress)
    assertThat(webAddressAddressEntity).isNotNull
  }

  @Test
  fun `When an departments email is deleted but it is being used other departments, then it is only deleted for that department`() {
    // Given
    val prisonId = "BRI"
    val departmentType = OFFENDER_MANAGEMENT_UNIT
    val otherDepartmentType = SOCIAL_VISIT
    val emailAddress = "aled@aled.com"

    createDBData(prisonId, departmentType, emailAddress = emailAddress)
    createDBData(prisonId, otherDepartmentType, emailAddress = emailAddress)

    val endPoint = getLegacyEndPointEmail(prisonId, "offender-management-unit")
    // When
    val responseSpec = doDeleteAction(endPoint, prisonId, headers = createMaintainRefRoleWithWriteScope())

    // Then
    responseSpec.expectStatus().isNoContent

    val contactDetails = contactDetailsRepository.getByPrisonIdAndType(prisonId, departmentType)
    assertThat(contactDetails).isNull()
    assertDbContactDetailsExist(prisonId, emailAddress = emailAddress, department = otherDepartmentType)
  }

  @Test
  fun `When an email deletion has been requested for offender-management-unit without a role, status unauthorized is returned`() {
    // Given
    val endPoint = getLegacyEndPointEmail(prisonId, "offender-management-unit")
    // When
    val responseSpec = doDeleteActionNoRoleLegacy(endPoint)

    // Then
    responseSpec.expectStatus().isUnauthorized
    verifyNoInteractions(contactDetailsRepository)
    verifyNoInteractions(emailAddressRepository)
  }

  @Test
  fun `When an email deletion has been requested for offender-management-unit with an incorrect role, status forbidden is returned`() {
    // Given
    val prisonId = "BRI"
    val endPoint = getLegacyEndPointEmail(prisonId, "offender-management-unit")
    // When
    val responseSpec = doDeleteAction(endPoint, prisonId, headers = createAnyRole())

    // Then
    responseSpec.expectStatus().isForbidden
    verifyNoInteractions(contactDetailsRepository)
    verifyNoInteractions(emailAddressRepository)
  }

  @Test
  fun `When an email is deleted for video-link-conferencing, isNoContent is returned and data is deleted`() {
    // Given
    val prisonId = "BRI"
    val departmentType = VIDEOLINK_CONFERENCING_CENTRE
    val emailAddress = "aled@aled.com"
    createDBData(prisonId, departmentType, emailAddress = emailAddress)
    val endPoint = getLegacyEndPointEmail(prisonId, "videolink-conferencing-centre")

    // When
    val responseSpec = doDeleteAction(endPoint, prisonId, headers = createMaintainRefRoleWithWriteScope())

    // Then
    responseSpec.expectStatus().isNoContent
    assertContactDetailsHaveBeenDeleted(prisonId, emailAddress, department = departmentType)
  }

  @Test
  fun `When an email deletion has been requested for video-link-conferencing without a role, status unauthorized is returned`() {
    // Given
    val endPoint = getLegacyEndPointEmail(prisonId, "videolink-conferencing-centre")
    // When
    val responseSpec = doDeleteActionNoRoleLegacy(endPoint)

    // Then
    responseSpec.expectStatus().isUnauthorized
    verifyNoInteractions(contactDetailsRepository)
    verifyNoInteractions(emailAddressRepository)
  }

  @Test
  fun `When an email deletion has been requested for video-link-conferencing with an incorrect role, status forbidden is returned`() {
    // Given
    val prisonId = "BRI"
    val endPoint = getLegacyEndPointEmail(prisonId, "videolink-conferencing-centre")
    // When
    val responseSpec = doDeleteAction(endPoint, prisonId, headers = createAnyRole())

    // Then
    responseSpec.expectStatus().isForbidden
    verifyNoInteractions(contactDetailsRepository)
    verifyNoInteractions(emailAddressRepository)
  }
}
