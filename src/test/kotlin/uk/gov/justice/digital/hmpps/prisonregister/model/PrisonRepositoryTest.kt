package uk.gov.justice.digital.hmpps.prisonregister.model

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.transaction.TestTransaction
import org.springframework.transaction.annotation.Transactional

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = WebEnvironment.MOCK)
@Transactional
class PrisonRepositoryTest {

  @Autowired
  lateinit var prisonRepository: PrisonRepository

  @Test
  fun `should insert prison`() {
    val prison = Prison("SHFCRT", "Sheffield Prison", active = true)

    val id = prisonRepository.save(prison).prisonId

    TestTransaction.flagForCommit()
    TestTransaction.end()

    val savedPrison = prisonRepository.findById(id).get()

    with(savedPrison) {
      assertThat(prisonId).isEqualTo("SHFCRT")
      assertThat(name).isEqualTo("Sheffield Prison")
      assertThat(active).isEqualTo(true)
    }
  }

  @Test
  fun `should find prison by name`() {
    val prison = prisonRepository.findById("MDI")
    assertThat(prison).get().isEqualTo(Prison("MDI", "Moorland (HMP & YOI)", active = true))
  }

  @Test
  fun `should find associated gp practice`() {
    val prison = prisonRepository.findById("MDI").orElseThrow()
    assertThat(prison.gpPractice).isEqualTo(PrisonGpPractice("MDI", "Y05537"))
  }

  @Test
  fun `should find associated prison address`() {
    val prison = prisonRepository.findById("MDI").orElseThrow()
    assertThat(prison.addresses).size().isEqualTo(1)
    assertThat(prison.addresses.first().addressLine1).isEqualTo("Bawtry Road")
    assertThat(prison.addresses.first().postcode).isEqualTo("DN7 6BW")
  }

  @Test
  fun `should get active prisons`() {
    val activePrisons = prisonRepository.findByActiveOrderByPrisonId(true)
    assertThat(activePrisons).hasSizeGreaterThan(100).allMatch { it.active }

    val inActivePrisons = prisonRepository.findByActiveOrderByPrisonId(false)
    assertThat(inActivePrisons).hasSizeGreaterThan(40).allMatch { !it.active }

    val allPrisons = prisonRepository.findAll()
    assertThat(allPrisons).hasSize(activePrisons.size + inActivePrisons.size)
  }

  @Test
  fun `should find prison by gp practice code`() {
    val prison = prisonRepository.findByGpPracticeGpPracticeCode("Y05537")
    assertThat(prison).isEqualTo(Prison("MDI", "Moorland (HMP & YOI)", active = true))
  }

  @Nested
  inner class prisonFilter {
    @Test
    fun `should find all prisons when no params provided`() {
      val allPrisons = prisonRepository.findAll(PrisonFilter())
      val groupedByActive = allPrisons.groupBy { it.active }
      assertThat(groupedByActive[true]).hasSizeGreaterThan(100)
      assertThat(groupedByActive[false]).hasSizeGreaterThan(40)
    }

    @Test
    fun `should find prisons by active or inactive`() {
      val activePrisons = prisonRepository.findAll(PrisonFilter(active = true))
      assertThat(activePrisons).hasSizeGreaterThan(100).allMatch { it.active }

      val inactivePrisons = prisonRepository.findAll(PrisonFilter(active = false))
      assertThat(inactivePrisons).hasSizeGreaterThan(40).allMatch { !it.active }
    }

    @Test
    fun `should find prisons by text search`() {
      // case insensitive
      val prisonsByPrisonId = prisonRepository.findAll(PrisonFilter(textSearch = "mdi"))
      assertThat(prisonsByPrisonId.first()).isEqualTo(Prison("MDI", "Moorland (HMP & YOI)", active = true))

      // wildcard is supported
      val prisonsByPrisonName = prisonRepository.findAll(PrisonFilter(textSearch = "moorland"))
      assertThat(prisonsByPrisonName.first()).isEqualTo(Prison("MDI", "Moorland (HMP & YOI)", active = true))
    }

    @Test
    fun `should find prisons by male and female flags`() {
      val malePrisons = prisonRepository.findAll(PrisonFilter(male = true))
      assertThat(malePrisons).hasSizeGreaterThan(100).allMatch { it.male }

      val femalePrisons = prisonRepository.findAll(PrisonFilter(female = true))
      assertThat(femalePrisons).hasSizeGreaterThan(10).allMatch { it.female }

      val bothMaleAndFemale = prisonRepository.findAll(PrisonFilter(male = true, female = true))
      assertThat(bothMaleAndFemale.first()).isEqualTo(Prison("WYI", "Wetherby (HMPYOI)", active = true, male = true, female = true))
    }

    @Test
    fun `should find prisons by active , text search , male flag`() {
      val prisonsByMultipleFields = prisonRepository.findAll(PrisonFilter(active = true, textSearch = "vei", male = true))
      assertThat(prisonsByMultipleFields.first()).isEqualTo(Prison("VEI", "The Verne (HMP)", active = true, male = true))
    }
  }
}
