package uk.gov.justice.digital.hmpps.prisonregister.model

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.flywaydb.core.Flyway
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.datasource.SingleConnectionDataSource
import org.testcontainers.postgresql.PostgreSQLContainer
import java.sql.DriverManager
import java.util.UUID

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ContactOwnershipMigrationTest {
  private val postgres = PostgreSQLContainer("postgres:17")
  private val agencies = listOf("court", "hospital", "probation_office", "police_custody_suite", "approved_premises", "agency")

  @BeforeAll
  fun startDatabase() {
    postgres.start()
  }

  @AfterAll
  fun stopDatabase() {
    postgres.stop()
  }

  @Test
  fun `splits shared contacts across every owner without losing values or unshared IDs`() = withPreviousSchema { jdbc, migrate ->
    listOf("email_address", "phone_number", "web_address").forEach { table ->
      jdbc.update("insert into $table (id, value) values (10000, 'shared'), (20000, 'unshared'), (30000, 'unreferenced')")
    }
    agencies.forEach { table ->
      val extras = when (table) {
        "court" -> ", court_type"
        "hospital" -> ", high_security"
        "agency" -> ", agency_type"
        else -> ""
      }
      val values = when (table) {
        "court" -> ", 'CC'"
        "hospital" -> ", false"
        "agency" -> ", 'PROBATION_CRC'"
        else -> ""
      }
      jdbc.update("insert into $table (${table}_id, name, active$extras) values ('TEST', 'Test', true$values)")
      jdbc.update("insert into ${table}_to_phone values ('TEST', 10000)")
      if (table != "hospital") jdbc.update("insert into ${table}_to_email_address values ('TEST', 10000)")
    }
    jdbc.update("insert into contact_details (id, prison_id, department_type) values (100, 'MDI', 'SOCIAL_VISIT'), (101, 'MDI', 'OFFICIAL_VISIT'), (102, 'MDI', 'OFFENDER_MANAGEMENT_UNIT')")
    listOf("email_address", "phone_number", "web_address").forEach { table ->
      jdbc.update("insert into contact_details_to_$table values (100, 10000), (101, 10000), (102, 20000)")
    }

    migrate()

    listOf("email_address", "phone_number", "web_address").forEach { table ->
      val agencyLinks = when (table) {
        "email_address" -> agencies.filterNot { it == "hospital" }.map { "select email_address_id as id from ${it}_to_email_address" }
        "phone_number" -> agencies.map { "select phone_id as id from ${it}_to_phone" }
        else -> emptyList()
      }
      val references = (agencyLinks + "select ${table}_id as id from contact_details_to_$table").joinToString(" union all ")
      val ids = jdbc.queryForList(references, Int::class.java)
      assertThat(ids).doesNotHaveDuplicates().contains(10000, 20000)
      assertThat(jdbc.queryForObject("select count(*) from $table where value = 'shared'", Int::class.java))
        .isEqualTo(ids.size - 1)
      assertThat(jdbc.queryForObject("select value from $table where id = 30000", String::class.java)).isEqualTo("unreferenced")
      assertThat(jdbc.queryForObject("select ${table}_id from contact_details_to_$table where contact_details_id = 102", Int::class.java)).isEqualTo(20000)
      assertThat(jdbc.queryForObject("select count(*) from ($references) refs join $table contact on contact.id = refs.id where contact.value = 'shared'", Int::class.java))
        .isEqualTo(ids.size - 1)
      val nextId = requireNotNull(jdbc.queryForObject("insert into $table (value) values ('shared') returning id", Int::class.java))
      assertThat(nextId).isGreaterThanOrEqualTo(30001).isNotIn(ids)
      jdbc.update("insert into $table (value) values ('shared')")
      assertThatThrownBy { jdbc.update("insert into $table (value) values (null)") }
        .isInstanceOf(DataIntegrityViolationException::class.java)
      assertThatThrownBy { jdbc.update("insert into $table (id, value) values (10000, 'duplicate ID')") }
        .isInstanceOf(DataIntegrityViolationException::class.java)
      assertThatThrownBy { jdbc.update("update contact_details_to_$table set ${table}_id = 20000 where contact_details_id = 100") }
        .isInstanceOf(DataIntegrityViolationException::class.java)
      assertThatThrownBy { jdbc.update("update contact_details_to_$table set ${table}_id = -1 where contact_details_id = 100") }
        .isInstanceOf(DataIntegrityViolationException::class.java)
    }
    assertThat(jdbc.queryForObject("select email_address_id from court_to_email_address", Int::class.java)).isEqualTo(10000)
    assertThat(jdbc.queryForObject("select phone_id from court_to_phone", Int::class.java)).isEqualTo(10000)
    assertThat(jdbc.queryForObject("select web_address_id from contact_details_to_web_address where contact_details_id = 100", Int::class.java)).isEqualTo(10000)
    assertThatThrownBy { jdbc.update("insert into contact_details (prison_id, department_type) values ('MDI', 'SOCIAL_VISIT')") }
      .isInstanceOf(DataIntegrityViolationException::class.java)
  }

  @Test
  fun `empty contact tables migrate and accept duplicate values`() = withPreviousSchema { jdbc, migrate ->
    migrate()
    listOf("email_address", "phone_number", "web_address").forEach {
      assertThat(jdbc.queryForObject("select count(*) from $it", Int::class.java)).isZero()
      jdbc.update("insert into $it (value) values ('same'), ('same')")
      assertThat(jdbc.queryForObject("select count(*) from $it where value = 'same'", Int::class.java)).isEqualTo(2)
    }
  }

  private fun withPreviousSchema(test: (JdbcTemplate, () -> Unit) -> Unit) {
    val schema = "ownership_${UUID.randomUUID().toString().replace("-", "")}"
    fun flyway(target: String) = Flyway.configure()
      .dataSource(postgres.jdbcUrl, postgres.username, postgres.password)
      .schemas(schema)
      .defaultSchema(schema)
      .target(target)
      .load()
    flyway("1.64").migrate()
    DriverManager.getConnection(postgres.jdbcUrl, postgres.username, postgres.password).use { connection ->
      connection.schema = schema
      test(JdbcTemplate(SingleConnectionDataSource(connection, true))) { flyway("1.65").migrate() }
    }
  }
}
