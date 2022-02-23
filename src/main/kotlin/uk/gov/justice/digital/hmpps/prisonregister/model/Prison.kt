package uk.gov.justice.digital.hmpps.prisonregister.model

import org.hibernate.Hibernate
import java.time.LocalDate
import javax.persistence.CascadeType
import javax.persistence.Entity
import javax.persistence.EnumType
import javax.persistence.Enumerated
import javax.persistence.FetchType
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.OneToMany
import javax.persistence.OneToOne

@Entity
data class Prison(
  @Id
  val prisonId: String,
  var name: String,
  var description: String? = null,
  var active: Boolean,

  @Enumerated(EnumType.STRING)
  var gender: Gender? = null,

  var inactiveDate: LocalDate? = null,

  @OneToMany(mappedBy = "prison", fetch = FetchType.LAZY, cascade = [CascadeType.ALL], orphanRemoval = true)
  var prisonTypes: List<PrisonType> = listOf(),

  @OneToMany(mappedBy = "prison", fetch = FetchType.LAZY, cascade = [CascadeType.ALL], orphanRemoval = true)
  var addresses: List<Address> = listOf(),
) {

  @OneToOne
  @JoinColumn(name = "prison_id")
  var gpPractice: PrisonGpPractice? = null

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
    other as Prison

    return prisonId != null && prisonId == other.prisonId
  }

  override fun hashCode(): Int = javaClass.hashCode()

  @Override
  override fun toString(): String {
    return this::class.simpleName + "(prisonId = $prisonId , name = $name , description = $description , active = $active , gender = $gender )"
  }
}

enum class Gender {
  MALE, FEMALE
}
