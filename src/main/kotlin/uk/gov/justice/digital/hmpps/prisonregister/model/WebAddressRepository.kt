package uk.gov.justice.digital.hmpps.prisonregister.model

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface WebAddressRepository : JpaRepository<WebAddress, Long> {

  @Modifying
  @Query(
    "DELETE FROM WebAddress wa " +
      "WHERE wa.value = :webAddress",
  )
  fun delete(webAddress: String)

  fun getByValue(webAddress: String): WebAddress?
}
