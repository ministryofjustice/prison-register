package uk.gov.justice.digital.hmpps.prisonregister.exceptions

import uk.gov.justice.digital.hmpps.prisonregister.model.DepartmentType

class ContactDetailsNotFoundException(val prisonId: String, val departmentType: DepartmentType) : RuntimeException()
