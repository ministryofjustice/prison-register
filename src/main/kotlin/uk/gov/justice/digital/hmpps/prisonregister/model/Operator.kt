package uk.gov.justice.digital.hmpps.prisonregister.model

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import org.hibernate.Hibernate

@Entity
data class Operator(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val id: Long = 0,

  val name: String,
) {

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false

    other as Operator
    return (name == other.name)
  }

  override fun hashCode(): Int = name.hashCode()
}
