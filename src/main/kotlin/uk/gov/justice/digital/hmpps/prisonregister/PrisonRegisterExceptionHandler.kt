package uk.gov.justice.digital.hmpps.prisonregister

import jakarta.persistence.EntityNotFoundException
import jakarta.validation.ValidationException
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import uk.gov.justice.digital.hmpps.prisonregister.exceptions.ContactNotFoundException
import uk.gov.justice.digital.hmpps.prisonregister.exceptions.UnsupportedDepartmentTypeException

@RestControllerAdvice
class PrisonRegisterExceptionHandler {

  @ExceptionHandler(ContactNotFoundException::class)
  fun handleException(e: ContactNotFoundException): ResponseEntity<String> {
    val message = "Contact not found for prison ID ${e.prisonId} type ${e.departmentType.value}."
    log.error(message)

    return ResponseEntity<String>(
      message,
      HttpStatus.NOT_FOUND,
    )
  }

  @ExceptionHandler(UnsupportedDepartmentTypeException::class)
  fun handleException(e: UnsupportedDepartmentTypeException): ResponseEntity<String> {
    val message = "Value for DepartmentType is not of a known type ${e.departmentType}."
    log.error(message)

    return ResponseEntity<String>(
      message,
      HttpStatus.BAD_REQUEST,
    )
  }

  @ExceptionHandler(EntityNotFoundException::class)
  fun handleNotFoundException(e: Exception): ResponseEntity<ErrorResponse> {
    log.debug("Prison not found exception: {}", e.message)
    return ResponseEntity
      .status(HttpStatus.NOT_FOUND)
      .body(ErrorResponse(status = HttpStatus.NOT_FOUND, developerMessage = e.message))
  }

  @ExceptionHandler(ValidationException::class)
  fun handleValidationException(e: java.lang.Exception): ResponseEntity<ErrorResponse?>? {
    log.info("Validation exception: {}", e.message)
    return ResponseEntity
      .status(HttpStatus.BAD_REQUEST)
      .body(ErrorResponse(status = HttpStatus.BAD_REQUEST, developerMessage = e.message))
  }

  companion object {
    val log = LoggerFactory.getLogger(PrisonRegisterExceptionHandler::class.java)
  }
}

data class ErrorResponse(
  val status: Int,
  val errorCode: Int? = null,
  val userMessage: String? = null,
  val developerMessage: String? = null,
  val moreInfo: String? = null,
) {
  constructor(
    status: HttpStatus,
    errorCode: Int? = null,
    userMessage: String? = null,
    developerMessage: String? = null,
    moreInfo: String? = null,
  ) :
    this(status.value(), errorCode, userMessage, developerMessage, moreInfo)
}
