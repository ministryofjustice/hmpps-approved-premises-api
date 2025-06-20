package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.migration.cas1

import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.MigrationJobType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.PlacementDate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAPlacementApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.migration.MigrationJobTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationDecision
import java.time.LocalDate

class Cas1FlattenPlacementAppDatesJobTest : MigrationJobTestBase() {

  @Test
  fun `flatten placement apps with multiple dates`() {

    val acceptedApplicationWithMultipleDates = givenAPlacementApplication(
      decision = PlacementApplicationDecision.ACCEPTED,
      placementDates = listOf(
        PlacementDate(expectedArrival = LocalDate.of(2024, 6, 1), duration = 5),
        PlacementDate(expectedArrival = LocalDate.of(2024, 7, 1), duration = 6),
        PlacementDate(expectedArrival = LocalDate.of(2024, 8, 1), duration = 7),
      )
    )

    // TODO: link to placement requests

    migrationJobService.runMigrationJob(MigrationJobType.cas1FlattenPlacementAppsWithMultipleDates)


  }

  // TODO: test where there is no placement requests (e.g. reallocated before auth)

}