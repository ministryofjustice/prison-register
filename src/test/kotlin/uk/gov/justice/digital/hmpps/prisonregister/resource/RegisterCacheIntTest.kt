package uk.gov.justice.digital.hmpps.prisonregister.resource

import jakarta.persistence.EntityManagerFactory
import org.assertj.core.api.Assertions.assertThat
import org.hibernate.SessionFactory
import org.hibernate.stat.Statistics
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.prisonregister.dsl.Root
import uk.gov.justice.digital.hmpps.prisonregister.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.prisonregister.model.CourtRepository
import uk.gov.justice.digital.hmpps.prisonregister.model.PrisonRepository
import uk.gov.justice.digital.hmpps.prisonregister.resource.model.PrisonRequest

/**
 * The busy prison and agency reads are served from an in-memory cache, which is cleared whenever a
 * change is committed (MAPB-1128).
 */
class RegisterCacheIntTest : IntegrationTestBase() {

  @Autowired
  lateinit var dsl: Root

  @Autowired
  lateinit var courtRepository: CourtRepository

  @Autowired
  lateinit var prisonRepository: PrisonRepository

  @Autowired
  lateinit var entityManagerFactory: EntityManagerFactory

  private val statistics: Statistics by lazy { entityManagerFactory.unwrap(SessionFactory::class.java).statistics }

  @BeforeEach
  fun setUp() {
    dsl.prison(prisonId = "CAPRI", name = "Cache test prison", male = true) {}
    dsl.court(courtId = "CACRT", name = "Cache test court", areaCode = "52", regionCode = "YOHUM") {}
  }

  @AfterEach
  fun tearDown() {
    courtRepository.deleteById("CACRT")
    prisonRepository.deleteById("CAPRI")
  }

  @Test
  fun `repeated prison reads do not use the database`() {
    readPrisons(prisonIds = listOf("CAPRI"))
    statistics.clear()

    // A different mix of ids is still served from the cache, because every prison is held together
    readPrisons(prisonIds = listOf("CAPRI", "NOSUCH"))

    assertThat(statistics.prepareStatementCount).isZero
    assertThat(statistics.transactionCount).isZero
  }

  @Test
  fun `repeated agency reads do not use the database`() {
    readAgencies()
    statistics.clear()

    readAgencies()

    assertThat(statistics.prepareStatementCount).isZero
    assertThat(statistics.transactionCount).isZero
  }

  @Test
  fun `a prison change made through the maintenance endpoints is seen by the next read`() {
    readPrisons(prisonIds = listOf("CAPRI"))

    webTestClient.put().uri("/prison-maintenance/id/CAPRI")
      .headers(setAuthorisation(roles = listOf("ROLE_MAINTAIN_REF_DATA"), scopes = listOf("write")))
      .bodyValue(UpdatePrisonDto(prisonName = "Cache test prison renamed", prisonNameInWelsh = "Carchar prawf", active = true, male = true))
      .exchange()
      .expectStatus().isOk

    webTestClient.get().uri("/prisons/id/CAPRI")
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.prisonName").isEqualTo("Cache test prison renamed")
      .jsonPath("$.prisonNameInWelsh").isEqualTo("Carchar prawf")

    webTestClient.post().uri("/prisons/prisonsByIds")
      .contentType(MediaType.APPLICATION_JSON)
      .bodyValue(PrisonRequest(listOf("CAPRI")))
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$[0].prisonName").isEqualTo("Cache test prison renamed")

    webTestClient.get().uri("/prisons/names?prison_id=CAPRI")
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$[0].prisonName").isEqualTo("Cache test prison renamed")
  }

  @Test
  fun `an agency change made through the NOMIS sync is seen by the next read`() {
    readAgencies()

    webTestClient.post().uri("/legacy/sync/agency/id/CACRT")
      .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
      .bodyValue(
        LegacyAgencyDto(
          agencyType = LegacyAgencyType.COURT,
          name = "Cache test court renamed",
          description = "Cache test court renamed",
          active = true,
          inactiveDate = null,
          contact = null,
          cjitCode = null,
          accessibleAccess = null,
          areaCode = "52",
          regionCode = "YOHUM",
          geographicalAreaCode = null,
          payrollRegionCode = null,
          localAuthorityCode = null,
          courtTypeCode = "CC",
          addresses = emptyList(),
          emailAddresses = emptyList(),
          phoneNumbers = emptyList(),
        ),
      )
      .exchange()
      .expectStatus().isOk

    webTestClient.get().uri("/api/agencies/CACRT")
      .headers(setAuthorisation(roles = listOf("ROLE_HMPPS_REGISTERS_API__R")))
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.description").isEqualTo("Cache test court renamed")

    webTestClient.get().uri("/api/agencies/type/COURT")
      .headers(setAuthorisation(roles = listOf("ROLE_HMPPS_REGISTERS_API__R")))
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$[?(@.agencyId == 'CACRT')].description").isEqualTo("Cache test court renamed")
  }

  @Test
  fun `read-only requests do not clear the cache`() {
    readPrisons(prisonIds = listOf("CAPRI"))

    webTestClient.get().uri("/legacy/agency/CACRT/is-used")
      .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__MAINTAIN__RW")))
      .exchange()
      .expectStatus().isOk
    webTestClient.get().uri("/legacy/reconciliation/CACRT")
      .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
      .exchange()
      .expectStatus().isOk
    statistics.clear()

    readPrisons(prisonIds = listOf("CAPRI"))

    assertThat(statistics.prepareStatementCount).isZero
  }

  @Test
  fun `an unknown prison is still not found`() {
    readPrisons(prisonIds = listOf("CAPRI"))

    webTestClient.get().uri("/prisons/id/NOSUCH")
      .exchange()
      .expectStatus().isNotFound
  }

  private fun readPrisons(prisonIds: List<String>) {
    webTestClient.get().uri("/prisons/id/CAPRI")
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.prisonId").isEqualTo("CAPRI")

    webTestClient.get().uri("/prisons")
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$[?(@.prisonId == 'CAPRI')].prisonName").isEqualTo("Cache test prison")

    webTestClient.post().uri("/prisons/prisonsByIds")
      .contentType(MediaType.APPLICATION_JSON)
      .bodyValue(PrisonRequest(prisonIds))
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.length()").isEqualTo(1)
      .jsonPath("$[0].prisonId").isEqualTo("CAPRI")

    webTestClient.get().uri("/prisons/names?active=true")
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$[?(@.prisonId == 'CAPRI')].prisonName").isEqualTo("Cache test prison")
  }

  private fun readAgencies() {
    listOf("/api/agencies/CACRT", "/api/agencies/type/COURT", "/api/agencies", "/api/agencies/legacy-type/CRT").forEach {
      webTestClient.get().uri(it)
        .headers(setAuthorisation(roles = listOf("ROLE_HMPPS_REGISTERS_API__R")))
        .exchange()
        .expectStatus().isOk
    }
  }
}
