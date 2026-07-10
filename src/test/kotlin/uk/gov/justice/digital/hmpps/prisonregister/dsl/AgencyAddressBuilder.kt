package uk.gov.justice.digital.hmpps.prisonregister.dsl

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.prisonregister.model.AgencyAddress
import uk.gov.justice.digital.hmpps.prisonregister.model.AgencyAddressRepository

@DslMarker
annotation class AgencyAddressDslMarker

@AgencyAddressDslMarker
@Component
class AgencyAddressBuilder(val agencyAddressRepository: AgencyAddressRepository) {
  fun build(
    addressLine1: String?,
    addressLine2: String?,
    town: String?,
    county: String?,
    postcode: String?,
    country: String?,
  ): AgencyAddress = agencyAddressRepository.saveAndFlush(
    AgencyAddress(
      addressLine1 = addressLine1,
      addressLine2 = addressLine2,
      town = town,
      county = county,
      postcode = postcode,
      country = country,
    ),
  )
}
