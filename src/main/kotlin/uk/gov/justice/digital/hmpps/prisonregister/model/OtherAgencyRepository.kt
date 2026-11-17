package uk.gov.justice.digital.hmpps.prisonregister.model

import org.springframework.data.jpa.domain.Specification
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface OtherAgencyRepository :
  JpaRepository<OtherAgency, String>,
  JpaSpecificationExecutor<OtherAgency> {
  // Loads everything the /api/agencies response needs except emails and phones in one query (MAPB-1127).
  // Only one list association can be join fetched, so emails and phones load lazily.
  @EntityGraph(attributePaths = ["area", "region", "addresses"])
  fun findWithDetailsByAgencyId(agencyId: String): OtherAgency?

  override fun findAll(spec: Specification<OtherAgency>): List<OtherAgency>

  fun findAllByActiveTrue(): List<OtherAgency>

  fun findAllByOtherAgencyType(otherAgencyType: OtherAgencyType): List<OtherAgency>

  fun findAllByOtherAgencyTypeAndActiveTrue(otherAgencyType: OtherAgencyType): List<OtherAgency>

  @Query(
    """
    select o.agencyId as agencyId, o.name as agencyName, o.active as active, o.otherAgencyType as agencyType
    from OtherAgency o
    where (:active is null or o.active = :active)
      and (:textSearch is null or upper(o.agencyId) like :textSearch
           or upper(o.name) like :textSearch or upper(o.description) like :textSearch)
      and (:#{#otherAgencyTypes.isEmpty()} = true or o.otherAgencyType in :otherAgencyTypes)
    order by o.agencyId
    """,
  )
  fun findAllSummaries(active: Boolean?, textSearch: String?, otherAgencyTypes: List<OtherAgencyType>): List<OtherAgencySummary>
}

interface OtherAgencySummary {
  val agencyId: String
  val agencyName: String
  val active: Boolean
  val agencyType: OtherAgencyType
}
