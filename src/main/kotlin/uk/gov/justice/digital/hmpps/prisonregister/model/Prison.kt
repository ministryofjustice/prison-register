package uk.gov.justice.digital.hmpps.prisonregister.model

import java.time.LocalDate
import javax.persistence.CascadeType
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.OneToMany
import javax.persistence.OneToOne

@Entity
data class Prison(
  @Id
  val prisonId: String,
  var name: String,
  var description: String?,
  var active: Boolean,
  var gender: Gender,
  var inactiveDate: LocalDate?,

  @OneToMany(cascade = [CascadeType.ALL], mappedBy = "prison")
  var prisonTypes: List<PrisonType> = listOf(),

  @OneToMany(cascade = [CascadeType.ALL], mappedBy = "prison")
  var addresses: List<Address> = listOf(),
) {

  @OneToOne
  @JoinColumn(name = "prison_id")
  var gpPractice: PrisonGpPractice? = null
}

enum class Gender {
  MALE, FEMALE
}
