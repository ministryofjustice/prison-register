package uk.gov.justice.digital.hmpps.prisonregister.dsl

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.prisonregister.model.EmailAddressRepository

@DslMarker
annotation class EmailAddressDslMarker

@EmailAddressDslMarker
@Component
class EmailAddress(val emailAddressRepository: EmailAddressRepository) {
  fun build(
    emailAddress: String,
  ): uk.gov.justice.digital.hmpps.prisonregister.model.EmailAddress = emailAddressRepository.saveAndFlush(
    uk.gov.justice.digital.hmpps.prisonregister.model.EmailAddress(
      value = emailAddress,
    ),
  )
}
