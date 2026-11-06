package uk.gov.justice.digital.hmpps.prisonregister.model

import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface ContactDetailsRepository : JpaRepository<ContactDetails, Long> {

  @Suppress("FunctionName")
  @Query(
    "SELECT e.value FROM ContactDetails c join c.emailAddress e WHERE c.prison.prisonId=:prisonId AND c.type=:departmentType",
  )
  @EntityGraph(value = "contact-entity-graph", type = EntityGraph.EntityGraphType.LOAD)
  fun getByPrison_PrisonIdAndType(prisonId: String, departmentType: DepartmentType): ContactDetails?
}
