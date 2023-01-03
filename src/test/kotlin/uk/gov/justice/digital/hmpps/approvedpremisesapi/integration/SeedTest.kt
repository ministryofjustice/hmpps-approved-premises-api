package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration

import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SeedFileType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SeedRequest
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.SeedLogger
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.SeedService
import java.nio.file.Files
import java.nio.file.StandardOpenOption
import kotlin.io.path.Path

@TestInstance(TestInstance.Lifecycle.PER_METHOD)
class SeedTest : IntegrationTestBase() {
  @Autowired
  lateinit var seedService: SeedService

  @Value("\${seed.file-prefix}")
  lateinit var seedFilePrefix: String

  @MockkBean
  lateinit var mockSeedLogger: SeedLogger
  private val logEntries = mutableListOf<LogEntry>()

  @BeforeEach
  fun setUp() {
    every { mockSeedLogger.info(any()) } answers {
      logEntries += LogEntry(it.invocation.args[0] as String, "info", null)
    }
    every { mockSeedLogger.error(any()) } answers {
      logEntries += LogEntry(it.invocation.args[0] as String, "error", null)
    }
    every { mockSeedLogger.error(any(), any()) } answers {
      logEntries += LogEntry(it.invocation.args[0] as String, "error", it.invocation.args[1] as Throwable)
    }
  }

  @Test
  fun `Requesting a Seed operation returns 202`() {
    webTestClient.post()
      .uri("/seed")
      .bodyValue(
        SeedRequest(
          seedType = SeedFileType.approvedPremises,
          fileName = "file.csv"
        )
      )
      .exchange()
      .expectStatus()
      .isAccepted
  }

  @Test
  fun `Attempting to process a file containing dots logs an error`() {
    seedService.seedData(SeedFileType.approvedPremises, "afile.csv")

    assertThat(logEntries).anyMatch {
      it.level == "error" &&
        it.message == "Unable to complete Seed Job" &&
        it.throwable != null &&
        it.throwable.message!!.contains(
          "Filename must be just the filename of a .csv file in the /seed directory, e.g. for /seed/upload.csv, just `upload` should be supplied"
        )
    }
  }

  @Test
  fun `Attempting to process a file containing forward slashes logs an error`() {
    seedService.seedData(SeedFileType.approvedPremises, "/afile")

    assertThat(logEntries).anyMatch {
      it.level == "error" &&
        it.message == "Unable to complete Seed Job" &&
        it.throwable != null &&
        it.throwable.message!!.contains(
          "Filename must be just the filename of a .csv file in the /seed directory, e.g. for /seed/upload.csv, just `upload` should be supplied"
        )
    }
  }

  @Test
  fun `Attempting to process a file containing backward slashes logs an error`() {
    seedService.seedData(SeedFileType.approvedPremises, "\\afile")

    assertThat(logEntries).anyMatch {
      it.level == "error" &&
        it.message == "Unable to complete Seed Job" &&
        it.throwable != null &&
        it.throwable.message!!.contains(
          "Filename must be just the filename of a .csv file in the /seed directory, e.g. for /seed/upload.csv, just `upload` should be supplied"
        )
    }
  }

  @Test
  fun `Attempting to process a non-existent file logs an error`() {
    seedService.seedData(SeedFileType.approvedPremises, "non-existent")

    assertThat(logEntries).anyMatch {
      it.level == "error" &&
        it.message == "Unable to complete Seed Job" &&
        it.throwable != null &&
        it.throwable.message!!.contains(
          "There was an issue opening the CSV file"
        )
    }
  }

  @Test
  fun `Attempting to process a malformed file logs an error`() {
    withCsv(
      "malformed",
      """
id,name,addressLine1,postcode,totalBeds,notes,probationRegionId,localAuthorityAreaId,characteristicIds,status,apCode,qCode
f5fb56ad-54ed-46e9-8c0e-c1e8581cfd5d,An AP,Address 1,PC1PC2,12,Notes,384b8abb-f335-499e-b41d-dc3d852f0761,cdd12e06-8cc7-4ae5-bcdc-23271f2492db,,status,APCODE,QCODE
,,,
      """.trimIndent()
    )

    seedService.seedData(SeedFileType.approvedPremises, "malformed")

    assertThat(logEntries).anyMatch {
      it.level == "error" &&
        it.message == "Unable to complete Seed Job" &&
        it.throwable != null &&
        it.throwable.cause!!.message == "Fields num seems to be 12 on each row, but on 2th csv row, fields num is 4."
    }
  }

  private fun withCsv(csvName: String, contents: String) {
    if (! Files.isDirectory(Path(seedFilePrefix))) {
      Files.createDirectory(Path(seedFilePrefix))
    }
    Files.writeString(Path("$seedFilePrefix/$csvName.csv"), contents, StandardOpenOption.CREATE)
  }
}

data class LogEntry(
  val message: String,
  val level: String,
  val throwable: Throwable?
)
