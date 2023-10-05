package uk.gov.justice.digital.hmpps.prisonregister.integration

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.SpyBean
import org.springframework.http.HttpHeaders
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.reactive.server.WebTestClient
import software.amazon.awssdk.services.sqs.model.PurgeQueueRequest
import uk.gov.justice.digital.hmpps.prisonregister.model.ContactDetailsRepository
import uk.gov.justice.digital.hmpps.prisonregister.model.EmailAddressRepository
import uk.gov.justice.digital.hmpps.prisonregister.utilities.JwtAuthHelper
import uk.gov.justice.digital.hmpps.prisonregister.utilities.TestEmailAddressRepository
import uk.gov.justice.digital.hmpps.visitscheduler.integration.container.LocalStackContainer
import uk.gov.justice.digital.hmpps.visitscheduler.integration.container.PostgresContainer
import uk.gov.justice.hmpps.sqs.HmppsQueueService
import uk.gov.justice.hmpps.sqs.HmppsSqsProperties
import uk.gov.justice.hmpps.sqs.MissingTopicException
import uk.gov.justice.hmpps.sqs.countMessagesOnQueue

@Suppress("SpringJavaInjectionPointsAutowiringInspection")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
abstract class IntegrationTest {

  data class HMPPSEventType(val Value: String, val Type: String)
  data class HMPPSMessageAttributes(val eventType: HMPPSEventType)
  data class HMPPSMessage(
    val Message: String,
    val MessageId: String,
    val MessageAttributes: HMPPSMessageAttributes,
  )

  @SpyBean
  lateinit var contactDetailsRepository: ContactDetailsRepository

  @SpyBean
  lateinit var emailAddressRepository: EmailAddressRepository

  @SpyBean
  lateinit var testEmailAddressRepository: TestEmailAddressRepository

  @Suppress("unused")
  @Autowired
  lateinit var webTestClient: WebTestClient

  @Autowired
  protected lateinit var jwtAuthHelper: JwtAuthHelper

  @SpyBean
  protected lateinit var hmppsSqsPropertiesSpy: HmppsSqsProperties

  @Autowired
  protected lateinit var hmppsQueueService: HmppsQueueService

  init {
    // Resolves an issue where Wiremock keeps previous sockets open from other tests causing connection resets
    System.setProperty("http.keepAlive", "false")
  }

  @BeforeEach
  fun `clear queues`() {
    testSqsClient.purgeQueue(PurgeQueueRequest.builder().queueUrl(testQueueUrl).build())
  }

  @AfterEach
  fun `clean up tests`() {
    contactDetailsRepository.deleteAll()
    contactDetailsRepository.flush()
    emailAddressRepository.deleteAll()
    contactDetailsRepository.flush()
  }

  fun HmppsSqsProperties.domaineventsTopicConfig() =
    topics["domainevents"] ?: throw MissingTopicException("domainevents has not been loaded from configuration properties")

  internal fun setAuthorisation(
    user: String = "prison-register-api-client",
    roles: List<String> = listOf(),
    scopes: List<String> = listOf(),
  ): (HttpHeaders) -> Unit = jwtAuthHelper.setAuthorisation(user, roles, scopes)

  internal val testQueue by lazy { hmppsQueueService.findByQueueId("domaineventstestqueue") ?: throw RuntimeException("Queue with name domaineventstestqueue doesn't exist") }
  internal val testSqsClient by lazy { testQueue.sqsClient }
  internal val testQueueUrl by lazy { testQueue.queueUrl }

  fun testQueueEventMessageCount(): Int? = testSqsClient.countMessagesOnQueue(testQueueUrl).get()

  companion object {
    private val pgContainer = PostgresContainer.instance
    private val lsContainer = LocalStackContainer.instance

    @JvmStatic
    @DynamicPropertySource
    fun properties(registry: DynamicPropertyRegistry) {
      pgContainer?.run {
        registry.add("spring.datasource.url", pgContainer::getJdbcUrl)
        registry.add("spring.datasource.username", pgContainer::getUsername)
        registry.add("spring.datasource.password", pgContainer::getPassword)
        registry.add("spring.datasource.placeholders.database_update_password", pgContainer::getPassword)
        registry.add("spring.datasource.placeholders.database_read_only_password", pgContainer::getPassword)
        registry.add("spring.flyway.url", pgContainer::getJdbcUrl)
        registry.add("spring.flyway.user", pgContainer::getUsername)
        registry.add("spring.flyway.password", pgContainer::getPassword)
      }
      lsContainer?.run {
        registry.add("hmpps.sqs.localstackUrl") { lsContainer.getEndpointOverride(org.testcontainers.containers.localstack.LocalStackContainer.Service.SNS) }
        registry.add("hmpps.sqs.region") { lsContainer.region }
      }
    }
  }
}
