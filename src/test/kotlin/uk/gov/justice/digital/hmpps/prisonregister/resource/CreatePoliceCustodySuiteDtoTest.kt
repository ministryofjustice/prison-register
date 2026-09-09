package uk.gov.justice.digital.hmpps.prisonregister.resource

import jakarta.validation.ConstraintViolation
import jakarta.validation.Validation
import jakarta.validation.Validator
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class CreatePoliceCustodySuiteDtoTest {

  private lateinit var validator: Validator

  private val valid = CreatePoliceCustodySuiteDto(
    policeCustodySuiteId = "NEWPCS",
    policeCustodySuiteName = "New Police Custody Suite",
    description = "The New Police Custody Suite",
    active = true,
    inactiveDate = null,
    cjitCode = "123456789",
    areaCode = "52",
    regionCode = "YOHUM",
    geographicalAreaCode = "WYORKS",
    localAuthorityCode = "00CG",
    payrollRegionCode = "NEY",
    addresses = listOf(
      UpdateAddressDto(
        addressLine1 = "Custody Suite, 31 High Street",
        addressLine2 = "City Centre",
        town = "Sheffield",
        county = "South Yorkshire",
        postcode = "S1 3GG",
        country = "England",
      ),
    ),
    emailAddresses = listOf(
      UpdateEmailAddressDto(address = "test@justice.gov.uk"),
    ),
    phoneNumbers = listOf(
      UpdatePhoneNumberDto(number = "0114 555 8989"),
    ),
  )

  @BeforeEach
  fun setUp() {
    validator = Validation.buildDefaultValidatorFactory().validator
  }

  private fun messagesFor(dto: CreatePoliceCustodySuiteDto): Set<String> = validator.validate(dto).map(ConstraintViolation<CreatePoliceCustodySuiteDto>::getMessage).toSet()

  @Test
  fun `no violations when dto is valid`() {
    assertThat(validator.validate(valid)).isEmpty()
  }

  @Nested
  inner class PoliceCustodySuiteId {
    @Test
    fun `blank police custody suite id fails validation`() {
      assertThat(messagesFor(valid.copy(policeCustodySuiteId = ""))).contains("Police Custody Suite id is required")
    }

    @Test
    fun `police custody suite id shorter than 2 characters fails validation`() {
      assertThat(messagesFor(valid.copy(policeCustodySuiteId = "A"))).contains("Police Custody Suite Id must be between 2 and 6 letters")
    }

    @Test
    fun `police custody suite id longer than 6 characters fails validation`() {
      assertThat(messagesFor(valid.copy(policeCustodySuiteId = "TOOLONG"))).contains("Police Custody Suite Id must be between 2 and 6 letters")
    }
  }

  @Nested
  inner class PoliceCustodySuiteName {
    @Test
    fun `blank police custody suite name fails validation`() {
      assertThat(messagesFor(valid.copy(policeCustodySuiteName = ""))).contains("Police Custody Suite name is required")
    }

    @Test
    fun `police custody suite name longer than 40 characters fails validation`() {
      assertThat(messagesFor(valid.copy(policeCustodySuiteName = "a".repeat(41))))
        .contains("Police Custody Suite name must be no more than 40 characters")
    }
  }

  @Nested
  inner class Description {
    @Test
    fun `null description passes validation`() {
      assertThat(validator.validate(valid.copy(description = null))).isEmpty()
    }

    @Test
    fun `description longer than 3000 characters fails validation`() {
      assertThat(messagesFor(valid.copy(description = "a".repeat(3001))))
        .contains("Description must be no more than 3000 characters")
    }
  }

  @Nested
  inner class CjitCode {
    @Test
    fun `null cjit code passes validation`() {
      assertThat(validator.validate(valid.copy(cjitCode = null))).isEmpty()
    }

    @Test
    fun `cjit code longer than 12 characters fails validation`() {
      assertThat(messagesFor(valid.copy(cjitCode = "a".repeat(13))))
        .contains("CJIT code must be no more than 12 characters")
    }
  }

  @Nested
  inner class AreaCode {
    @Test
    fun `null area code passes validation`() {
      assertThat(validator.validate(valid.copy(areaCode = null))).isEmpty()
    }

    @Test
    fun `area code longer than 12 characters fails validation`() {
      assertThat(messagesFor(valid.copy(areaCode = "a".repeat(13))))
        .contains("Area code must be no more than 12 characters")
    }
  }

  @Nested
  inner class RegionCode {
    @Test
    fun `null region code passes validation`() {
      assertThat(validator.validate(valid.copy(regionCode = null))).isEmpty()
    }

    @Test
    fun `region code longer than 12 characters fails validation`() {
      assertThat(messagesFor(valid.copy(regionCode = "a".repeat(13))))
        .contains("Region code must be no more than 12 characters")
    }
  }

  @Nested
  inner class GeographicalAreaCode {
    @Test
    fun `null geographical area code passes validation`() {
      assertThat(validator.validate(valid.copy(geographicalAreaCode = null))).isEmpty()
    }

    @Test
    fun `geographical area code longer than 12 characters fails validation`() {
      assertThat(messagesFor(valid.copy(geographicalAreaCode = "a".repeat(13))))
        .contains("Geographical area code must be no more than 12 characters")
    }
  }

  @Nested
  inner class Addresses {
    @Test
    fun `empty address list passes validation`() {
      assertThat(validator.validate(valid.copy(addresses = listOf()))).isEmpty()
    }

    @Test
    fun `cascades validation into nested address`() {
      assertThat(
        messagesFor(
          valid.copy(
            addresses = listOf(
              UpdateAddressDto(
                addressLine1 = "a".repeat(81),
                addressLine2 = null,
                town = "Sheffield",
                county = null,
                postcode = "S1 3GG",
                country = "England",
              ),
            ),
          ),
        ),
      ).contains("Address line 1 must be no more than 80 characters")
    }
  }

  @Nested
  inner class EmailAddresses {
    @Test
    fun `empty email address list passes validation`() {
      assertThat(validator.validate(valid.copy(emailAddresses = listOf()))).isEmpty()
    }

    @Test
    fun `cascades validation into nested email address that is blank`() {
      assertThat(messagesFor(valid.copy(emailAddresses = listOf(UpdateEmailAddressDto(address = "")))))
        .contains("Email address is required")
    }

    @Test
    fun `cascades validation into nested email address that is an incorrect format`() {
      assertThat(messagesFor(valid.copy(emailAddresses = listOf(UpdateEmailAddressDto(address = "not-an-email")))))
        .contains("Email address is in an incorrect format")
    }

    @Test
    fun `cascades validation into nested email address longer than 100 characters`() {
      assertThat(
        messagesFor(
          valid.copy(emailAddresses = listOf(UpdateEmailAddressDto(address = "a".repeat(95) + "@a.com"))),
        ),
      ).contains("Email address must be no more than 100 characters")
    }
  }

  @Nested
  inner class PhoneNumbers {
    @Test
    fun `empty phone number list passes validation`() {
      assertThat(validator.validate(valid.copy(phoneNumbers = listOf()))).isEmpty()
    }

    @Test
    fun `cascades validation into nested phone number that is blank`() {
      assertThat(messagesFor(valid.copy(phoneNumbers = listOf(UpdatePhoneNumberDto(number = "")))))
        .contains("Phone number is required")
    }

    @Test
    fun `cascades validation into nested phone number that is an invalid format`() {
      assertThat(messagesFor(valid.copy(phoneNumbers = listOf(UpdatePhoneNumberDto(number = "not-a-number")))))
        .isNotEmpty()
    }
  }
}
