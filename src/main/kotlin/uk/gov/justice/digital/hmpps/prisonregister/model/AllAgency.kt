package uk.gov.justice.digital.hmpps.prisonregister.model

import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.Immutable
import uk.gov.justice.digital.hmpps.prisonregister.resource.LegacyAgencyType

/**
 * Read-only mapping of the all_agencies view, which unions the ids of every agency type.
 */
@Entity
@Immutable
@Table(name = "all_agencies")
data class AllAgency(
  @Id
  val agencyId: String,
  val name: String,
  @Enumerated(EnumType.STRING)
  val legacyType: LegacyAgencyType,
)
