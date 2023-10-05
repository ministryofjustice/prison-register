package uk.gov.justice.digital.hmpps.visitscheduler.integration.container

import org.slf4j.LoggerFactory
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.containers.output.Slf4jLogConsumer
import org.testcontainers.containers.wait.strategy.Wait
import java.io.IOException
import java.net.ServerSocket

object PostgresContainer {
  private val log = LoggerFactory.getLogger(this::class.java)
  val instance: PostgreSQLContainer<Nothing>? by lazy { startPostgresqlIfNotRunning() }
  private fun startPostgresqlIfNotRunning(): PostgreSQLContainer<Nothing>? {
    if (isPostgresRunning()) {
      return null
    }

    val logConsumer = Slf4jLogConsumer(log).withPrefix("postgresql")

    return PostgreSQLContainer<Nothing>("postgres:13.2").apply {
      withEnv("HOSTNAME_EXTERNAL", "localhost")
      withExposedPorts(5432)
      withDatabaseName("prison-register")
      withUsername("prison-register")
      withPassword("prison-register")
      setWaitStrategy(Wait.forListeningPort())
      withReuse(true)
      start()
      followOutput(logConsumer)
    }
  }

  private fun isPostgresRunning(): Boolean =
    try {
      val serverSocket = ServerSocket(5432)
      serverSocket.localPort == 0
    } catch (e: IOException) {
      true
    }
}
