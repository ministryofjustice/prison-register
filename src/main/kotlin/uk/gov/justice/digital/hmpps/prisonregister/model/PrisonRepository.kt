package uk.gov.justice.digital.hmpps.prisonregister.model

import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface PrisonRepository : CrudRepository<Prison, String> {
  fun findByActiveOrderByPrisonId(active: Boolean): List<Prison>
}
