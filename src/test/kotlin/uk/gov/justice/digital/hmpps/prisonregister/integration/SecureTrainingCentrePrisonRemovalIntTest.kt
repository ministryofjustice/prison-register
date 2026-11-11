package uk.gov.justice.digital.hmpps.prisonregister.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.ClassPathResource
import org.springframework.http.MediaType
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator
import uk.gov.justice.digital.hmpps.prisonregister.dsl.Root
import uk.gov.justice.digital.hmpps.prisonregister.model.DepartmentType
import uk.gov.justice.digital.hmpps.prisonregister.model.OtherAgencyRepository
import uk.gov.justice.digital.hmpps.prisonregister.model.OtherAgencyType
import uk.gov.justice.digital.hmpps.prisonregister.model.PrisonRepository
import uk.gov.justice.digital.hmpps.prisonregister.model.Type
import uk.gov.justice.digital.hmpps.prisonregister.resource.LegacyAgencyDto
import uk.gov.justice.digital.hmpps.prisonregister.resource.LegacyAgencyType
import javax.sql.DataSource

class SecureTrainingCentrePrisonRemovalIntTest : IntegrationTestBase() {

  @Autowired
  private lateinit var prisonRepository: PrisonRepository

  @Autowired
  private lateinit var otherAgencyRepository: OtherAgencyRepository

  @Autowired
  private lateinit var jdbcTemplate: JdbcTemplate

  @Autowired
  private lateinit var dataSource: DataSource

  @Autowired
  private lateinit var dsl: Root

  private val stcIds = listOf("STC2", "STC3")

  @BeforeEach
  fun setUpDuplicates() {
    stcIds.forEach { id ->
      dsl.agency(agencyId = id, name = "$id Secure Training Centre", otherAgencyType = OtherAgencyType.SECURE_TRAINING_CENTRE) {
        address(addressLine1 = "1 Agency Road", town = "Rugby", postcode = "CV23 8SY", country = "England")
        email(emailAddress = "${id.lowercase()}.agency@example.gov.uk")
        phoneNumber(phoneNumber = "01788 000 000")
      }
      dsl.prison(prisonId = id, name = "$id (STC)", active = false) {
        address(addressLine1 = "1 Prison Road", town = "Rugby", postcode = "CV23 8SY", country = "England")
        email("${id.lowercase()}.prison@example.gov.uk")
        phoneNumber("01788 111 111")
        department(departmentType = DepartmentType.SOCIAL_VISIT, emailAddress = "${id.lowercase()}.visits@example.gov.uk", phoneNumber = "01788 222 222")
        type(Type.STC)
      }
      jdbcTemplate.update("insert into prison_operator (prison_id, operator_id) select ?, min(id) from operator", id)
      jdbcTemplate.update("insert into prison_category (prison_id, category) values (?, 'C')", id)
    }
    dsl.prison(prisonId = "KEEP", name = "A real prison") {
      email("keep.prison@example.gov.uk")
    }
  }

  @AfterEach
  fun tearDown() {
    stcIds.filter { prisonRepository.existsById(it) }.forEach { prisonRepository.deleteById(it) }
    stcIds.filter { otherAgencyRepository.existsById(it) }.forEach { otherAgencyRepository.deleteById(it) }
    prisonRepository.deleteById("KEEP")
  }

  @Test
  fun `secure training centres are no longer listed twice`() {
    assertThat(allAgenciesCount()).isEqualTo(2)

    removeSecureTrainingCentresFromPrisons()

    assertThat(allAgenciesCount()).isEqualTo(1)
    stcIds.forEach { id ->
      assertThat(prisonRepository.existsById(id)).isFalse()
      assertThat(otherAgencyRepository.existsById(id)).isTrue()
      assertThat(syncDetails(id).agencyType).isEqualTo(LegacyAgencyType.SECURE_TRAINING_CENTRE)
    }
  }

  @Test
  fun `everything attached to the prison copies is removed and the agency records are untouched`() {
    removeSecureTrainingCentresFromPrisons()

    listOf("contact_details", "address", "prison_type", "prison_operator", "prison_category").forEach { table ->
      assertThat(countFor(table)).describedAs(table).isZero()
    }
    assertThat(emailCount("stc_.prison@example.gov.uk")).isZero()
    assertThat(emailCount("stc_.visits@example.gov.uk")).isZero()
    assertThat(phoneCount("01788 111 111")).isZero()
    assertThat(phoneCount("01788 222 222")).isZero()
    assertThat(emailCount("stc_.agency@example.gov.uk")).isEqualTo(2)
    assertThat(prisonRepository.existsById("KEEP")).isTrue()
    assertThat(jdbcTemplate.queryForObject("select count(*) from contact_details where prison_id = 'KEEP'", Int::class.java)).isEqualTo(1)
    stcIds.forEach { id ->
      with(syncDetails(id)) {
        assertThat(addresses).hasSize(1)
        assertThat(emailAddresses).hasSize(1)
        assertThat(phoneNumbers).hasSize(1)
      }
    }
  }

  @Test
  fun `running the script again changes nothing`() {
    removeSecureTrainingCentresFromPrisons()
    removeSecureTrainingCentresFromPrisons()

    stcIds.forEach { assertThat(otherAgencyRepository.existsById(it)).isTrue() }
    assertThat(prisonRepository.existsById("KEEP")).isTrue()
  }

  private fun allAgenciesCount() = jdbcTemplate.queryForObject("select count(*) from all_agencies where agency_id = 'STC2'", Int::class.java)

  private fun countFor(table: String) = jdbcTemplate.queryForObject("select count(*) from $table where prison_id in ('STC2', 'STC3')", Int::class.java)

  private fun emailCount(pattern: String) = jdbcTemplate.queryForObject("select count(*) from email_address where value like ?", Int::class.java, pattern)

  private fun phoneCount(number: String) = jdbcTemplate.queryForObject("select count(*) from phone_number where value = ?", Int::class.java, number)

  private fun syncDetails(id: String): LegacyAgencyDto = webTestClient.get()
    .uri("/legacy/$id")
    .accept(MediaType.APPLICATION_JSON)
    .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
    .exchange()
    .expectStatus().isOk
    .expectBodyResponse()

  private fun removeSecureTrainingCentresFromPrisons() = ResourceDatabasePopulator(ClassPathResource("db/migration/V1_68__remove_secure_training_centres_from_prisons.sql")).execute(dataSource)
}
