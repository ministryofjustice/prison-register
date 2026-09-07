package uk.gov.justice.digital.hmpps.prisonregister.exceptions

class EmailAddressAlreadyExistsException(val emailAddress: String) : RuntimeException()
