package uk.gov.justice.digital.hmpps.prisonregister.model

import org.springframework.data.repository.CrudRepository

interface OffenderManagementUnitRepository : CrudRepository<OffenderManagementUnit, String>
