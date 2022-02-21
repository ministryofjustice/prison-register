package uk.gov.justice.digital.hmpps.prisonregister.service

import com.amazonaws.services.sns.AmazonSNSAsync
import com.amazonaws.services.sns.model.MessageAttributeValue
import com.amazonaws.services.sns.model.PublishRequest
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.hmpps.sqs.HmppsQueueService
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Service
class SnsService(hmppsQueueService: HmppsQueueService, private val objectMapper: ObjectMapper) {
  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  private val domaineventsTopic by lazy { hmppsQueueService.findByTopicId("domainevents") ?: throw RuntimeException("Topic with name domainevents doesn't exist") }
  private val domaineventsTopicClient by lazy { domaineventsTopic.snsClient as AmazonSNSAsync }

  fun sendPrisonRegisterAmendedEvent(prisonId: String, occurredAt: Instant) {
    publishToDomainEventsTopic(
      HMPPSDomainEvent(
        "register.prison.amended",
        AdditionalInformation(prisonId),
        occurredAt,
        "A prison has been updated"
      )
    )
  }

  private fun publishToDomainEventsTopic(payload: HMPPSDomainEvent) {
    log.debug("Event {} for id {}", payload.eventType, payload.additionalInformation.prisonId)
    domaineventsTopicClient.publishAsync(
      PublishRequest(domaineventsTopic.arn, objectMapper.writeValueAsString(payload))
        .withMessageAttributes(
          mapOf(
            "eventType" to MessageAttributeValue().withDataType("String").withStringValue(payload.eventType)
          )
        )
        .also { log.info("Published event $payload to outbound topic") }
    )
  }
}

data class AdditionalInformation(
  val prisonId: String
)

data class HMPPSDomainEvent(
  val eventType: String,
  val additionalInformation: AdditionalInformation,
  val version: Int,
  val occurredAt: String,
  val description: String
) {
  constructor(
    eventType: String,
    additionalInformation: AdditionalInformation,
    occurredAt: Instant,
    description: String
  ) : this(
    eventType,
    additionalInformation,
    1,
    occurredAt.toOffsetDateFormat(),
    description
  )
}
fun Instant.toOffsetDateFormat(): String =
  atZone(ZoneId.of("Europe/London")).toOffsetDateTime().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
