package uk.gov.justice.digital.hmpps.prisonregister.model

import org.springframework.data.domain.Sort
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface PayrollRegionRepository : JpaRepository<PayrollRegion, String> {
  fun findAllByActiveTrue(sort: Sort): List<PayrollRegion>
}
