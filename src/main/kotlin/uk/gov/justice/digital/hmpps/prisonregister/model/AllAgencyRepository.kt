package uk.gov.justice.digital.hmpps.prisonregister.model

import org.springframework.data.repository.Repository

interface AllAgencyRepository : Repository<AllAgency, String> {
  fun findFirstByAgencyId(agencyId: String): AllAgency?
}
