package uk.gov.justice.digital.hmpps.prisonregister.model

import jakarta.persistence.Entity
import jakarta.persistence.Id

@Entity
data class PrisonGpPractice(
  @Id
  val prisonId: String,
  val gpPracticeCode: String,
)
