package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.migration

import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.migration.MigrationJobService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.migration.MigrationLogger
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.LogEntry

@TestInstance(TestInstance.Lifecycle.PER_METHOD)
abstract class MigrationJobTestBase : IntegrationTestBase() {
  @Autowired
  lateinit var migrationJobService: MigrationJobService

  @MockkBean
  lateinit var mockMigrationLogger: MigrationLogger
  protected val logEntries = mutableListOf<LogEntry>()

  private val log = LoggerFactory.getLogger(this::class.java)

  @BeforeEach
  fun setUp() {
    every { mockMigrationLogger.info(any()) } answers {
      val message = it.invocation.args[0] as String
      logEntries += LogEntry(message, "info", null)
      log.info(message)
    }
    every { mockMigrationLogger.error(any()) } answers {
      val message = it.invocation.args[0] as String
      logEntries += LogEntry(message, "error", null)
      log.error(message)
    }
    every { mockMigrationLogger.error(any(), any()) } answers {
      val message = it.invocation.args[0] as String
      val throwable = it.invocation.args[1] as Throwable
      logEntries += LogEntry(message, "error", it.invocation.args[1] as Throwable)
      log.error(message, throwable)
    }
  }
}
