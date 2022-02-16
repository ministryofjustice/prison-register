package uk.gov.justice.digital.hmpps.prisonregister.model

import com.amazonaws.services.sns.model.MessageAttributeValue
import com.amazonaws.services.sns.model.PublishRequest
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.hmpps.sqs.HmppsQueueService

@Service
class SnsService(hmppsQueueService: HmppsQueueService, private val objectMapper: ObjectMapper) {
  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }
  private val domaineventsTopic by lazy {
    hmppsQueueService.findByTopicId("domainevents")
      ?: throw RuntimeException("Topic with name domainevents doesn't exist")
  }

  fun sendEvent(eventType: EventType, id: String) {
    publishToDomainEventsTopic(RegisterChangeEvent(eventType, id))
  }

  private fun publishToDomainEventsTopic(hmppsEvent: RegisterChangeEvent) {
    log.debug("Event {} for id {}", hmppsEvent.eventType, hmppsEvent.id)
    domaineventsTopic.snsClient.publish(
      PublishRequest(domaineventsTopic.arn, objectMapper.writeValueAsString(hmppsEvent))
        .withMessageAttributes(
          mapOf("eventType" to MessageAttributeValue().withDataType("String").withStringValue(hmppsEvent.eventType.name))
        )
        .also { log.info("Published event $hmppsEvent to outbound topic") }
    )
  }
}

data class RegisterChangeEvent(
  val eventType: EventType,
  val id: String
)

enum class EventType {
  PRISON_REGISTER_UPDATE
}
