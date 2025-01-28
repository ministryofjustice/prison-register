package uk.gov.justice.digital.hmpps.prisonregister.model

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface PhoneNumberRepository : JpaRepository<PhoneNumber, Long> {

  @Modifying
  @Query(
    "DELETE FROM PhoneNumber pn " +
      "WHERE pn.value = :phoneNumber",
  )
  fun delete(phoneNumber: String)

  fun getByValue(phoneNumber: String): PhoneNumber?
}
