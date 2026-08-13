package uk.gov.justice.digital.hmpps.prisonregister.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "local_authority")
class LocalAuthority(
  @Id
  @Column
  var code: String,
  var description: String,
  var active: Boolean = false,

)
