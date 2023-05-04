package uk.gov.justice.digital.hmpps.prisonregister.model

import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne

@Entity
data class Address(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val id: Long? = null,
  var addressLine1: String? = null,
  var addressLine2: String? = null,
  var town: String,
  var county: String? = null,
  var postcode: String,
  var country: String,

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "PRISON_ID", nullable = false)
  var prison: Prison,
)
