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
class VideolinkConferencingCentreRepositoryTest(
  @Autowired val prisonRepository: PrisonRepository,
  @Autowired val vccRepository: VideoLinkConferencingCentreRepository,
) {
  @Test
  fun lifecycle() {
    val prisonId = "MDI"

    val prison = prisonRepository.findByIdOrNull(prisonId) ?: fail("Expected to find Prison")

    vccRepository.save(VideolinkConferencingCentre(prison = prison, emailAddress = "a@b.com"))

    TestTransaction.flagForCommit()
    TestTransaction.end()
    TestTransaction.start()

    val vcc = vccRepository.findByIdOrNull(prisonId) ?: fail("Expected to find VCC")
    with(vcc) {
      assertThat(emailAddress).isEqualTo("a@b.com")
    }

    vccRepository.deleteById(prisonId)

    TestTransaction.flagForCommit()
    TestTransaction.end()
    TestTransaction.start()

    assertThat(vccRepository.findById(prisonId)).isEmpty
  }
}
