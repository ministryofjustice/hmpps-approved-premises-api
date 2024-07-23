package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.cas1.seed

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SeedFileType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.seed.SeedTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.CsvBuilder
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.cas1.OfflineApplicationsSeedCsvRow

@TestInstance(TestInstance.Lifecycle.PER_METHOD)
class SeedApprovedPremisesOfflineApplicationsTest : SeedTestBase() {
  @Test
  fun `An Offline Application is created for a CRN that doesn't currently have any Offline Applications`() {
    withCsv(
      "offline-applications",
      approvedPremisesSeedCsvRowsToCsv(
        listOf(
          OfflineApplicationsSeedCsvRow("NEWCRN"),
        ),
      ),
    )

    seedService.seedData(SeedFileType.approvedPremisesOfflineApplications, "offline-applications")

    assertThat(offlineApplicationRepository.findAll()).anyMatch {
      it.crn == "NEWCRN"
    }
  }

  @Test
  fun `An Offline Application is not created for a CRN that already has an Offline Application`() {
    offlineApplicationEntityFactory.produceAndPersist {
      withCrn("EXISTINGCRN")
      withService(ServiceName.approvedPremises.value)
    }

    withCsv(
      "offline-applications",
      approvedPremisesSeedCsvRowsToCsv(
        listOf(
          OfflineApplicationsSeedCsvRow("EXISTINGCRN"),
        ),
      ),
    )

    seedService.seedData(SeedFileType.approvedPremisesOfflineApplications, "offline-applications")

    val matchingOfflineApplications = offlineApplicationRepository.findAll().filter {
      it.crn == "EXISTINGCRN"
    }

    assertThat(matchingOfflineApplications.size).isEqualTo(1)
  }

  private fun approvedPremisesSeedCsvRowsToCsv(rows: List<OfflineApplicationsSeedCsvRow>): String {
    val builder = CsvBuilder()
      .withUnquotedFields(
        "crn",
      )
      .newRow()

    rows.forEach {
      builder
        .withQuotedField(it.crn)
        .newRow()
    }

    return builder.build()
  }
}
