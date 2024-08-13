package uk.gov.justice.digital.hmpps.prisonregister.resource

import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.springframework.boot.test.mock.mockito.MockBean
import uk.gov.justice.digital.hmpps.prisonregister.integration.IntegrationTest
import uk.gov.justice.digital.hmpps.prisonregister.model.Address
import uk.gov.justice.digital.hmpps.prisonregister.model.AddressRepository
import uk.gov.justice.digital.hmpps.prisonregister.model.Category
import uk.gov.justice.digital.hmpps.prisonregister.model.Operator
import uk.gov.justice.digital.hmpps.prisonregister.model.Prison
import uk.gov.justice.digital.hmpps.prisonregister.model.PrisonFilter
import uk.gov.justice.digital.hmpps.prisonregister.model.PrisonRepository
import uk.gov.justice.digital.hmpps.prisonregister.model.PrisonType
import uk.gov.justice.digital.hmpps.prisonregister.model.Type
import uk.gov.justice.digital.hmpps.prisonregister.resource.model.PrisonRequest
import java.util.Optional

class PrisonResourceIntTest : IntegrationTest() {
  @MockBean
  private lateinit var prisonRepository: PrisonRepository

  @MockBean
  private lateinit var addressRepository: AddressRepository

  @Suppress("ClassName")
  @Nested
  inner class `find prisons` {
    @Test
    fun `find all prisons`() {
      val prison = Prison(
        prisonId = "MDI",
        name = "Moorland HMP",
        active = true,
        male = true,
        female = false,
        contracted = true,
        categories = mutableSetOf(Category.B, Category.C),
      )

      val address = Address(
        21,
        "Bawtry Road",
        "Hatfield Woodhouse",
        "Doncaster",
        "South Yorkshire",
        "DN7 6BW",
        "England",
        prison,
      )

      val operator = Operator(1, "PSP")

      prison.addresses = listOf(address)
      prison.prisonOperators = listOf(operator)

      val prisons = listOf(
        prison,
        Prison("LEI", "Leeds HMP", active = true, female = true),
      )

      whenever(prisonRepository.findAll()).thenReturn(
        prisons,
      )
      webTestClient.get().uri("/prisons")
        .exchange()
        .expectStatus().isOk
        .expectBody().json("prisons".loadJson())
    }

    @Test
    fun `find prison by id`() {
      val prison = Prison("MDI", "Moorland HMP", active = true, male = true, female = false, contracted = true, categories = mutableSetOf(Category.D))
      val mdiAddress = Address(
        21,
        "Bawtry Road",
        "Hatfield Woodhouse",
        "Doncaster",
        "South Yorkshire",
        "DN7 6BW",
        "England",
        prison,
      )

      prison.addresses = listOf(mdiAddress)
      prison.prisonOperators = listOf(Operator(1, "PSP"))

      whenever(prisonRepository.findById(anyString())).thenReturn(
        Optional.of(prison),
      )
      webTestClient.get().uri("/prisons/id/MDI")
        .exchange()
        .expectStatus().isOk
        .expectBody().json("prison_id_MDI".loadJson())
    }

    @Test
    fun `find prisons by ids`() {
      val prison = Prison(
        prisonId = "MDI",
        name = "Moorland HMP",
        active = true,
        male = true,
        female = false,
        contracted = true,
        categories = mutableSetOf(Category.B, Category.C),
      )

      val address = Address(
        21,
        "Bawtry Road",
        "Hatfield Woodhouse",
        "Doncaster",
        "South Yorkshire",
        "DN7 6BW",
        "England",
        prison,
      )

      val operator = Operator(1, "PSP")

      prison.addresses = listOf(address)
      prison.prisonOperators = listOf(operator)

      val prisons = listOf(
        prison,
        Prison("LEI", "Leeds HMP", active = true, female = true),
      )

      val prisonsList = listOf("MDI", "LEI")
      whenever(prisonRepository.findAllByPrisonIdIsIn(prisonsList)).thenReturn(
        prisons,
      )
      webTestClient.post().uri("/prisons/prisonsByIds")
        .header("Content-Type", "application/json")
        .bodyValue(PrisonRequest(prisonsList))
        .exchange()
        .expectStatus().isOk
        .expectBody().json("prisons".loadJson())
    }

    @Test
    fun `find prison validation failure`() {
      webTestClient.get().uri("/prisons/id/1234567890123")
        .exchange()
        .expectStatus().isBadRequest
        .expectBody().json("prison_id_badrequest_getPrisonFromId".loadJson())
    }
  }

  @Suppress("ClassName")
  @Nested
  inner class getPrisonsBySearchFilter {
    @Test
    fun `search by active , text , male flag , prison type`() {
      val prisons = listOf(
        Prison(
          "MDI",
          "Moorland HMP",
          active = true,
          male = true,
          female = true,
          contracted = true,
          lthse = true,
        ).apply {
          val prison = this
          prisonTypes = mutableSetOf(PrisonType(prison = prison, type = Type.HMP))
        },
      )
      whenever(prisonRepository.findAll(any<PrisonFilter>())).thenReturn(prisons)

      webTestClient.get()
        .uri { uriBuilder ->
          uriBuilder
            .path("/prisons/search")
            .queryParam("active", true)
            .queryParam("textSearch", "MDI")
            .queryParam("genders", listOf("MALE"))
            .queryParam("prisonTypeCodes", listOf("HMP"))
            .queryParam("lthse", true)
            .build()
        }
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$[0].prisonId").isEqualTo("MDI")
        .jsonPath("$[0].prisonName").isEqualTo("Moorland HMP")
        .jsonPath("$[0].active").isEqualTo(true)
        .jsonPath("$[0].male").isEqualTo(true)
        .jsonPath("$[0].female").isEqualTo(true)
        .jsonPath("$[0].contracted").isEqualTo(true)
        .jsonPath("$[0].lthse").isEqualTo(true)
        .jsonPath("$[0].types").isEqualTo(mapOf("code" to "HMP", "description" to "His Majesty’s Prison"))
    }

    @Test
    fun `no search params provided`() {
      val prisons = listOf(
        Prison("MDI", "Moorland HMP", active = true),
      )
      whenever(prisonRepository.findAll(any<PrisonFilter>())).thenReturn(prisons)

      webTestClient.get()
        .uri { uriBuilder ->
          uriBuilder
            .path("/prisons/search")
            .build()
        }
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$[0].prisonId").isEqualTo("MDI")
        .jsonPath("$[0].prisonName").isEqualTo("Moorland HMP")
        .jsonPath("$[0].active").isEqualTo(true)
    }
  }

  @Nested
  inner class FindPrisonAddressById {
    @Test
    fun `should find prison address by prison and address id`() {
      val prison = Prison("MDI", "A Prison", active = true)
      val address = Address(
        id = 77,
        addressLine1 = "Bawtry Road",
        addressLine2 = "Hatfield Woodhouse",
        town = "Doncaster",
        county = "South Yorkshire",
        country = "England",
        postcode = "DN7 6BW",
        prison = prison,
      )
      prison.addresses = listOf(address)

      whenever(addressRepository.findById(any())).thenReturn(
        Optional.of(address),
      )

      webTestClient.get().uri("/prisons/id/MDI/address/77")
        .exchange()
        .expectStatus().isOk
        .expectBody().json("prison_address".loadJson())
    }

    @Test
    fun `should not find an address not associated with the prison Id`() {
      val prison = Prison("MDI", "Moorland HMP", active = true)
      val mdiAddress = Address(
        21,
        "Bawtry Road",
        "Hatfield Woodhouse",
        "Doncaster",
        "South Yorkshire",
        "DN7 6BW",
        "England",
        prison,
      )

      whenever(addressRepository.findById(any())).thenReturn(
        Optional.of(mdiAddress),
      )
      webTestClient.get().uri("/prisons/id/LEI/address/21")
        .exchange()
        .expectStatus().isNotFound
        .expectBody()
        .jsonPath("$.developerMessage").isEqualTo("Address 21 not in prison LEI")
    }

    @Test
    fun `should not find an address if the address does not exist`() {
      webTestClient.get().uri("/prisons/id/MDI/address/999")
        .exchange()
        .expectStatus().isNotFound
        .expectBody()
        .jsonPath("$.developerMessage").isEqualTo("Address 999 not found")
    }
  }

  private fun String.loadJson(): String =
    PrisonResourceIntTest::class.java.getResource("$this.json").readText()
}
