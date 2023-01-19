package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SeedFileType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SeedRequest

@TestInstance(TestInstance.Lifecycle.PER_METHOD)
class SeedScaffoldingTest : SeedTestBase() {
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
id,name,addressLine1,postcode,totalBeds,notes,probationRegion,localAuthorityArea,characteristics,status,apCode,qCode
f5fb56ad-54ed-46e9-8c0e-c1e8581cfd5d,An AP,Address 1,PC1PC2,12,Notes,North East,Middlesbrough,,status,APCODE,QCODE
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
}
