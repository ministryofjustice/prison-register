package uk.gov.justice.digital.hmpps.prisonregister.model

import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface ContactDetailsRepository : JpaRepository<ContactDetails, Long> {

  @Suppress("FunctionName")
  @EntityGraph(value = "contact-entity-graph", type = EntityGraph.EntityGraphType.LOAD)
  fun getByPrison_PrisonIdAndType(prisonId: String, departmentType: DepartmentType): ContactDetails?
}
