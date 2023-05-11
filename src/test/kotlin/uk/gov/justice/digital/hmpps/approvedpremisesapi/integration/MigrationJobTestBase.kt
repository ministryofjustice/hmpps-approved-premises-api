package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration

import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.approvedpremisesapi.migration.MigrationLogger
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.MigrationJobService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.LogEntry

@TestInstance(TestInstance.Lifecycle.PER_METHOD)
abstract class MigrationJobTestBase : IntegrationTestBase() {
  @Autowired
  lateinit var migrationJobService: MigrationJobService

  @MockkBean
  lateinit var mockMigrationLogger: MigrationLogger
  protected val logEntries = mutableListOf<LogEntry>()

  @BeforeEach
  fun setUp() {
    every { mockMigrationLogger.info(any()) } answers {
      logEntries += LogEntry(it.invocation.args[0] as String, "info", null)
    }
    every { mockMigrationLogger.error(any()) } answers {
      logEntries += LogEntry(it.invocation.args[0] as String, "error", null)
    }
    every { mockMigrationLogger.error(any(), any()) } answers {
      logEntries += LogEntry(it.invocation.args[0] as String, "error", it.invocation.args[1] as Throwable)
    }
  }
}
