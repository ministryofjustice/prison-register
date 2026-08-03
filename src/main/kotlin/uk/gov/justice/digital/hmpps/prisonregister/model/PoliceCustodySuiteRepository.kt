package uk.gov.justice.digital.hmpps.prisonregister.model

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface PoliceCustodySuiteRepository : JpaRepository<PoliceCustodySuite, String>
