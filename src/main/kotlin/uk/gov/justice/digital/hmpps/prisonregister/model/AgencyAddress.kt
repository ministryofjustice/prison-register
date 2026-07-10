package uk.gov.justice.digital.hmpps.prisonregister.model

import jakarta.persistence.Entity

@Entity
class AgencyAddress(
  var addressLine1: String? = null,
  var addressLine2: String? = null,
  var town: String? = null,
  var county: String? = null,
  var postcode: String? = null,
  var country: String? = null,
) : AbstractIdEntity()
