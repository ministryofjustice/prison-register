package uk.gov.justice.digital.hmpps.prisonregister.integration

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.prisonregister.model.DepartmentType
import uk.gov.justice.digital.hmpps.prisonregister.model.DepartmentType.OFFICIAL_VISIT
import uk.gov.justice.digital.hmpps.prisonregister.model.DepartmentType.SOCIAL_VISIT

class GetContactDetailsResourceTest : ContactDetailsBaseIntegrationTest() {

  @Nested
  inner class SocialVisitContacts {
    @Test
    fun `When correct request details are given, then contact details are returned`() {
      // Given
      val prisonId = "BRI"
      val departmentType = SOCIAL_VISIT
      createDBData(
        prisonId,
        departmentType,
        emailAddress = "aled@moj.gov.uk",
        phoneNumber = "01234567890",
        webAddress = "www.moj.gov.uk",
      )

      val endPoint = getContactDetailsEndPoint(prisonId)

      // When
      val responseSpec = doGetAction(endPoint, departmentType, createAnyRole())

      // Then
      responseSpec.expectStatus()
        .isOk

      val contactDetailsDto = getContactDetailsDtoResults(responseSpec.expectBody())
      assertNotNull(contactDetailsDto)
      assertEquals(contactDetailsDto.emailAddress, "aled@moj.gov.uk")
      assertEquals(contactDetailsDto.phoneNumber, "01234567890")
      assertEquals(contactDetailsDto.webAddress, "www.moj.gov.uk")
    }

    @Test
    fun `When email only exists, then contact details are returned`() {
      // Given
      val prisonId = "BRI"
      val departmentType = DepartmentType.SOCIAL_VISIT
      createDBData(prisonId, departmentType, emailAddress = "aled@moj.gov.uk")

      val endPoint = getContactDetailsEndPoint(prisonId)

      // When
      val responseSpec = doGetAction(endPoint, departmentType, createAnyRole())

      // Then
      responseSpec.expectStatus()
        .isOk

      val contactDetailsDto = getContactDetailsDtoResults(responseSpec.expectBody())
      assertNotNull(contactDetailsDto)
      assertEquals(contactDetailsDto.emailAddress, "aled@moj.gov.uk")
      assertNull(contactDetailsDto.phoneNumber)
      assertNull(contactDetailsDto.webAddress)
    }

    @Test
    fun `When web address only exists, then contact details are returned`() {
      // Given
      val prisonId = "BRI"
      val departmentType = DepartmentType.SOCIAL_VISIT
      createDBData(prisonId, departmentType, webAddress = "www.moj.gov.uk")

      val endPoint = getContactDetailsEndPoint(prisonId)

      // When
      val responseSpec = doGetAction(endPoint, departmentType, createAnyRole())

      // Then
      responseSpec.expectStatus()
        .isOk

      val contactDetailsDto = getContactDetailsDtoResults(responseSpec.expectBody())
      assertNotNull(contactDetailsDto)
      assertNull(contactDetailsDto.emailAddress)
      assertNull(contactDetailsDto.phoneNumber)
      assertEquals(contactDetailsDto.webAddress, "www.moj.gov.uk")
    }

    @Test
    fun `When phone number only exists, then contact details are returned`() {
      // Given
      val prisonId = "BRI"
      val departmentType = SOCIAL_VISIT
      createDBData(prisonId, departmentType, phoneNumber = "01234567890")

      val endPoint = getContactDetailsEndPoint(prisonId)

      // When
      val responseSpec = doGetAction(endPoint, departmentType, createAnyRole())

      // Then
      responseSpec.expectStatus()
        .isOk

      val contactDetailsDto = getContactDetailsDtoResults(responseSpec.expectBody())
      assertNotNull(contactDetailsDto)
      assertNull(contactDetailsDto.emailAddress)
      assertEquals(contactDetailsDto.phoneNumber, "01234567890")
      assertNull(contactDetailsDto.webAddress)
    }

    @Test
    fun `When contact details cannot be found for prison, then appropriate error is show`() {
      // Given
      val prisonId = "BRI"
      val endPoint = getContactDetailsEndPoint(prisonId)

      // When
      val responseSpec = doGetAction(endPoint, SOCIAL_VISIT, createAnyRole())

      // Then
      responseSpec.expectStatus()
        .isNotFound

      assertDeveloperMessage(responseSpec, "Contact details not found for BRI / social visit department.")
    }

    @Test
    fun `When no role is give to get phone for given type, status unauthorized is returned`() {
      // Given
      val prisonId = "BRI"
      val endPoint = getContactDetailsEndPoint(prisonId)

      // When
      val responseSpec = doGetActionNoRole(endPoint)

      // Then
      responseSpec.expectStatus().isUnauthorized
    }
  }

  @Nested
  inner class OfficialVisitContacts {
    val prisonId = "BRI"
    val endPoint = getContactDetailsEndPoint(prisonId)
    val emailAddress = "legal-visits@bri.justice.gov.uk"
    val phoneNumber = "01234567890"
    val webAddress = "https://visits.justice.gov.uk/official"

    @Test
    fun `When full request details are available the full official visit contact details are returned`() {
      val departmentType = OFFICIAL_VISIT
      createDBData(prisonId, departmentType, phoneNumber, emailAddress, webAddress)

      val responseSpec = doGetAction(endPoint, departmentType, createAnyRole())

      responseSpec.expectStatus().isOk

      val contactDetailsDto = getContactDetailsDtoResults(responseSpec.expectBody())

      assertNotNull(contactDetailsDto)
      assertEquals(contactDetailsDto.emailAddress, emailAddress)
      assertEquals(contactDetailsDto.phoneNumber, phoneNumber)
      assertEquals(contactDetailsDto.webAddress, webAddress)
    }

    @Test
    fun `When only an email address exists then the official visit contact email address is returned`() {
      val departmentType = OFFICIAL_VISIT
      createDBData(prisonId, departmentType, emailAddress = emailAddress)

      val responseSpec = doGetAction(endPoint, departmentType, createAnyRole())

      responseSpec.expectStatus().isOk

      val contactDetailsDto = getContactDetailsDtoResults(responseSpec.expectBody())

      assertNotNull(contactDetailsDto)
      assertEquals(contactDetailsDto.emailAddress, emailAddress)
      assertNull(contactDetailsDto.phoneNumber)
      assertNull(contactDetailsDto.webAddress)
    }

    @Test
    fun `When only the web address exists then only the official visit web address is returned`() {
      val departmentType = OFFICIAL_VISIT
      createDBData(prisonId, departmentType, webAddress = webAddress)

      val responseSpec = doGetAction(endPoint, departmentType, createAnyRole())

      responseSpec.expectStatus().isOk

      val contactDetailsDto = getContactDetailsDtoResults(responseSpec.expectBody())

      assertNotNull(contactDetailsDto)
      assertNull(contactDetailsDto.emailAddress)
      assertNull(contactDetailsDto.phoneNumber)
      assertEquals(contactDetailsDto.webAddress, webAddress)
    }

    @Test
    fun `When only the phone number exists then only the official visit phone number is returned`() {
      val departmentType = OFFICIAL_VISIT
      createDBData(prisonId, departmentType, phoneNumber = phoneNumber)

      val responseSpec = doGetAction(endPoint, departmentType, createAnyRole())

      responseSpec.expectStatus().isOk

      val contactDetailsDto = getContactDetailsDtoResults(responseSpec.expectBody())

      assertNotNull(contactDetailsDto)
      assertNull(contactDetailsDto.emailAddress)
      assertEquals(contactDetailsDto.phoneNumber, phoneNumber)
      assertNull(contactDetailsDto.webAddress)
    }

    @Test
    fun `When contact details cannot be found for prison, then appropriate error is show`() {
      val departmentType = OFFICIAL_VISIT

      val responseSpec = doGetAction(endPoint, departmentType, createAnyRole())

      responseSpec.expectStatus().isNotFound

      assertDeveloperMessage(responseSpec, "Contact details not found for BRI / official visit department.")
    }

    @Test
    fun `When no role is give to get phone for given type, status unauthorized is returned`() {
      val responseSpec = doGetActionNoRole(endPoint)

      responseSpec.expectStatus().isUnauthorized
    }
  }
}
