package uk.gov.justice.digital.hmpps.prisonregister.jpa

import org.assertj.core.api.Assertions.assertThat
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
    val prison = Prison("SHFCRT", "Sheffield Prison", true)

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
    assertThat(prison).get().isEqualTo(Prison("MDI", "Moorland (HMP & YOI)", true))
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
}
