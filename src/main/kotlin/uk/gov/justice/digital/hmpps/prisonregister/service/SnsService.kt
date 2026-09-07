package uk.gov.justice.digital.hmpps.prisonregister.service

import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import software.amazon.awssdk.services.sns.model.MessageAttributeValue
import software.amazon.awssdk.services.sns.model.PublishRequest
import uk.gov.justice.hmpps.sqs.HmppsQueueService
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Service
class SnsService(hmppsQueueService: HmppsQueueService, private val objectMapper: ObjectMapper) {
  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  private val domaineventsTopic by lazy {
    hmppsQueueService.findByTopicId("domainevents")
      ?: throw RuntimeException("Topic with name domainevents doesn't exist")
  }
  private val domaineventsTopicClient by lazy { domaineventsTopic.snsClient }

  fun sendPrisonRegisterInsertedEvent(prisonId: String, occurredAt: Instant) {
    publishToDomainEventsTopic(
      HMPPSDomainEvent(
        "register.prison.inserted",
        AdditionalInformation(prisonId),
        occurredAt,
        "A prison has been inserted",
      ),
    )
  }

  fun sendPrisonRegisterAmendedEvent(prisonId: String, occurredAt: Instant) {
    publishToDomainEventsTopic(
      HMPPSDomainEvent(
        "register.prison.amended",
        AdditionalInformation(prisonId),
        occurredAt,
        "A prison has been updated",
      ),
    )
  }

  fun sendCourtRegisterAmendedEvent(courtId: String, occurredAt: Instant) {
    publishToDomainEventsTopic(
      HMPPSCourtDomainEvent(
        "register.court.amended",
        CourtAdditionalInformation(courtId),
        occurredAt,
        "A court has been updated",
      ),
    )
  }

  fun sendCourtRegisterInsertedEvent(courtId: String, occurredAt: Instant) {
    publishToDomainEventsTopic(
      HMPPSCourtDomainEvent(
        "register.court.inserted",
        CourtAdditionalInformation(courtId),
        occurredAt,
        "A court has been inserted",
      ),
    )
  }

  private fun publishToDomainEventsTopic(payload: HMPPSDomainEvent) {
    log.debug("Event {} for id {}", payload.eventType, payload.additionalInformation.prisonId)
    publish(payload.eventType, payload)
  }

  private fun publishToDomainEventsTopic(payload: HMPPSCourtDomainEvent) {
    log.debug("Event {} for id {}", payload.eventType, payload.additionalInformation.courtId)
    publish(payload.eventType, payload)
  }

  private fun publish(eventType: String, payload: Any) {
    domaineventsTopicClient.publish(
      PublishRequest.builder()
        .topicArn(domaineventsTopic.arn)
        .message(objectMapper.writeValueAsString(payload))
        .messageAttributes(
          mapOf(
            "eventType" to MessageAttributeValue.builder().dataType("String").stringValue(eventType).build(),
          ),
        )
        .build()
        .also { log.info("Published event $eventType to domainevents topic") },
    )
  }
}

data class AdditionalInformation(
  val prisonId: String,
)

data class CourtAdditionalInformation(
  val courtId: String,
)

data class HMPPSDomainEvent(
  val eventType: String,
  val additionalInformation: AdditionalInformation,
  val version: Int,
  val occurredAt: String,
  val description: String,
) {
  constructor(
    eventType: String,
    additionalInformation: AdditionalInformation,
    occurredAt: Instant,
    description: String,
  ) : this(
    eventType,
    additionalInformation,
    1,
    occurredAt.toOffsetDateFormat(),
    description,
  )
}

data class HMPPSCourtDomainEvent(
  val eventType: String,
  val additionalInformation: CourtAdditionalInformation,
  val version: Int,
  val occurredAt: String,
  val description: String,
) {
  constructor(
    eventType: String,
    additionalInformation: CourtAdditionalInformation,
    occurredAt: Instant,
    description: String,
  ) : this(
    eventType,
    additionalInformation,
    1,
    occurredAt.toOffsetDateFormat(),
    description,
  )
}
fun Instant.toOffsetDateFormat(): String = atZone(ZoneId.of("Europe/London")).toOffsetDateTime().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
