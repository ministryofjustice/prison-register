package uk.gov.justice.digital.hmpps.prisonregister.model

import org.springframework.data.jpa.domain.Specification
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface PrisonRepository :
  JpaRepository<Prison, String>,
  JpaSpecificationExecutor<Prison> {

  @Query("FROM Prison p where (:active is null or p.active = :active) order by p.name")
  fun findByActiveOrderByPrisonName(active: Boolean?): List<Prison>

  fun findOneByGpPractice(gpPractice: String): Prison?

  @EntityGraph(value = "prison-entity-graph", type = EntityGraph.EntityGraphType.LOAD)
  fun findAllByPrisonIdIsIn(ids: List<String>): List<Prison>

  @EntityGraph(value = "prison-entity-graph", type = EntityGraph.EntityGraphType.LOAD)
  override fun findById(id: String): Optional<Prison>

  @EntityGraph(value = "prison-entity-graph", type = EntityGraph.EntityGraphType.LOAD)
  override fun findAll(spec: Specification<Prison>?): List<Prison>

  @EntityGraph(value = "prison-entity-graph", type = EntityGraph.EntityGraphType.LOAD)
  override fun findAll(): List<Prison>
}
