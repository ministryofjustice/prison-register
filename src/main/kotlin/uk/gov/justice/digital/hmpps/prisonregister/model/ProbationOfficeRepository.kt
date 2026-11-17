package uk.gov.justice.digital.hmpps.prisonregister.model

import org.springframework.data.jpa.domain.Specification
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface ProbationOfficeRepository :
  JpaRepository<ProbationOffice, String>,
  JpaSpecificationExecutor<ProbationOffice> {
  // Loads everything the /api/agencies response needs except emails and phones in one query (MAPB-1127).
  // Only one list association can be join fetched, so emails and phones load lazily.
  @EntityGraph(attributePaths = ["area", "region", "addresses"])
  fun findWithDetailsByProbationOfficeId(probationOfficeId: String): ProbationOffice?

  override fun findAll(spec: Specification<ProbationOffice>): List<ProbationOffice>

  fun findAllByActiveTrue(): List<ProbationOffice>

  @Query(
    """
    select p.probationOfficeId as probationOfficeId, p.name as probationOfficeName, p.active as active
    from ProbationOffice p
    where (:active is null or p.active = :active)
      and (:textSearch is null or upper(p.probationOfficeId) like :textSearch
           or upper(p.name) like :textSearch or upper(p.description) like :textSearch)
    order by p.probationOfficeId
    """,
  )
  fun findAllSummaries(active: Boolean?, textSearch: String?): List<ProbationOfficeSummary>
}

interface ProbationOfficeSummary {
  val probationOfficeId: String
  val probationOfficeName: String
  val active: Boolean
}
