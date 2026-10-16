package uk.gov.justice.digital.hmpps.prisonregister.model

import org.springframework.data.domain.Sort
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface AreaRepository : JpaRepository<Area, String> {
  fun findAllByActiveTrue(sort: Sort): List<Area>

  fun findAllByRegionCode(region: String, sort: Sort): List<Area>

  fun findAllByRegionCodeAndActiveTrue(region: String, sort: Sort): List<Area>
}
