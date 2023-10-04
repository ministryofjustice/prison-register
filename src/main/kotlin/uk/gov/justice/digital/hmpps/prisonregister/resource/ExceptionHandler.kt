package uk.gov.justice.digital.hmpps.prisonregister.resource

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import uk.gov.justice.digital.hmpps.prisonregister.exceptions.BadContactPurposeTypeException

@RestControllerAdvice
class ExceptionHandler() {

  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  @ExceptionHandler(BadContactPurposeTypeException::class)
  fun handleAccessDeniedException(e: BadContactPurposeTypeException): ResponseEntity<String> {
    val message = "Value for ContactPurposeType is not of a know type ${e.contactPurposeType}."
    log.error(message)

    return ResponseEntity<String>(
      message,
      HttpStatus.BAD_REQUEST,
    )
  }
}
