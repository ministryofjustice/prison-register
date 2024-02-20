package uk.gov.justice.digital.hmpps.prisonregister.model

import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import org.hibernate.Hibernate

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
) {

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false

    other as PrisonType
    return (type == other.type) && (prison.prisonId == other.prison.prisonId)
  }

  override fun hashCode(): Int = (prison.prisonId + type).hashCode()
}

enum class Type(val code: String, val description: String) {
  HMP("HMP", "His Majesty’s Prison"),
  YOI("YOI", "His Majesty’s Youth Offender Institution"),
  IRC("IRC", "Immigration Removal Centre"),
  STC("STC", "Secure Training Centre"),
  YCS("YCS", "Youth Custody Service"),
}
