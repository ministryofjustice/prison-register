package uk.gov.justice.digital.hmpps.prisonregister.model

import javax.persistence.Entity
import javax.persistence.Id

@Entity
data class PrisonGpPractice(
  @Id
  val prisonId: String,
  val gpPracticeCode: String,
)
