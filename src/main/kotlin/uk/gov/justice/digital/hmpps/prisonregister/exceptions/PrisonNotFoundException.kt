package uk.gov.justice.digital.hmpps.prisonregister.exceptions

class PrisonNotFoundException(val prisonId: String) : RuntimeException()
