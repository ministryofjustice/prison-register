package uk.gov.justice.digital.hmpps.prisonregister.model

import org.springframework.data.jpa.domain.Specification
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.stereotype.Repository

@Repository
interface HospitalRepository :
  JpaRepository<Hospital, String>,
  JpaSpecificationExecutor<Hospital> {
  override fun findAll(spec: Specification<Hospital>): List<Hospital>
}
