package uk.gov.justice.digital.hmpps.prisonregister.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.verifyNoInteractions
import uk.gov.justice.digital.hmpps.prisonregister.model.DepartmentType.OFFENDER_MANAGEMENT_UNIT
import uk.gov.justice.digital.hmpps.prisonregister.model.DepartmentType.OFFICIAL_VISIT
import uk.gov.justice.digital.hmpps.prisonregister.model.DepartmentType.SOCIAL_VISIT

class DeleteContactDetailsResourceTest : ContactDetailsBaseIntegrationTest() {

  @Nested
  inner class SocialVisitContacts {
    @Test
    fun `When contact details have been deleted with maintain ref data role, isNoContent is returned and data is deleted`() {
      // Given
      val prisonId = "BRI"
      val departmentType = SOCIAL_VISIT
      val phoneNumber = "01234567880"

      createDBData(prisonId, departmentType, phoneNumber = phoneNumber)
      val endPoint = getContactDetailsEndPoint(prisonId)

      // When
      val responseSpec =
        doDeleteAction(endPoint, prisonId, departmentType, headers = createMaintainRefRoleWithWriteScope())

      // Then
      responseSpec.expectStatus().isNoContent
      assertContactDetailsHaveBeenDeleted(prisonId, phoneNumber = phoneNumber, department = departmentType)
    }

    @Test
    fun `When contact details have been deleted with maintain prison data role, isNoContent is returned and data is deleted`() {
      // Given
      val prisonId = "BRI"
      val departmentType = SOCIAL_VISIT
      val phoneNumber = "01234567880"

      createDBData(prisonId, departmentType, phoneNumber = phoneNumber)
      val endPoint = getContactDetailsEndPoint(prisonId)

      // When
      val responseSpec =
        doDeleteAction(endPoint, prisonId, departmentType, headers = createMaintainPrisonRoleWithWriteScope())

      // Then
      responseSpec.expectStatus().isNoContent
      assertContactDetailsHaveBeenDeleted(prisonId, phoneNumber = phoneNumber, department = departmentType)
    }

    @Test
    fun `When contact details cannot be found for prison, then appropriate error is shown`() {
      // Given
      val prisonId = "BRI"
      val endPoint = getContactDetailsEndPoint(prisonId)

      // When
      val responseSpec =
        doDeleteAction(endPoint, prisonId, SOCIAL_VISIT, headers = createMaintainRefRoleWithWriteScope())

      // Then
      responseSpec.expectStatus().isNotFound

      assertDeveloperMessage(responseSpec, "Contact details not found for BRI / social visit department.")
    }

    @Test
    fun `When an contact details are deleted and is being used other departments, then it is only deleted for that department`() {
      // Given
      val prisonId = "BRI"
      val departmentType = SOCIAL_VISIT
      val otherDepartmentType = OFFENDER_MANAGEMENT_UNIT
      val phoneNumber = "01234567890"

      createDBData(prisonId, departmentType, phoneNumber = phoneNumber)
      createDBData(prisonId, otherDepartmentType, phoneNumber = phoneNumber)

      val endPoint = getContactDetailsEndPoint(prisonId)

      // When
      val responseSpec =
        doDeleteAction(endPoint, prisonId, departmentType, headers = createMaintainRefRoleWithWriteScope())

      // Then
      responseSpec.expectStatus().isNoContent

      val contactDetails = contactDetailsRepository.getByPrisonIdAndType(prisonId, departmentType)
      assertThat(contactDetails).isNull()
      assertDbContactDetailsExist(prisonId, phoneNumber = phoneNumber, department = otherDepartmentType)
    }

    @Test
    fun `When contact details are deleted but web address and email address are used by others, only phone is deleted`() {
      // Given
      val prisonId = "BRI"
      val departmentType = OFFENDER_MANAGEMENT_UNIT
      val emailAddress = "aled@aled.com"
      val phoneNumber = "01234567880"
      val webAddress = "www.test.com"

      createDBData(
        prisonId,
        departmentType,
        emailAddress = emailAddress,
        phoneNumber = phoneNumber,
        webAddress = webAddress,
      )
      createDBData(prisonId, SOCIAL_VISIT, emailAddress = emailAddress, webAddress = webAddress)

      val endPoint = getContactDetailsEndPoint(prisonId)

      // When
      val responseSpec =
        doDeleteAction(endPoint, prisonId, departmentType, headers = createMaintainRefRoleWithWriteScope())

      // Then
      responseSpec.expectStatus().isNoContent
      assertOnlyPhoneHasBeenDeleted(
        prisonId,
        emailAddress = emailAddress,
        phoneNumber = phoneNumber,
        webAddress = webAddress,
        department = departmentType,
      )
    }

    @Test
    fun `When contact details are deleted but phone number and email address are used by others, only web address is deleted`() {
      // Given
      val prisonId = "BRI"
      val departmentType = OFFENDER_MANAGEMENT_UNIT
      val emailAddress = "aled@aled.com"
      val phoneNumber = "01234567880"
      val webAddress = "www.test.com"

      createDBData(
        prisonId,
        departmentType,
        emailAddress = emailAddress,
        phoneNumber = phoneNumber,
        webAddress = webAddress,
      )
      createDBData(prisonId, SOCIAL_VISIT, emailAddress = emailAddress, phoneNumber = phoneNumber)

      val endPoint = getContactDetailsEndPoint(prisonId)

      // When
      val responseSpec =
        doDeleteAction(endPoint, prisonId, departmentType, headers = createMaintainRefRoleWithWriteScope())

      // Then
      responseSpec.expectStatus().isNoContent
      assertOnlyWebAddressHasBeenDeleted(
        prisonId,
        emailAddress = emailAddress,
        phoneNumber = phoneNumber,
        webAddress = webAddress,
        department = departmentType,
      )
    }

    @Test
    fun `When contact details are deleted but phone number and email address are used by others, only email address is deleted`() {
      // Given
      val prisonId = "BRI"
      val departmentType = OFFENDER_MANAGEMENT_UNIT
      val emailAddress = "aled@aled.com"
      val phoneNumber = "01234567880"
      val webAddress = "www.test.com"

      createDBData(
        prisonId,
        departmentType,
        emailAddress = emailAddress,
        phoneNumber = phoneNumber,
        webAddress = webAddress,
      )
      createDBData(prisonId, SOCIAL_VISIT, webAddress = webAddress, phoneNumber = phoneNumber)

      val endPoint = getContactDetailsEndPoint(prisonId)

      // When
      val responseSpec =
        doDeleteAction(endPoint, prisonId, departmentType, headers = createMaintainRefRoleWithWriteScope())

      // Then
      responseSpec.expectStatus().isNoContent
      assertOnlyEmailHasBeenDeleted(
        prisonId,
        emailAddress = emailAddress,
        phoneNumber = phoneNumber,
        webAddress = webAddress,
        department = departmentType,
      )
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
  }

  @Nested
  inner class OfficialVisitContacts {
    val prisonId = "BRI"
    val departmentType = OFFICIAL_VISIT
    val endPoint = getContactDetailsEndPoint(prisonId)
    val emailAddress = "legal-visits@bri.justice.gov.uk"
    val phoneNumber = "01234567890"
    val webAddress = "https://visits.justice.gov.uk/official"

    @Test
    fun `When official visit contact details have been deleted (maintain ref data role) then isNoContent is returned and data is deleted`() {
      createDBData(prisonId, departmentType, phoneNumber)

      val responseSpec = doDeleteAction(endPoint, prisonId, departmentType, headers = createMaintainRefRoleWithWriteScope())

      responseSpec.expectStatus().isNoContent
      assertContactDetailsHaveBeenDeleted(prisonId, phoneNumber = phoneNumber, department = departmentType)
    }

    @Test
    fun `When official visit contact details have been deleted (maintain prison role) then isNoContent is returned and data is deleted`() {
      createDBData(prisonId, departmentType, phoneNumber)

      val responseSpec = doDeleteAction(endPoint, prisonId, departmentType, headers = createMaintainPrisonRoleWithWriteScope())

      responseSpec.expectStatus().isNoContent
      assertContactDetailsHaveBeenDeleted(prisonId, phoneNumber = phoneNumber, department = departmentType)
    }

    @Test
    fun `When official visit contact details cannot be found for prison, then appropriate error is shown`() {
      val responseSpec = doDeleteAction(endPoint, prisonId, departmentType, headers = createMaintainRefRoleWithWriteScope())

      responseSpec.expectStatus().isNotFound
      assertDeveloperMessage(responseSpec, "Contact details not found for BRI / official visit department.")
    }

    @Test
    fun `When official visit contact is deleted but phone is used by another department, then only phone is deleted`() {
      val otherDepartmentType = OFFENDER_MANAGEMENT_UNIT

      createDBData(prisonId, departmentType, phoneNumber)
      createDBData(prisonId, otherDepartmentType, phoneNumber)

      val responseSpec = doDeleteAction(endPoint, prisonId, departmentType, headers = createMaintainRefRoleWithWriteScope())

      responseSpec.expectStatus().isNoContent

      val contactDetails = contactDetailsRepository.getByPrisonIdAndType(prisonId, departmentType)

      assertThat(contactDetails).isNull()
      assertDbContactDetailsExist(prisonId, phoneNumber = phoneNumber, department = otherDepartmentType)
    }

    @Test
    fun `When contact details are removed for one department the web and email on official visits are still retained`() {
      val otherDepartmentType = OFFENDER_MANAGEMENT_UNIT

      // Contact for official visit
      createDBData(prisonId, departmentType, emailAddress = emailAddress, webAddress = webAddress)

      // Contact for OMU
      createDBData(prisonId, otherDepartmentType, phoneNumber, emailAddress, webAddress)

      // Delete the OMU contact
      val responseSpec = doDeleteAction(endPoint, prisonId, otherDepartmentType, headers = createMaintainRefRoleWithWriteScope())

      responseSpec.expectStatus().isNoContent

      // Check the OMU contact has been removed but web and email detail are still present in the DB for the official visit contact
      assertOnlyPhoneHasBeenDeleted(
        prisonId,
        emailAddress = emailAddress,
        phoneNumber = phoneNumber,
        webAddress = webAddress,
        department = otherDepartmentType,
      )
    }

    @Test
    fun `When contact details are deleted but phone and email are used on official visits contact, only the web address is not present`() {
      val otherDepartmentType = OFFENDER_MANAGEMENT_UNIT

      // Contact for official visit
      createDBData(prisonId, departmentType, phoneNumber = phoneNumber, emailAddress = emailAddress)

      // Contact for OMU
      createDBData(prisonId, otherDepartmentType, phoneNumber, emailAddress, webAddress)

      // Clear the OMU contacts
      val responseSpec = doDeleteAction(endPoint, prisonId, otherDepartmentType, headers = createMaintainRefRoleWithWriteScope())

      responseSpec.expectStatus().isNoContent

      // Check OMU contact is fully removed but official visit contact detail for phone and email are retained in the DB
      assertOnlyWebAddressHasBeenDeleted(
        prisonId,
        emailAddress = emailAddress,
        phoneNumber = phoneNumber,
        webAddress = webAddress,
        department = otherDepartmentType,
      )
    }

    @Test
    fun `When official visit contact details are requested to be deleted without a role, status unauthorized is returned`() {
      val responseSpec = doDeleteActionNoRole(endPoint, prisonId, departmentType)

      responseSpec.expectStatus().isUnauthorized

      verifyNoInteractions(contactDetailsRepository)
      verifyNoInteractions(phoneNumberRepository)
    }

    @Test
    fun `When contact details are requested to be deleted with an incorrect role, status forbidden is returned`() {
      val responseSpec = doDeleteAction(endPoint, prisonId, departmentType, headers = createAnyRole())

      responseSpec.expectStatus().isForbidden

      verifyNoInteractions(contactDetailsRepository)
      verifyNoInteractions(phoneNumberRepository)
    }
  }
}
