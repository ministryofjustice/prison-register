package uk.gov.justice.digital.hmpps.prisonregister.model

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface WebAddressRepository : JpaRepository<WebAddress, Long> {
  fun findAllByValue(webAddress: String): List<WebAddress>
}
