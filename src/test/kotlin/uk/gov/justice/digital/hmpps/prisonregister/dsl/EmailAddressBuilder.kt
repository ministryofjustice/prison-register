package uk.gov.justice.digital.hmpps.prisonregister.dsl

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.prisonregister.model.EmailAddress
import uk.gov.justice.digital.hmpps.prisonregister.model.EmailAddressRepository

@DslMarker
annotation class EmailAddressDslMarker

@EmailAddressDslMarker
@Component
class EmailAddressBuilder(val emailAddressRepository: EmailAddressRepository) {
  fun build(
    emailAddress: String,
  ): EmailAddress = emailAddressRepository.saveAndFlush(
    EmailAddress(
      value = emailAddress,
    ),
  )
}
