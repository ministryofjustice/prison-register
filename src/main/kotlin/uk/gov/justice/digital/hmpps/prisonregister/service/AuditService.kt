package uk.gov.justice.digital.hmpps.prisonregister.service

import com.amazonaws.services.sqs.model.SendMessageRequest
import com.fasterxml.jackson.databind.ObjectMapper
import com.microsoft.applicationinsights.TelemetryClient
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.prisonregister.config.SecurityUserContext
import uk.gov.justice.hmpps.sqs.HmppsQueue
import uk.gov.justice.hmpps.sqs.HmppsQueueService
import java.time.Instant

@Service
class AuditService(
  @Value("\${spring.application.name}")
  private val serviceName: String,
  private val hmppsQueueService: HmppsQueueService,
  private val telemetryClient: TelemetryClient,
  private val objectMapper: ObjectMapper,
  private val securityUserContext: SecurityUserContext,
) {
  private val auditQueue by lazy { hmppsQueueService.findByQueueId("audit") as HmppsQueue }
  private val auditSqsClient by lazy { auditQueue.sqsClient }
  private val auditQueueUrl by lazy { auditQueue.queueUrl }

  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  fun sendAuditEvent(auditType: String, details: Any, occurredAt: Instant) {
    val auditEvent = AuditEvent(
      what = auditType,
      `when` = occurredAt,
      who = securityUserContext.principal,
      service = serviceName,
      details = objectMapper.writeValueAsString(details),
    )
    log.debug("Audit {} ", auditEvent)

    val result =
      auditSqsClient.sendMessage(
        SendMessageRequest(
          auditQueueUrl,
          auditEvent.toJson(),
        ),
      )

    telemetryClient.trackEvent(
      auditEvent.what,
      mapOf("messageId" to result.messageId),
      null,
    )
  }

  private fun Any.toJson() = objectMapper.writeValueAsString(this)
}

data class AuditEvent(
  val what: String,
  val `when`: Instant = Instant.now(),
  val who: String,
  val service: String,
  val details: String? = null,
)
enum class AuditType {
  PRISON_REGISTER_INSERT,
  PRISON_REGISTER_UPDATE,
  PRISON_REGISTER_ADDRESS_UPDATE,
  PRISON_REGISTER_ADDRESS_INSERT,
  PRISON_REGISTER_ADDRESS_DELETE,
}
