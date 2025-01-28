package uk.gov.justice.digital.hmpps.prisonregister.resource.validator

import jakarta.validation.Constraint
import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext
import jakarta.validation.Payload
import org.apache.commons.validator.routines.UrlValidator
import kotlin.reflect.KClass

@Target(AnnotationTarget.FIELD, AnnotationTarget.VALUE_PARAMETER)
@MustBeDocumented
@Constraint(validatedBy = [WebAddressValidator::class])
annotation class ValidWebAddress(
  val message: String = "Web address is in an incorrect format",
  val groups: Array<KClass<*>> = [],
  val payload: Array<KClass<out Payload>> = [],
)

class WebAddressValidator : ConstraintValidator<ValidWebAddress, String> {
  override fun isValid(value: String?, context: ConstraintValidatorContext?): Boolean {
    if (value == null) return true
    if (value.isBlank()) return false
    return this.isValidWebAddress(value)
  }
  private fun isValidWebAddress(value: String): Boolean {
    val schemes = arrayOf("http", "https")
    val validator = UrlValidator(schemes)
    return validator.isValid(value) || validator.isValid("http://" + value)
  }
}
