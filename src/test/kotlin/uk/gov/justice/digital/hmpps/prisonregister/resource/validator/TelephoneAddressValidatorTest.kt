package uk.gov.justice.digital.hmpps.prisonregister.resource.validator

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

/**
 * Test logic in the TelephoneAddressValidator class.
 */
class TelephoneAddressValidatorTest() {

  private val telephoneAddressValidator: TelephoneAddressValidator = TelephoneAddressValidator()

  @Test
  fun `valid numbers that should pass validation`() {
    // Given

    val phoneNumbers = arrayOf(
      "+44 01348811540",
      "+1 (415) 555‑0132",
      "07505902221",
      "(020) 3123 1234",
      "(01481) 123 123",
    )

    // When

    val results = phoneNumbers.map { telephoneAddressValidator.isValid(value = it, null) }

    // Then

    results.forEachIndexed { index, result ->
      Assertions.assertTrue(
        result,
        "Telephone Address is not valid, but it should be :${phoneNumbers[index]}",
      )
    }
  }

  @Test
  fun `in-valid numbers that should fail validation`() {
    // Given

    val phoneNumbers = arrayOf("", "a", "+44 as", "222", "+44", "@£", "aled@aled.com", "s", null)

    // When

    val results = phoneNumbers.map { telephoneAddressValidator.isValid(value = it, null) }

    // Then

    results.forEachIndexed { index, result ->
      Assertions.assertFalse(
        result,
        "Telephone Address is valid, but it should not be : ${phoneNumbers[index]}",
      )
    }
  }
}
