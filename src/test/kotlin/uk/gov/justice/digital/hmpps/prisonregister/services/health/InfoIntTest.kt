package uk.gov.justice.digital.hmpps.prisonregister.services.health

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.prisonregister.resource.IntegrationTest
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter.ISO_DATE
import java.util.function.Consumer

class InfoIntTest : IntegrationTest() {
  @Test
  fun `Info page contains git information`() {
    webTestClient.get().uri("/info")
        .exchange()
        .expectStatus().isOk
        .expectBody().jsonPath("git.commit.id").isNotEmpty
  }

  @Test
  fun `Info page reports version`() {
    webTestClient.get().uri("/info")
        .exchange()
        .expectStatus().isOk
        .expectBody().jsonPath("build.version").value(Consumer<String> {
          assertThat(it).startsWith(LocalDateTime.now().format(ISO_DATE))
        })
  }
}
