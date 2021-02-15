package uk.gov.justice.digital.hmpps.prisonregister.model

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.transaction.TestTransaction
import org.springframework.transaction.annotation.Transactional

@ActiveProfiles("test")
@DataJpaTest
@Transactional
class VideolinkConferencingCentreRepositoryTest {

  @Autowired
  lateinit var prisonRepository: PrisonRepository

  @Autowired
  lateinit var vccRepository: VideoLinkConferencingCentreRepository

  @Test
  fun lifecycle() {
    val prisonId = "MDI"

    val prison = prisonRepository.findById(prisonId).get()
    vccRepository.save(VideolinkConferencingCentre(prison = prison, emailAddress = "a@b.com"))

    TestTransaction.flagForCommit()
    TestTransaction.end()
    TestTransaction.start()

    val vcc = vccRepository.findById(prisonId).get()
    with(vcc) {
      Assertions.assertThat(emailAddress).isEqualTo("a@b.com")
    }

    vccRepository.deleteById(prisonId)

    TestTransaction.flagForCommit()
    TestTransaction.end()
    TestTransaction.start()

    Assertions.assertThat(vccRepository.findById(prisonId)).isEmpty
  }
}
