package uk.gov.justice.digital.hmpps.prisonregister.exceptions

class PhoneNumberAlreadyExistsException(val phoneNumber: String) : RuntimeException()
