package uk.gov.justice.digital.hmpps.prisonregister.model

import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface PrisonRepository : CrudRepository<Prison, String>, JpaSpecificationExecutor<Prison> {
  fun findByActiveOrderByPrisonId(active: Boolean): List<Prison>

  fun findByGpPracticeGpPracticeCode(gpPracticeCode: String): Prison?
}
