package uk.gov.justice.digital.hmpps.courtregister.jpa

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
class CourtRepositoryTest {

  @Autowired
  lateinit var courtRepository: CourtRepository

  @Test
  fun `should insert court`() {
    val court = Court("SHFCRT", "Sheffield Court", "A Court in Sheffield", true)

    val id = courtRepository.save(court).id

    TestTransaction.flagForCommit()
    TestTransaction.end()

    val savedCourt = courtRepository.findById(id).get()

    with(savedCourt) {
      assertThat(id).isEqualTo("SHFCRT")
      assertThat(courtName).isEqualTo("Sheffield Court")
      assertThat(active).isEqualTo(true)
    }
  }

  @Test
  fun `should find court by name`() {
    val court = courtRepository.findById("SHEFCC")
    assertThat(court).get().isEqualTo(Court("SHEFCC", "Sheffield Crown Court", null, true))
  }

  @Test
  fun `should get active courts`() {
    val activeCourts = courtRepository.findByActiveOrderById(true)
    assertThat(activeCourts).hasSizeGreaterThan(100).allMatch { it.active }

    val inActiveCourts = courtRepository.findByActiveOrderById(false)
    assertThat(inActiveCourts).hasSizeGreaterThan(40).allMatch { !it.active }

    val allCourts = courtRepository.findAll()
    assertThat(allCourts).hasSize(activeCourts.size + inActiveCourts.size)
  }
}
