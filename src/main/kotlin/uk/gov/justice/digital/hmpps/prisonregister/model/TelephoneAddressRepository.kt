package uk.gov.justice.digital.hmpps.prisonregister.model

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface TelephoneAddressRepository : JpaRepository<TelephoneAddress, Long> {

  @Modifying
  @Query(
    "DELETE FROM TelephoneAddress ta " +
      "WHERE ta.value = :telephoneAddress",
  )
  fun delete(telephoneAddress: String)

  @Query(
    "SELECT ta FROM TelephoneAddress ta WHERE ta.value=:telephoneAddress",
  )
  fun getTelephoneAddress(telephoneAddress: String): TelephoneAddress?
}
