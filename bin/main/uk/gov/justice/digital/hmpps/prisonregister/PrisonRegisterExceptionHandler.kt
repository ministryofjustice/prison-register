package uk.gov.justice.digital.hmpps.prisonregister

import jakarta.persistence.EntityNotFoundException
import jakarta.validation.ValidationException
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import uk.gov.justice.digital.hmpps.prisonregister.exceptions.ContactDetailsAlreadyExistException
import uk.gov.justice.digital.hmpps.prisonregister.exceptions.ContactDetailsNotFoundException
import uk.gov.justice.digital.hmpps.prisonregister.exceptions.ItemNotFoundException
import uk.gov.justice.digital.hmpps.prisonregister.exceptions.PrisonNotFoundException

@RestControllerAdvice
class PrisonRegisterExceptionHandler {

  @ExceptionHandler(ItemNotFoundException::class)
  fun handleException(e: ItemNotFoundException): ResponseEntity<ErrorResponse> {
    log.error(e.message)

    return ResponseEntity
      .status(HttpStatus.NOT_FOUND)
      .body(ErrorResponse(status = HttpStatus.NOT_FOUND, developerMessage = e.message))
  }

  @ExceptionHandler(MethodArgumentNotValidException::class)
  fun handleException(e: MethodArgumentNotValidException): ResponseEntity<ErrorResponse> {
    val message = e.allErrors.filterNotNull().map { it.defaultMessage!!.replaceFirstChar { it.uppercase() } }.sorted().joinToString(separator = ", ")
    log.error(message)
    return ResponseEntity
      .status(HttpStatus.BAD_REQUEST)
      .body(ErrorResponse(status = HttpStatus.NOT_FOUND, developerMessage = e.message, userMessage = message))
  }

  @ExceptionHandler(ContactDetailsAlreadyExistException::class)
  fun handleException(e: ContactDetailsAlreadyExistException): ResponseEntity<ErrorResponse> {
    val message = "Contact details already exist for ${e.prisonId} / ${e.departmentType.toMessage()} department."
    log.error(message)
    return ResponseEntity
      .status(HttpStatus.BAD_REQUEST)
      .body(ErrorResponse(status = HttpStatus.BAD_REQUEST, developerMessage = message, userMessage = message))
  }

  @ExceptionHandler(ContactDetailsNotFoundException::class)
  fun handleException(e: ContactDetailsNotFoundException): ResponseEntity<ErrorResponse> {
    val message = "Contact details not found for ${e.prisonId} / ${e.departmentType.toMessage()} department."
    log.error(message)

    return ResponseEntity
      .status(HttpStatus.NOT_FOUND)
      .body(ErrorResponse(status = HttpStatus.NOT_FOUND, developerMessage = message, userMessage = message))
  }

  @ExceptionHandler(EntityNotFoundException::class)
  fun handleNotFoundException(e: Exception): ResponseEntity<ErrorResponse> {
    log.debug("Prison not found exception: {}", e.message)
    return ResponseEntity
      .status(HttpStatus.NOT_FOUND)
      .body(ErrorResponse(status = HttpStatus.NOT_FOUND, developerMessage = e.message, userMessage = "Prison not found exception"))
  }

  @ExceptionHandler(PrisonNotFoundException::class)
  fun handleException(e: PrisonNotFoundException): ResponseEntity<ErrorResponse> {
    val message = "Prison not found exception: ${e.prisonId}"
    log.debug(message)
    return ResponseEntity
      .status(HttpStatus.NOT_FOUND)
      .body(ErrorResponse(status = HttpStatus.NOT_FOUND, developerMessage = e.message, userMessage = message))
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
