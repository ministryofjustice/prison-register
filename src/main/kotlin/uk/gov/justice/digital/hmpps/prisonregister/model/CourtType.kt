@file:Suppress("unused")

package uk.gov.justice.digital.hmpps.prisonregister.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "court_type")
class CourtType(
  @Id
  @Column(unique = true)
  var code: String,
  var description: String,
  var active: Boolean = false,
) {
  companion object {
    enum class Type(val code: String, val description: String) {
      CACD("CACD", "Court of Appeal - Criminal Division"),
      CB("CB", "Combined Court"),
      CC("CC", "Crown Court"),
      CO("CO", "County Court"),
      DCM("DCM", "District Court Marshal"),
      GCM("GCM", "General Court Marshal"),
      IMM("IMM", "Immigration Court"),
      MC("MC", "Magistrates Court"),
      OTHER("OTHER", "Courts Outside England and Wales"),
      YC("YC", "Youth Court"),
    }
  }
}
