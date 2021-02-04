package uk.gov.justice.digital.hmpps.courtregister

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class CourtRegisterApplication

fun main(args: Array<String>) {
  runApplication<CourtRegisterApplication>(*args)
}
