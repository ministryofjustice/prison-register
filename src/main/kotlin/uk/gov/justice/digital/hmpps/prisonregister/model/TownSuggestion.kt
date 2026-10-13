package uk.gov.justice.digital.hmpps.prisonregister.model

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "town_suggestions")
class TownSuggestion(
  @Id
  val code: String,
  val description: String,
)
