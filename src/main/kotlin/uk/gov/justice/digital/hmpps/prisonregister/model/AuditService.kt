package uk.gov.justice.digital.hmpps.prisonregister.model

import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.prisonregister.config.SecurityUserContext
import java.time.Instant

@Service
class AuditService(
  private val auditApiService: AuditApiService,
  @Value("\${spring.application.name}")
  private val serviceName: String,
  private val securityUserContext: SecurityUserContext,
  private val mapper: ObjectMapper
) {

  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  fun sendAuditEvent(what: String, details: Any, occurredAt: Instant) {
    val auditEvent = AuditEvent(
      what = what,
      `when` = occurredAt,
      who = securityUserContext.principal,
      service = serviceName,
      details = mapper.writeValueAsString(details)
    )
    log.debug("Sending event $auditEvent")
    auditApiService.auditPrisonEvent(auditEvent)
  }
}

data class AuditEvent(
  val what: String,
  val `when`: Instant = Instant.now(),
  val who: String,
  val service: String,
  val details: String? = null,
)
enum class AuditType {
  PRISON_REGISTER_UPDATE
}
