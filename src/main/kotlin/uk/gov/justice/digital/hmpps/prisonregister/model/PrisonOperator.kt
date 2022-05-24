package uk.gov.justice.digital.hmpps.prisonregister.model

import javax.persistence.Embeddable
import javax.persistence.EmbeddedId
import javax.persistence.Entity
import javax.persistence.FetchType
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne

@Entity
data class PrisonOperator(
  @EmbeddedId
  val prisonOperatorId: PrisonOperatorId
)

@Embeddable
class PrisonOperatorId(
  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "PRISON_ID", nullable = false)
  val prison: Prison,

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "OPERATOR_ID", nullable = false)
  val operator: Operator
) : java.io.Serializable
