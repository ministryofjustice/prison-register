package uk.gov.justice.digital.hmpps.prisonregister.model

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.fail
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.data.repository.findByIdOrNull
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.transaction.TestTransaction
import org.springframework.transaction.annotation.Transactional

@ActiveProfiles("test")
@DataJpaTest
@Transactional
class OffenderManagementUnitRepositoryTest(
  @Autowired val prisonRepository: PrisonRepository,
  @Autowired val omuRepository: OffenderManagementUnitRepository,
) {

  @Test
  fun lifecycle() {
    val prisonId = "MDI"

    val prison = prisonRepository.findByIdOrNull(prisonId) ?: fail("Expected to find Prison")

    omuRepository.save(OffenderManagementUnit(prison = prison, emailAddress = "a@b.com"))

    TestTransaction.flagForCommit()
    TestTransaction.end()
    TestTransaction.start()

    val omu = omuRepository.findByIdOrNull(prisonId) ?: fail("Expected to find OMU")
    with(omu) {
      assertThat(emailAddress).isEqualTo("a@b.com")
    }

    omuRepository.deleteById(prisonId)

    TestTransaction.flagForCommit()
    TestTransaction.end()
    TestTransaction.start()

    assertThat(omuRepository.findById(prisonId)).isEmpty
  }
}
