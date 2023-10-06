package uk.gov.justice.digital.hmpps.prisonregister.resource.validator

import com.google.i18n.phonenumbers.NumberParseException
import com.google.i18n.phonenumbers.PhoneNumberUtil
import jakarta.validation.Constraint
import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext
import jakarta.validation.Payload
import kotlin.reflect.KClass

@Target(AnnotationTarget.FIELD, AnnotationTarget.VALUE_PARAMETER)
@MustBeDocumented
@Constraint(validatedBy = [TelephoneAddressValidator::class])
annotation class ValidTelephoneAddress(
  val message: String = " telephone address is an incorrect format",
  val groups: Array<KClass<*>> = [],
  val payload: Array<KClass<out Payload>> = [],
)

class TelephoneAddressValidator : ConstraintValidator<ValidTelephoneAddress, String> {
  override fun isValid(value: String?, context: ConstraintValidatorContext?): Boolean {
    if (value.isNullOrBlank()) return false
    return this.isValidTelephoneAddress(value)
  }
  private fun isValidTelephoneAddress(value: String): Boolean {
    return try {
      // If no + prefix defaults to uk
      val countryCode = if (value.startsWith("+")) "" else "GB"
      val numberUtil = PhoneNumberUtil.getInstance()
      val phoneNumber = numberUtil.parse(value, countryCode)
      numberUtil.isValidNumber(phoneNumber)
    } catch (e: NumberParseException) {
      false
    }
  }
}
