package uk.gov.justice.digital.hmpps.prisonregister.model

import org.springframework.data.jpa.domain.Specification
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.stereotype.Repository

@Repository
interface OtherAgencyRepository :
  JpaRepository<OtherAgency, String>,
  JpaSpecificationExecutor<OtherAgency> {
  override fun findAll(spec: Specification<OtherAgency>): List<OtherAgency>

  fun findAllByActiveTrue(): List<OtherAgency>

  fun findAllByOtherAgencyType(otherAgencyType: OtherAgencyType): List<OtherAgency>

  fun findAllByOtherAgencyTypeAndActiveTrue(otherAgencyType: OtherAgencyType): List<OtherAgency>
}
