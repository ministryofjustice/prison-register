package uk.gov.justice.digital.hmpps.prisonregister.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table

@Entity
@Table(name = "area")
class Area(
  @Id
  @Column(unique = true)
  var code: String,
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "parent")
  var region: Region?,
  var description: String,
  @Suppress("unused") var prisonArea: Boolean = false,
  var active: Boolean = false,

)
