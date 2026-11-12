package uk.gov.justice.digital.hmpps.prisonregister.model

import org.springframework.data.jpa.domain.Specification
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.stereotype.Repository

@Repository
interface ApprovedPremisesRepository :
  JpaRepository<ApprovedPremises, String>,
  JpaSpecificationExecutor<ApprovedPremises> {
  // Loads everything the /api/agencies response needs except emails and phones in one query (MAPB-1127).
  // Only one list association can be join fetched, so emails and phones load lazily.
  @EntityGraph(attributePaths = ["area", "region", "addresses"])
  fun findWithDetailsByApprovedPremisesId(approvedPremisesId: String): ApprovedPremises?

  override fun findAll(spec: Specification<ApprovedPremises>): List<ApprovedPremises>
}
