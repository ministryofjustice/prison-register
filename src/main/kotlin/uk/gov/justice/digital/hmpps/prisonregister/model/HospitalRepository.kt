package uk.gov.justice.digital.hmpps.prisonregister.model

import org.springframework.data.jpa.domain.Specification
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.stereotype.Repository

@Repository
interface HospitalRepository :
  JpaRepository<Hospital, String>,
  JpaSpecificationExecutor<Hospital> {
  // Loads everything the /api/agencies response needs except emails and phones in one query (MAPB-1127).
  // Only one list association can be join fetched, so emails and phones load lazily.
  @EntityGraph(attributePaths = ["area", "region", "addresses"])
  fun findWithDetailsByHospitalId(hospitalId: String): Hospital?

  override fun findAll(spec: Specification<Hospital>): List<Hospital>

  fun findAllByActiveTrue(): List<Hospital>

  fun findAllByHighSecurity(highSecurity: Boolean): List<Hospital>

  fun findAllByHighSecurityAndActiveTrue(highSecurity: Boolean): List<Hospital>
}
