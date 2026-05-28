package uk.gov.justice.digital.hmpps.prisonregister.exceptions

import uk.gov.justice.digital.hmpps.prisonregister.model.Prison

class PrisonNotFoundException(
  val prisonId: String,
  message: String = "Unable to find ${Prison::class.java.name} with id $prisonId",
) : RuntimeException(message)
