package uk.gov.justice.digital.hmpps.prisonregister.model

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface EmailAddressRepository : JpaRepository<EmailAddress, Long> {

  @Modifying
  @Query(
    "DELETE FROM EmailAddress ea " +
      "WHERE ea.value = :emailAddress",
  )
  fun delete(emailAddress: String)

  @Query(
    "SELECT ea FROM EmailAddress ea WHERE ea.value=:emailAddress",
  )
  fun getEmailAddress(emailAddress: String): EmailAddress?
}
