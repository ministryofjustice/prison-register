package uk.gov.justice.digital.hmpps.prisonregister.model

import org.springframework.data.jpa.domain.Specification
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.stereotype.Repository

@Repository
interface ApprovedPremisesRepository :
  JpaRepository<ApprovedPremises, String>,
  JpaSpecificationExecutor<ApprovedPremises> {
  override fun findAll(spec: Specification<ApprovedPremises>): List<ApprovedPremises>
}
