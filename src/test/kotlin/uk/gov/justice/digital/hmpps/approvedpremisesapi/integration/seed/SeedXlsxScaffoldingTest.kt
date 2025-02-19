package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.seed

import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.kotlinx.dataframe.api.emptyDataFrame
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SeedFromExcelDirectoryRequest
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SeedFromExcelFileRequest
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SeedFromExcelFileType

class SeedXlsxScaffoldingTest : SeedTestBase() {

  @Nested
  inner class SeedDirectory {
    @Test
    fun success() {
      webTestClient.post()
        .uri("/seedFromExcel/directory")
        .bodyValue(
          SeedFromExcelDirectoryRequest(
            seedType = SeedFromExcelFileType.CAS1_IMPORT_SITE_SURVEY_ROOMS,
            directoryName = "my-dir",
          ),
        )
        .exchange()
        .expectStatus()
        .isAccepted
    }

    @Test
    fun `Attempting to process a directory containing forward slashes logs an error`() {
      seedXlsxService.seedDirectoryRecursive(SeedFromExcelFileType.CAS1_IMPORT_SITE_SURVEY_ROOMS, "/afile")

      assertThat(logEntries).anyMatch {
        it.level == "error" &&
          it.message == "Invalid directory /afile. Should be the name of a file within ./test-seed-csvs. Sub directories are not allowed"
      }
    }

    @Test
    fun `Attempting to process a directory containing backward slashes logs an error`() {
      seedXlsxService.seedDirectoryRecursive(SeedFromExcelFileType.CAS1_IMPORT_SITE_SURVEY_ROOMS, "\\afile")

      assertThat(logEntries).anyMatch {
        it.level == "error" &&
          it.message == "Invalid directory \\afile. Should be the name of a file within ./test-seed-csvs. Sub directories are not allowed"
      }
    }

    @Test
    fun `Attempting to process a non-existent directory logs an error`() {
      seedXlsxService.seedDirectoryRecursive(SeedFromExcelFileType.CAS1_IMPORT_SITE_SURVEY_ROOMS, "non-existent")

      assertThat(logEntries).anyMatch {
        it.level == "error" &&
          it.message.contains("Cannot find directory 'non-existent' within ./test-seed-csvs")
      }
    }
  }

  @Nested
  inner class SeedFile {

    @Test
    fun success() {
      webTestClient.post()
        .uri("/seedFromExcel/file")
        .bodyValue(
          SeedFromExcelFileRequest(
            seedType = SeedFromExcelFileType.CAS1_IMPORT_SITE_SURVEY_ROOMS,
            fileName = "file.xlsx",
          ),
        )
        .exchange()
        .expectStatus()
        .isAccepted
    }

    @Test
    fun `Attempting to process an xlsx file containing forward slashes logs an error`() {
      seedXlsxService.seedFile(SeedFromExcelFileType.CAS1_IMPORT_SITE_SURVEY_ROOMS, "/afile")

      assertThat(logEntries).anyMatch {
        it.level == "error" &&
          it.message == "Invalid filename /afile. Should be the name of a file within ./test-seed-csvs. Sub directories are not allowed"
      }
    }

    @Test
    fun `Attempting to process an xlsx file containing backward slashes logs an error`() {
      seedXlsxService.seedFile(SeedFromExcelFileType.CAS1_IMPORT_SITE_SURVEY_ROOMS, "\\afile")

      assertThat(logEntries).anyMatch {
        it.level == "error" &&
          it.message == "Invalid filename \\afile. Should be the name of a file within ./test-seed-csvs. Sub directories are not allowed"
      }
    }

    @Test
    fun `Attempting to process a non-existent xlsx file logs an error`() {
      seedXlsxService.seedFile(SeedFromExcelFileType.CAS1_IMPORT_SITE_SURVEY_ROOMS, "non-existent.xlsx")

      assertThat(logEntries).anyMatch {
        it.level == "error" &&
          it.message.contains("Unable to complete Excel seed job for 'non-existent.xlsx' with message")
      }
    }

    @Test
    fun `Attempting to process an xlsx file without Sheet3 logs an error`() {
      createXlsxForSeeding(
        fileName = "wrongSheetName.xlsx",
        sheets = mapOf("wrongSheetName" to emptyDataFrame<Any>()),
      )

      seedXlsxService.seedFile(SeedFromExcelFileType.CAS1_IMPORT_SITE_SURVEY_PREMISES, "wrongSheetName.xlsx")

      assertThat(logEntries).anyMatch {
        it.level == "error" &&
          it.message == "Unable to complete Excel seed job for 'wrongSheetName.xlsx' with message 'Sheet with name Sheet2 not found'"
      }
    }
  }
}
