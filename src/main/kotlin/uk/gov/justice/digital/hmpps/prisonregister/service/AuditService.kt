package uk.gov.justice.digital.hmpps.prisonregister.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.microsoft.applicationinsights.TelemetryClient
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import software.amazon.awssdk.services.sqs.model.SendMessageRequest
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
        SendMessageRequest.builder()
          .queueUrl(auditQueueUrl)
          .messageBody(auditEvent.toJson())
          .build(),
      ).get()

    telemetryClient.trackEvent(
      auditEvent.what,
      mapOf("messageId" to result.messageId()),
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
  COURT_REGISTER_UPDATE,
  COURT_REGISTER_ADDRESS_UPDATE,
  COURT_REGISTER_ADDRESS_INSERT,
  COURT_REGISTER_PHONE_UPDATE,
  COURT_REGISTER_PHONE_INSERT,
  COURT_REGISTER_EMAIL_UPDATE,
  COURT_REGISTER_EMAIL_INSERT,
  COURT_REGISTER_INSERT,
  COURT_REGISTER_DELETE,
  COURT_REGISTER_ADDRESS_DELETE,
  COURT_REGISTER_PHONE_DELETE,
  COURT_REGISTER_EMAIL_DELETE,
  POLICE_CUSTODY_SUITE_REGISTER_UPDATE,
  POLICE_CUSTODY_SUITE_REGISTER_ADDRESS_UPDATE,
  POLICE_CUSTODY_SUITE_REGISTER_ADDRESS_INSERT,
  POLICE_CUSTODY_SUITE_REGISTER_PHONE_UPDATE,
  POLICE_CUSTODY_SUITE_REGISTER_PHONE_INSERT,
  POLICE_CUSTODY_SUITE_REGISTER_EMAIL_UPDATE,
  POLICE_CUSTODY_SUITE_REGISTER_EMAIL_INSERT,
  POLICE_CUSTODY_SUITE_REGISTER_INSERT,
  POLICE_CUSTODY_SUITE_REGISTER_DELETE,
  POLICE_CUSTODY_SUITE_REGISTER_ADDRESS_DELETE,
  POLICE_CUSTODY_SUITE_REGISTER_PHONE_DELETE,
  POLICE_CUSTODY_SUITE_REGISTER_EMAIL_DELETE,
  HOSPITAL_REGISTER_INSERT,
  HOSPITAL_REGISTER_ADDRESS_INSERT,
  HOSPITAL_REGISTER_PHONE_INSERT,
  HOSPITAL_REGISTER_UPDATE,
  HOSPITAL_REGISTER_ADDRESS_UPDATE,
  HOSPITAL_REGISTER_PHONE_UPDATE,
  HOSPITAL_REGISTER_DELETE,
  HOSPITAL_REGISTER_ADDRESS_DELETE,
  HOSPITAL_REGISTER_PHONE_DELETE,
  PROBATION_OFFICE_REGISTER_INSERT,
  PROBATION_OFFICE_REGISTER_UPDATE,
  PROBATION_OFFICE_REGISTER_DELETE,
  PROBATION_OFFICE_REGISTER_ADDRESS_INSERT,
  PROBATION_OFFICE_REGISTER_ADDRESS_UPDATE,
  PROBATION_OFFICE_REGISTER_ADDRESS_DELETE,
  PROBATION_OFFICE_REGISTER_PHONE_INSERT,
  PROBATION_OFFICE_REGISTER_PHONE_UPDATE,
  PROBATION_OFFICE_REGISTER_PHONE_DELETE,
  PROBATION_OFFICE_REGISTER_EMAIL_INSERT,
  PROBATION_OFFICE_REGISTER_EMAIL_UPDATE,
  PROBATION_OFFICE_REGISTER_EMAIL_DELETE,
  APPROVED_PREMISE_REGISTER_INSERT,
  APPROVED_PREMISE_REGISTER_UPDATE,
  APPROVED_PREMISE_REGISTER_DELETE,
  APPROVED_PREMISE_REGISTER_ADDRESS_INSERT,
  APPROVED_PREMISE_REGISTER_ADDRESS_UPDATE,
  APPROVED_PREMISE_REGISTER_ADDRESS_DELETE,
  APPROVED_PREMISE_REGISTER_PHONE_INSERT,
  APPROVED_PREMISE_REGISTER_PHONE_UPDATE,
  APPROVED_PREMISE_REGISTER_PHONE_DELETE,
  APPROVED_PREMISE_REGISTER_EMAIL_INSERT,
  APPROVED_PREMISE_REGISTER_EMAIL_UPDATE,
  APPROVED_PREMISE_REGISTER_EMAIL_DELETE,
  AGENCY_REGISTER_INSERT,
  AGENCY_REGISTER_UPDATE,
  AGENCY_REGISTER_DELETE,
  AGENCY_REGISTER_ADDRESS_INSERT,
  AGENCY_REGISTER_ADDRESS_UPDATE,
  AGENCY_REGISTER_ADDRESS_DELETE,
  AGENCY_REGISTER_PHONE_INSERT,
  AGENCY_REGISTER_PHONE_UPDATE,
  AGENCY_REGISTER_PHONE_DELETE,
  AGENCY_REGISTER_EMAIL_INSERT,
  AGENCY_REGISTER_EMAIL_UPDATE,
  AGENCY_REGISTER_EMAIL_DELETE,
}
