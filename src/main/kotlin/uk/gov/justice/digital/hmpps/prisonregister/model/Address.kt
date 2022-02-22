package uk.gov.justice.digital.hmpps.prisonregister.model

import java.util.UUID
import javax.persistence.Entity
import javax.persistence.FetchType
import javax.persistence.Id
import javax.persistence.ManyToOne

@Entity
data class Address(
  @Id
  val id: UUID,
  var addressLine1: String,
  var addressLine2: String?,
  var town: String?,
  var county: String,
  var postcode: String,
  var country: String,

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  var prison: Prison,
)
