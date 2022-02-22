package uk.gov.justice.digital.hmpps.prisonregister.model

import java.util.UUID
import javax.persistence.Entity
import javax.persistence.FetchType
import javax.persistence.Id
import javax.persistence.ManyToOne

@Entity
data class PrisonType(
  @Id
  val id: UUID,
  var type: Type,

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  var prison: Prison,
)

enum class Type(val code: String, val description: String) {
  HMP("HMP", "Adult prison"),
  YOI("YOI", "Youth Offenders Institute"),
  STC("STC", "Secure Training Centre"),
  IRC("IRC", "Immigration Removal Centre"),
}
