package uk.gov.justice.digital.hmpps.prisonregister.model

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "county_suggestions")
class CountySuggestion(
  @Id
  val code: String,
  val description: String,
)
