package uk.gov.justice.digital.hmpps.prisonregister.model

import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface CourtRepository : JpaRepository<Court, String> {
  @EntityGraph(value = "court-entity-graph", type = EntityGraph.EntityGraphType.LOAD)
  fun findByCourtId(courtId: String): Court
}
