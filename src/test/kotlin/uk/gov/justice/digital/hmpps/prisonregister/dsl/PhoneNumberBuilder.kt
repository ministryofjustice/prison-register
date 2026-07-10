package uk.gov.justice.digital.hmpps.prisonregister.dsl

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.prisonregister.model.PhoneNumber
import uk.gov.justice.digital.hmpps.prisonregister.model.PhoneNumberRepository

@DslMarker
annotation class PhoneNumberDslMarker

@PhoneNumberDslMarker
@Component
class PhoneNumberBuilder(val phoneNumberRepository: PhoneNumberRepository) {
  fun build(
    phoneNumber: String,
  ): PhoneNumber = phoneNumberRepository.saveAndFlush(
    PhoneNumber(
      value = phoneNumber,
    ),
  )
}
