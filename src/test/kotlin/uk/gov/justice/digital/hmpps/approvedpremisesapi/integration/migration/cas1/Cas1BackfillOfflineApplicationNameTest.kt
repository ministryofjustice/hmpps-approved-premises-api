package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.migration.cas1

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.MigrationJobType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.CaseSummaryFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnOfflineApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.apDeliusContextAddCaseSummaryToBulkResponse
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.apDeliusContextAddListCaseSummaryToBulkResponse
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.apDeliusContextEmptyCaseSummaryToBulkResponse
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.migration.MigrationJobTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.deliuscontext.Name

class Cas1BackfillOfflineApplicationNameTest : MigrationJobTestBase() {

  @Test
  fun `should update offline application name when name is found for crn`() {
    val crn1 = "exampleCrn1"
    val application1 = givenAnOfflineApplication(crn1)

    val crn2 = "exampleCrn2"
    val application2 = givenAnOfflineApplication(crn2)

    apDeliusContextAddListCaseSummaryToBulkResponse(
      listOf(
        CaseSummaryFactory()
          .withCrn(crn1)
          .withName(Name("John", "Doe", listOf("Alfred")))
          .produce(),
        CaseSummaryFactory()
          .withCrn(crn2)
          .withName(Name("Jane", "Doe", listOf("Betty")))
          .produce(),
      ),
    )

    migrationJobService.runMigrationJob(MigrationJobType.cas1BackfillOfflineApplicationName)

    val updatedApplication1 = offlineApplicationRepository.getReferenceById(application1.id)
    Assertions.assertThat(updatedApplication1.name).isEqualTo("John Doe")

    val updatedApplication2 = offlineApplicationRepository.getReferenceById(application2.id)
    Assertions.assertThat(updatedApplication2.name).isEqualTo("Jane Doe")
  }

  @Test
  fun `should not update offline application name when name is not null`() {
    val crn = "CRN"
    val application = givenAnOfflineApplication(crn, name = "Jane Doe")

    apDeliusContextAddCaseSummaryToBulkResponse(
      CaseSummaryFactory()
        .withCrn(crn)
        .withName(Name("John", "Doe", listOf("Alfred")))
        .produce(),
    )

    migrationJobService.runMigrationJob(MigrationJobType.cas1BackfillOfflineApplicationName)

    val updatedApplication = offlineApplicationRepository.getReferenceById(application.id)
    Assertions.assertThat(updatedApplication.name).isEqualTo("Jane Doe")
  }

  @Test
  fun `should leave name as null for offline application when no summary is found for crn`() {
    val crn = "CRN"
    val application = givenAnOfflineApplication(crn)

    apDeliusContextEmptyCaseSummaryToBulkResponse(crn)

    migrationJobService.runMigrationJob(MigrationJobType.cas1BackfillOfflineApplicationName)

    val updatedApplication = offlineApplicationRepository.getReferenceById(application.id)
    Assertions.assertThat(updatedApplication.name).isNull()
  }
}
