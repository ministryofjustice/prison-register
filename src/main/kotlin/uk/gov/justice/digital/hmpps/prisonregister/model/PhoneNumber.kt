package uk.gov.justice.digital.hmpps.prisonregister.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table

@Entity
@Table(name = "PHONE_NUMBER")
data class PhoneNumber(
  @Column(name = "VALUE", nullable = false)
  var value: String,

) : AbstractIdEntity()
