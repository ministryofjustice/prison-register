package uk.gov.justice.digital.hmpps.prisonregister.integration

import com.amazonaws.services.sqs.model.PurgeQueueRequest
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.SpyBean
import org.springframework.http.HttpHeaders
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.prisonregister.utilities.JwtAuthHelper
import uk.gov.justice.hmpps.sqs.HmppsQueueService
import uk.gov.justice.hmpps.sqs.HmppsSqsProperties
import uk.gov.justice.hmpps.sqs.MissingTopicException

@Suppress("SpringJavaInjectionPointsAutowiringInspection")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
abstract class IntegrationTest {
  @BeforeEach
  fun `clear queues`() {
    testSqsClient.purgeQueue(PurgeQueueRequest(testQueueUrl))
  }

  @Suppress("unused")
  @Autowired
  lateinit var webTestClient: WebTestClient

  @Autowired
  protected lateinit var jwtAuthHelper: JwtAuthHelper

  init {
    // Resolves an issue where Wiremock keeps previous sockets open from other tests causing connection resets
    System.setProperty("http.keepAlive", "false")
  }

  @SpyBean
  protected lateinit var hmppsSqsPropertiesSpy: HmppsSqsProperties
  @Autowired
  protected lateinit var hmppsQueueService: HmppsQueueService

  fun HmppsSqsProperties.domaineventsTopicConfig() =
    topics["domainevents"] ?: throw MissingTopicException("domainevents has not been loaded from configuration properties")

  internal fun setAuthorisation(
    user: String = "prison-register-api-client",
    roles: List<String> = listOf(),
    scopes: List<String> = listOf()
  ): (HttpHeaders) -> Unit = jwtAuthHelper.setAuthorisation(user, roles, scopes)

  internal val testQueue by lazy { hmppsQueueService.findByQueueId("domaineventstestqueue") ?: throw RuntimeException("Queue with name domaineventstestqueue doesn't exist") }
  internal val testSqsClient by lazy { testQueue.sqsClient }
  internal val testQueueUrl by lazy { testQueue.queueUrl }

  fun testQueueEventMessageCount(): Int? {
    val queueAttributes = testSqsClient.getQueueAttributes(testQueueUrl, listOf("ApproximateNumberOfMessages"))
    return queueAttributes.attributes["ApproximateNumberOfMessages"]?.toInt()
  }

  data class HMPPSEventType(val Value: String, val Type: String)
  data class HMPPSMessageAttributes(val eventType: HMPPSEventType)
  data class HMPPSMessage(
    val Message: String,
    val MessageId: String,
    val MessageAttributes: HMPPSMessageAttributes
  )
}
