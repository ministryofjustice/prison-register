package uk.gov.justice.digital.hmpps.prisonregister.integration

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.DuplicateKeyException
import org.springframework.jdbc.core.JdbcTemplate
import uk.gov.justice.digital.hmpps.prisonregister.dsl.Root
import uk.gov.justice.digital.hmpps.prisonregister.model.CourtRepository

class PrimaryKeyIntTest : IntegrationTestBase() {

  @Autowired
  private lateinit var jdbcTemplate: JdbcTemplate

  @Autowired
  private lateinit var dsl: Root

  @Autowired
  private lateinit var courtRepository: CourtRepository

  private val courtId = "PKTEST"

  @AfterEach
  fun removeCourt() {
    courtRepository.deleteById(courtId)
  }

  // HMPPS Data Hub cannot ingest a table without a primary key (MAPB-1197), so a new table without one fails here
  @Test
  fun `every table has a primary key`() {
    val tablesWithoutPrimaryKey = jdbcTemplate.queryForList(
      """
      select t.relname
        from pg_class t
        join pg_namespace n on n.oid = t.relnamespace
       where n.nspname = 'public'
         and t.relkind = 'r'
         and not exists (select 1 from pg_constraint c where c.conrelid = t.oid and c.contype = 'p')
       order by t.relname
      """,
      String::class.java,
    )

    assertThat(tablesWithoutPrimaryKey).isEmpty()
  }

  @Test
  fun `the same link between an agency and its phone number cannot be stored twice`() {
    dsl.court(courtId = courtId, name = "Primary Key Test Court") {
      phoneNumber(phoneNumber = "0114 555 1234")
    }

    assertThatThrownBy {
      jdbcTemplate.update("insert into court_to_phone (court_id, phone_id) select court_id, phone_id from court_to_phone where court_id = ?", courtId)
    }.isInstanceOf(DuplicateKeyException::class.java)
    assertThat(jdbcTemplate.queryForObject("select count(*) from court_to_phone where court_id = ?", Long::class.java, courtId)).isEqualTo(1)
  }
}
