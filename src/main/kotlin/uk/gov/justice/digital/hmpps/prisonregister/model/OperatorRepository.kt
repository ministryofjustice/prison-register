package uk.gov.justice.digital.hmpps.prisonregister.model

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface OperatorRepository : JpaRepository<Operator, Long> {
  fun findByName(name: String): Operator?
}
