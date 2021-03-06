package uk.gov.justice.digital.hmpps.prisonregister.model

import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.OneToOne

@Entity
data class Prison(
  @Id
  val prisonId: String,
  val name: String,
  val active: Boolean,
) {

  @OneToOne
  @JoinColumn(name = "prison_id")
  var gpPractice: PrisonGpPractice? = null
}
