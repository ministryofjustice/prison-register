package uk.gov.justice.digital.hmpps.prisonregister.model

import org.springframework.data.jpa.domain.Specification
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.stereotype.Repository

@Repository
interface PoliceCustodySuiteRepository :
  JpaRepository<PoliceCustodySuite, String>,
  JpaSpecificationExecutor<PoliceCustodySuite> {
  override fun findAll(spec: Specification<PoliceCustodySuite>): List<PoliceCustodySuite>
}
