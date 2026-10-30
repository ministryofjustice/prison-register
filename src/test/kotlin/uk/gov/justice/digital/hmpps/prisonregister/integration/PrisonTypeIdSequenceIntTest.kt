package uk.gov.justice.digital.hmpps.prisonregister.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.ClassPathResource
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.web.reactive.function.BodyInserters
import uk.gov.justice.digital.hmpps.prisonregister.model.Prison
import uk.gov.justice.digital.hmpps.prisonregister.model.PrisonRepository
import uk.gov.justice.digital.hmpps.prisonregister.model.PrisonType
import uk.gov.justice.digital.hmpps.prisonregister.model.Type
import uk.gov.justice.digital.hmpps.prisonregister.resource.PrisonDto
import uk.gov.justice.digital.hmpps.prisonregister.resource.UpdatePrisonDto
import uk.gov.justice.digital.hmpps.prisonregister.service.AuditService
import javax.sql.DataSource

class PrisonTypeIdSequenceIntTest : IntegrationTestBase() {

  @Autowired
  private lateinit var prisonRepository: PrisonRepository

  @Autowired
  private lateinit var jdbcTemplate: JdbcTemplate

  @Autowired
  private lateinit var dataSource: DataSource

  @MockitoBean
  private lateinit var auditService: AuditService

  private val prisonId = "SEQ"
  private val sequence = "pg_get_serial_sequence('prison_type', 'id')"

  @BeforeEach
  fun setUpPrison() {
    val prison = Prison(prisonId = prisonId, name = "Sequence Prison", active = true, male = true, female = false, contracted = false)
    prison.prisonTypes = mutableSetOf(PrisonType(type = Type.YOI, prison = prison))
    prisonRepository.save(prison)
  }

  @AfterEach
  fun removePrison() {
    prisonRepository.deleteById(prisonId)
  }

  @Test
  fun `prison types can be changed again once the id sequence has been reset`() {
    jdbcTemplate.queryForObject("select setval($sequence, 1)", Long::class.java)

    changeTypeToYcs().expectStatus().is5xxServerError

    resetPrisonTypeIdSequence()

    val prison = changeTypeToYcs().expectStatus().isOk.expectBody(PrisonDto::class.java).returnResult().responseBody!!
    assertThat(prison.types.map { it.code }).containsExactly(Type.YCS)
  }

  @Test
  fun `resetting the id sequence never moves it backwards`() {
    val ahead = jdbcTemplate.queryForObject("select setval($sequence, (select max(id) from prison_type) + 1000)", Long::class.java)

    resetPrisonTypeIdSequence()

    assertThat(jdbcTemplate.queryForObject("select pg_sequence_last_value($sequence::regclass)", Long::class.java)).isEqualTo(ahead)
  }

  private fun changeTypeToYcs() = webTestClient.put()
    .uri("/prison-maintenance/id/$prisonId")
    .headers(setAuthorisation(roles = listOf("ROLE_MAINTAIN_REF_DATA"), scopes = listOf("write")))
    .body(BodyInserters.fromValue(UpdatePrisonDto("Sequence Prison", active = true, male = true, prisonTypes = setOf(Type.YCS))))
    .exchange()

  private fun resetPrisonTypeIdSequence() = ResourceDatabasePopulator(ClassPathResource("db/migration/V1_66__reset_prison_type_id_sequence.sql")).execute(dataSource)
}
