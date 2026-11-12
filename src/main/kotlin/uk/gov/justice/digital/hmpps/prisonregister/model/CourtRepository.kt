package uk.gov.justice.digital.hmpps.prisonregister.model

import org.springframework.data.jpa.domain.Specification
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface CourtRepository :
  JpaRepository<Court, String>,
  JpaSpecificationExecutor<Court> {
  // Loads everything the /api/agencies response needs except emails and phones in one query (MAPB-1127).
  // Only one list association can be join fetched, so emails and phones load lazily.
  @EntityGraph(attributePaths = ["courtType", "area", "region", "addresses"])
  fun findWithDetailsByCourtId(courtId: String): Court?

  override fun findAll(spec: Specification<Court>): List<Court>

  @Query(
    """
    select c.courtId as courtId, c.name as courtName, c.active as active,
           ct.code as courtTypeCode, ct.description as courtTypeDescription
    from Court c
    join c.courtType ct
    where (:active is null or c.active = :active)
      and (:textSearch is null or upper(c.courtId) like :textSearch
           or upper(c.name) like :textSearch or upper(c.description) like :textSearch)
      and (:#{#courtTypeCodes.isEmpty()} = true or ct.code in :courtTypeCodes)
    order by c.courtId
    """,
  )
  fun findAllSummaries(active: Boolean?, textSearch: String?, courtTypeCodes: List<String>): List<CourtSummary>
}

interface CourtSummary {
  val courtId: String
  val courtName: String
  val active: Boolean
  val courtTypeCode: String
  val courtTypeDescription: String
}
