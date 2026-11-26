package uk.gov.justice.digital.hmpps.prisonregister.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table

@Entity
@Table(name = "WEB_ADDRESS")
data class WebAddress(
  @Column(name = "VALUE", nullable = false)
  val value: String,

) : AbstractIdEntity()
