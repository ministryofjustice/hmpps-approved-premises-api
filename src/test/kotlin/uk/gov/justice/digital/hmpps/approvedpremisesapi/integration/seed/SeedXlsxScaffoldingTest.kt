package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.seed

import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.kotlinx.dataframe.api.emptyDataFrame
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SeedFromExcelFileType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SeedFromExcelRequest
import java.util.UUID

class SeedXlsxScaffoldingTest : SeedTestBase() {

  @Test
  fun `Requesting a seed from Excel operation returns 202`() {
    webTestClient.post()
      .uri("/seedFromExcel")
      .bodyValue(
        SeedFromExcelRequest(
          seedType = SeedFromExcelFileType.CAS1_IMPORT_SITE_SURVEY_ROOMS,
          premisesId = UUID.randomUUID(),
          fileName = "file.xlsx",
        ),
      )
      .exchange()
      .expectStatus()
      .isAccepted
  }

  @Test
  fun `Attempting to process an xlsx file containing forward slashes logs an error`() {
    seedXlsxService.seedExcelData(SeedFromExcelFileType.CAS1_IMPORT_SITE_SURVEY_ROOMS, UUID.randomUUID(), "/afile")

    assertThat(logEntries).anyMatch {
      it.level == "error" &&
        it.message == "Unable to complete Excel seed job" &&
        it.throwable != null &&
        it.throwable.message!!.contains(
          "Filename must be just the filename of a .xlsx file in the /seed directory, e.g. for /seed/upload.xlsx, just `upload` should be supplied",
        )
    }
  }

  @Test
  fun `Attempting to process an xlsx file containing backward slashes logs an error`() {
    seedXlsxService.seedExcelData(SeedFromExcelFileType.CAS1_IMPORT_SITE_SURVEY_ROOMS, UUID.randomUUID(), "\\afile")

    assertThat(logEntries).anyMatch {
      it.level == "error" &&
        it.message == "Unable to complete Excel seed job" &&
        it.throwable != null &&
        it.throwable.message!!.contains(
          "Filename must be just the filename of a .xlsx file in the /seed directory, e.g. for /seed/upload.xlsx, just `upload` should be supplied",
        )
    }
  }

  @Test
  fun `Attempting to process a non-existent xlsx file logs an error`() {
    seedXlsxService.seedExcelData(SeedFromExcelFileType.CAS1_IMPORT_SITE_SURVEY_ROOMS, UUID.randomUUID(), "non-existent")

    assertThat(logEntries).anyMatch {
      it.level == "error" &&
        it.message == "Unable to complete Excel seed job" &&
        it.throwable != null &&
        it.throwable.message!!.contains(
          "Unable to process XLSX file",
        )
    }
  }

  @Test
  fun `Attempting to process an xlsx file without Sheet3 logs an error`() {
    createXlsxForSeeding(
      fileName = "wrongSheetName.xlsx",
      sheets = mapOf("wrongSheetName" to emptyDataFrame<Any>()),
    )

    seedXlsxService.seedExcelData(SeedFromExcelFileType.CAS1_IMPORT_SITE_SURVEY_ROOMS, UUID.randomUUID(), "wrongSheetName.xlsx")

    assertThat(logEntries).anyMatch {
      it.level == "error" &&
        it.message == "Unable to complete Excel seed job" &&
        it.throwable != null &&
        it.throwable.cause!!.message == "Sheet with name Sheet3 not found"
    }
  }
}
