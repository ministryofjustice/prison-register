package uk.gov.justice.digital.hmpps.prisonregister.service

import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyString
import uk.gov.justice.digital.hmpps.prisonregister.jpa.Prison
import uk.gov.justice.digital.hmpps.prisonregister.jpa.PrisonRepository
import uk.gov.justice.digital.hmpps.prisonregister.resource.PrisonDto
import java.util.Optional

class PrisonServiceTest {
  private val prisonRepository: PrisonRepository = mock()
  private val prisonService = PrisonService(prisonRepository)

  @Suppress("ClassName")
  @Nested
  inner class findById {
    @Test
    fun `find prison`() {
      whenever(prisonRepository.findById(anyString())).thenReturn(
        Optional.of(Prison("MDI", "A Prison", true))
      )
      val prisonDto = prisonService.findById("MDI")
      assertThat(prisonDto).isEqualTo(PrisonDto("MDI", "A Prison", true))
      verify(prisonRepository).findById("MDI")
    }
  }
}
