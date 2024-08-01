package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.cas1.seed

import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SeedFileType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.seed.SeedTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.CsvBuilder
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.cas1.Cas1ApAreaEmailAddressSeedCsvRow

@TestInstance(TestInstance.Lifecycle.PER_METHOD)
class SeedCas1ApAreaEmailAddressTest : SeedTestBase() {

  @Test
  fun `Attempting to seed email with an invalid ap area identifier logs an error`() {
    withCsv(
      "invalid-ap-area-id",
      cas1ApAreaEmailAddressesCsvRowsToCsv(
        listOf(
          Cas1ApAreaEmailAddressSeedCsvRow(apAreaIdentifier = "the-invalid-id", emailAddress = "me@here.com"),
        ),
      ),
    )

    seedService.seedData(SeedFileType.approvedPremisesApAreaEmailAddresses, "invalid-ap-area-id.csv")

    Assertions.assertThat(logEntries)
      .withFailMessage("-> logEntries actually contains: $logEntries")
      .anyMatch {
        it.level == "error" &&
          it.message == "Error on row 1:" &&
          it.throwable != null &&
          it.throwable.message == "AP Area with identifier 'the-invalid-id' does not exist"
      }
  }

  @Test
  fun `Attempting to seed email with a blank email address logs an error`() {
    withCsv(
      "invalid-ap-area-id",
      cas1ApAreaEmailAddressesCsvRowsToCsv(
        listOf(
          Cas1ApAreaEmailAddressSeedCsvRow(apAreaIdentifier = "SWSC", emailAddress = "    "),
        ),
      ),
    )

    seedService.seedData(SeedFileType.approvedPremisesApAreaEmailAddresses, "invalid-ap-area-id.csv")

    assertThat(logEntries)
      .withFailMessage("-> logEntries actually contains: $logEntries")
      .anyMatch {
        it.level == "error" &&
          it.message == "Error on row 1:" &&
          it.throwable != null &&
          it.throwable.message == "Email address for 'SWSC' is blank"
      }
  }

  @Test
  fun `Attempting to seed email with missing required headers lists missing fields`() {
    withCsv(
      "missing-headers",
      "totally,wrong_headers\n" +
        "SWSC,me@here.com",
    )

    seedService.seedData(SeedFileType.approvedPremisesApAreaEmailAddresses, "missing-headers.csv")

    val expectedErrorMessage = "The headers provided: " +
      "[totally, wrong_headers] " +
      "did not include required headers: " +
      "[ap_area_identifier, email_address]"

    Assertions.assertThat(logEntries)
      .withFailMessage("-> logEntries actually contains: $logEntries")
      .anyMatch {
        it.level == "error" &&
          it.message == "Unable to complete Seed Job" &&
          it.throwable != null &&
          it.throwable.message!!.contains(expectedErrorMessage)
      }
  }

  @Test
  fun `Updating email address persists correctly`() {
    withCsv(
      "valid-csv",
      cas1ApAreaEmailAddressesCsvRowsToCsv(
        listOf(
          Cas1ApAreaEmailAddressSeedCsvRow(apAreaIdentifier = "SWSC", emailAddress = "swsc@test.com"),
          Cas1ApAreaEmailAddressSeedCsvRow(apAreaIdentifier = "Mids", emailAddress = "mids@midlands.com"),
        ),
      ),
    )

    seedService.seedData(SeedFileType.approvedPremisesApAreaEmailAddresses, "valid-csv.csv")

    assertThat(apAreaRepository.findByIdentifier("SWSC")!!.emailAddress).isEqualTo("swsc@test.com")
    assertThat(apAreaRepository.findByIdentifier("Mids")!!.emailAddress).isEqualTo("mids@midlands.com")
  }

  private fun cas1ApAreaEmailAddressesCsvRowsToCsv(rows: List<Cas1ApAreaEmailAddressSeedCsvRow>): String {
    val builder = CsvBuilder()
      .withUnquotedFields(
        "ap_area_identifier",
        "email_address",
      )
      .newRow()

    rows.forEach {
      builder
        .withQuotedField(it.apAreaIdentifier)
        .withQuotedField(it.emailAddress)
        .newRow()
    }

    return builder.build()
  }
}
