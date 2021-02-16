package uk.gov.justice.digital.hmpps.prisonregister.model

import org.hibernate.annotations.GenericGenerator
import org.hibernate.annotations.Parameter
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.Id
import javax.persistence.OneToOne
import javax.persistence.PrimaryKeyJoinColumn

@Entity
data class OffenderManagementUnit(
  @OneToOne(optional = false)
  @PrimaryKeyJoinColumn
  var prison: Prison,

  var emailAddress: String
) {
  @Id
  @GeneratedValue(generator = "omuKeyGenerator")
  @GenericGenerator(
    name = "omuKeyGenerator",
    strategy = "foreign",
    parameters = [Parameter(name = "property", value = "prison")]
  )
  private var prisonId: String? = null
}
