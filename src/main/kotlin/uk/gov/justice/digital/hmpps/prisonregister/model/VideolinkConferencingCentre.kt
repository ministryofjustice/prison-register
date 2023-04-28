package uk.gov.justice.digital.hmpps.prisonregister.model

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.Id
import jakarta.persistence.OneToOne
import jakarta.persistence.PrimaryKeyJoinColumn
import org.hibernate.annotations.GenericGenerator
import org.hibernate.annotations.Parameter

@Entity
data class VideolinkConferencingCentre(
  @OneToOne(optional = false)
  @PrimaryKeyJoinColumn
  var prison: Prison,

  var emailAddress: String,
) {
  @Id
  @GeneratedValue(generator = "omuKeyGenerator")
  @GenericGenerator(
    name = "omuKeyGenerator",
    strategy = "foreign",
    parameters = [Parameter(name = "property", value = "prison")],
  )
  private var prisonId: String? = null
}
