package uk.gov.justice.digital.hmpps.prisonregister.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import org.hibernate.Hibernate

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
  @Column(name = "address_line1_in_welsh")
  var addressLine1InWelsh: String? = null,
  @Column(name = "address_line2_in_welsh")
  var addressLine2InWelsh: String? = null,
  var townInWelsh: String? = null,
  var countyInWelsh: String? = null,
  var countryInWelsh: String? = null,

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "PRISON_ID", nullable = false)
  var prison: Prison,
) {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false

    other as Address
    return (id == other.id)
  }

  override fun hashCode(): Int = id.hashCode()
}
