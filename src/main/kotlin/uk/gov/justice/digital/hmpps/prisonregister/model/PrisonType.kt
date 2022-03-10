package uk.gov.justice.digital.hmpps.prisonregister.model

import javax.persistence.Entity
import javax.persistence.EnumType
import javax.persistence.Enumerated
import javax.persistence.FetchType
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne

@Entity
data class PrisonType(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val id: Long = 0,

  @Enumerated(EnumType.STRING)
  var type: Type,

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "PRISON_ID", nullable = false)
  var prison: Prison,
)

enum class Type(val code: String, val description: String) {
  HMP("HMP", "Her Majesty’s Prison"),
  YOI("YOI", "Her Majesty’s Youth Offender Institution"),
  STC("STC", "Secure Training Centre"),
  IRC("IRC", "Immigration Removal Centre"),
}
