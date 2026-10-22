package uk.gov.justice.digital.hmpps.prisonregister.model

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface EmailAddressRepository : JpaRepository<EmailAddress, Long> {
  fun findAllByValue(emailAddress: String): List<EmailAddress>
}
