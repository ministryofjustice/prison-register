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
@Constraint(validatedBy = [PhoneNumberValidator::class])
annotation class ValidPhoneNumber(
  val message: String = "Phone number is in an incorrect format",
  val groups: Array<KClass<*>> = [],
  val payload: Array<KClass<out Payload>> = [],
)

class PhoneNumberValidator : ConstraintValidator<ValidPhoneNumber, String> {
  override fun isValid(value: String?, context: ConstraintValidatorContext?): Boolean {
    if (value == null) return true
    if (value.isBlank()) return false
    return this.isValidPhoneNumber(value)
  }
  private fun isValidPhoneNumber(value: String): Boolean = try {
    // If no + prefix defaults to uk
    val countryCode = if (value.startsWith("+")) "" else "GB"
    val numberUtil = PhoneNumberUtil.getInstance()
    val phoneNumber = numberUtil.parse(value, countryCode)
    numberUtil.isValidNumber(phoneNumber)
  } catch (e: NumberParseException) {
    false
  }
}
