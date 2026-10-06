package uk.gov.justice.digital.hmpps.prisonregister.service

import com.fasterxml.jackson.databind.ObjectMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import software.amazon.awssdk.services.sns.SnsAsyncClient
import software.amazon.awssdk.services.sns.model.PublishRequest
import uk.gov.justice.hmpps.sqs.HmppsQueueService
import uk.gov.justice.hmpps.sqs.HmppsTopic
import java.time.Instant

class SnsServiceTest {
  private val hmppsQueueService: HmppsQueueService = mock()
  private val snsClient: SnsAsyncClient = mock()
  private val topic = HmppsTopic("domainevents", "arn:aws:sns:eu-west-2:123456789012:domainevents", snsClient)
  private val objectMapper = ObjectMapper()
  private val snsService = SnsService(hmppsQueueService, objectMapper)

  @Test
  fun `publishes court event with the supplied attributes as JSON`() {
    whenever(hmppsQueueService.findByTopicId("domainevents")).thenReturn(topic)
    val occurredAt = Instant.parse("2026-10-06T13:30:00Z")

    snsService.sendCourtRegisterEmailInsertedEvent(courtId = "COURT1", emailId = 12345L, source = "TEST", occurredAt = occurredAt)

    val requestCaptor = argumentCaptor<PublishRequest>()
    verify(snsClient).publish(requestCaptor.capture())

    val message = objectMapper.readTree(requestCaptor.firstValue.message())
    assertThat(message["eventType"].asText()).isEqualTo("register.court.email.inserted")
    assertThat(message["additionalInformation"]["courtId"].asText()).isEqualTo("COURT1")
    assertThat(message["additionalInformation"]["source"].asText()).isEqualTo("TEST")
    assertThat(message["additionalInformation"]["emailId"].asLong()).isEqualTo(12345L)
    assertThat(message["occurredAt"].asText()).isEqualTo("2026-10-06T14:30:00+01:00")
  }
}
