package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.seed

import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.kotlinx.dataframe.DataFrame
import org.jetbrains.kotlinx.dataframe.io.writeExcel
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SeedFileType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.SeedLogger
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.SeedService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.SeedXlsxService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.LogEntry
import java.io.File
import java.nio.file.Files
import kotlin.io.path.Path
import kotlin.io.path.pathString

@TestInstance(TestInstance.Lifecycle.PER_METHOD)
abstract class SeedTestBase : IntegrationTestBase() {
  @Autowired
  lateinit var seedService: SeedService

  @Autowired
  lateinit var seedXlsxService: SeedXlsxService

  @Value("\${seed.file-prefix}")
  lateinit var seedFilePrefix: String

  @MockkBean
  lateinit var mockSeedLogger: SeedLogger

  protected val logEntries = mutableListOf<LogEntry>()

  private val log = LoggerFactory.getLogger(this::class.java)

  @BeforeEach
  fun setUp() {
    every { mockSeedLogger.info(any()) } answers {
      logEntries += LogEntry(it.invocation.args[0] as String, "info", null)
      log.info(it.invocation.args[0] as String)
    }
    every { mockSeedLogger.error(any()) } answers {
      logEntries += LogEntry(it.invocation.args[0] as String, "error", null)
      log.error(it.invocation.args[0] as String)
    }
    every { mockSeedLogger.error(any(), any()) } answers {
      logEntries += LogEntry(it.invocation.args[0] as String, "error", it.invocation.args[1] as Throwable)
      log.info(it.invocation.args[0] as String, it.invocation.args[1] as Throwable)
    }
  }

  protected fun assertError(row: Int, message: String) {
    assertThat(logEntries).anyMatch {
      it.level == "error" &&
        it.message == "Error on row $row:" &&
        it.throwable != null &&
        it.throwable.message == message
    }
  }

  protected fun seed(seedFileType: SeedFileType, contents: String) {
    val fileName = seedFileType.name
    generateCsvFile(
      fileName,
      contents,
    )

    seedService.seedData(seedFileType, "$fileName.csv")
  }

  protected fun generateCsvFile(fileName: String, contents: String) {
    if (!Files.isDirectory(Path(seedFilePrefix))) {
      Files.createDirectory(Path(seedFilePrefix))
    }
    Files.writeString(
      Path("$seedFilePrefix/$fileName.csv"),
      contents,
    )
  }

  protected fun createXlsxForSeeding(fileName: String, sheets: Map<String, DataFrame<*>>) {
    val dir = Path(seedFilePrefix)

    if (!Files.isDirectory(dir)) {
      Files.createDirectory(dir)
    }

    val file = dir.resolve(fileName)
    if (Files.exists(file)) {
      Files.delete(file)
    }

    var fileExists = false
    sheets.forEach { (name, dataFrame) ->
      dataFrame.writeExcel(
        path = file.pathString,
        sheetName = name,
        keepFile = fileExists,
      )
      fileExists = true
    }
  }

  protected fun withXlsx(xlsxName: String, sheets: Map<String, DataFrame<*>>) {
    if (!Files.isDirectory(Path(seedFilePrefix))) {
      Files.createDirectory(Path(seedFilePrefix))
    }
    val path = "$seedFilePrefix/$xlsxName.xlsx"
    if (File(path).exists()) {
      File(path).delete()
    }

    var fileExists = false
    sheets.forEach { (name, dataFrame) ->
      dataFrame.writeExcel(
        "$seedFilePrefix/$xlsxName.xlsx",
        sheetName = name,
        keepFile = fileExists,
      )
      fileExists = true
    }
  }
}
