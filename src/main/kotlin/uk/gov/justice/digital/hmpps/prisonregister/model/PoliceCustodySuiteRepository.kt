package uk.gov.justice.digital.hmpps.prisonregister.model

import org.springframework.data.jpa.domain.Specification
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface PoliceCustodySuiteRepository :
  JpaRepository<PoliceCustodySuite, String>,
  JpaSpecificationExecutor<PoliceCustodySuite> {
  // Loads everything the /api/agencies response needs except emails and phones in one query (MAPB-1127).
  // Only one list association can be join fetched, so emails and phones load lazily.
  @EntityGraph(attributePaths = ["area", "region", "addresses"])
  fun findWithDetailsByPoliceCustodySuiteId(policeCustodySuiteId: String): PoliceCustodySuite?

  override fun findAll(spec: Specification<PoliceCustodySuite>): List<PoliceCustodySuite>

  fun findAllByActiveTrue(): List<PoliceCustodySuite>

  @Query(
    """
    select p.policeCustodySuiteId as policeCustodySuiteId, p.name as policeCustodySuiteName, p.active as active
    from PoliceCustodySuite p
    where (:active is null or p.active = :active)
      and (:textSearch is null or upper(p.policeCustodySuiteId) like :textSearch
           or upper(p.name) like :textSearch or upper(p.description) like :textSearch)
    order by p.policeCustodySuiteId
    """,
  )
  fun findAllSummaries(active: Boolean?, textSearch: String?): List<PoliceCustodySuiteSummary>
}

interface PoliceCustodySuiteSummary {
  val policeCustodySuiteId: String
  val policeCustodySuiteName: String
  val active: Boolean
}
