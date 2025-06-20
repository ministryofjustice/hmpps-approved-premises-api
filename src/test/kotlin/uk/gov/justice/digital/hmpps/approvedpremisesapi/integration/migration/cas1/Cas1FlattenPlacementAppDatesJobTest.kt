package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.migration.cas1

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.MigrationJobType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.PlacementDate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAPlacementApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAPlacementRequest
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.migration.MigrationJobTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationDecision
import java.time.LocalDate

class Cas1FlattenPlacementAppDatesJobTest : MigrationJobTestBase() {

  @Test
  fun `flatten placement apps with multiple dates and linked placement requests`() {
    val placementAppWithOneDate = givenAPlacementApplication(
      decision = PlacementApplicationDecision.ACCEPTED,
      placementDates = listOf(
        PlacementDate(
          expectedArrival = LocalDate.of(2021, 2, 3),
          duration = 3,
        ),
      ),
    )
    val placementRequest0 = givenAPlacementRequest(placementApplication = placementAppWithOneDate).first
    val placementDate0 = placementAppWithOneDate.placementDates[0]
    placementDate0.placementRequest = placementRequest0
    placementDateRepository.save(placementDate0)

    val placementAppWithMultipleDates = givenAPlacementApplication(
      decision = PlacementApplicationDecision.ACCEPTED,
      placementDates = listOf(
        PlacementDate(
          expectedArrival = LocalDate.of(2024, 6, 1),
          duration = 5,
        ),
        PlacementDate(
          expectedArrival = LocalDate.of(2024, 7, 1),
          duration = 6,
        ),
        PlacementDate(
          expectedArrival = LocalDate.of(2024, 8, 1),
          duration = 7,
        ),
      ),
    )

    val placementRequest1 = givenAPlacementRequest(placementApplication = placementAppWithMultipleDates).first
    val placementRequest2 = givenAPlacementRequest(placementApplication = placementAppWithMultipleDates).first
    val placementRequest3 = givenAPlacementRequest(placementApplication = placementAppWithMultipleDates).first

    val placementDate1 = placementAppWithMultipleDates.placementDates.first { it.duration == 5 }
    val placementDate2 = placementAppWithMultipleDates.placementDates.first { it.duration == 6 }
    val placementDate3 = placementAppWithMultipleDates.placementDates.first { it.duration == 7 }

    placementDate1.placementRequest = placementRequest1
    placementDateRepository.save(placementDate1)

    placementDate2.placementRequest = placementRequest2
    placementDateRepository.save(placementDate2)

    placementDate3.placementRequest = placementRequest3
    placementDateRepository.save(placementDate3)

    assertThat(placementApplicationRepository.findAll()).hasSize(2)

    migrationJobService.runMigrationJob(MigrationJobType.cas1FlattenPlacementAppsWithMultipleDates)

    val updatedPlacementApps = placementApplicationRepository.findAll()

    assertThat(updatedPlacementApps).hasSize(4)

    updatedPlacementApps.filter { it.id != placementAppWithOneDate.id }.forEach {
      assertThat(it.placementRequests).hasSize(1)
      assertThat(it.placementDates).hasSize(1)
      assertThat(it.application.id).isEqualTo(placementAppWithMultipleDates.application.id)
      assertThat(it.createdByUser.id).isEqualTo(placementAppWithMultipleDates.createdByUser.id)
      assertThat(it.data).isEqualTo(placementAppWithMultipleDates.data)
      assertThat(it.document).isEqualTo(placementAppWithMultipleDates.document)
      assertThat(it.createdAt).isEqualTo(placementAppWithMultipleDates.createdAt)
      assertThat(it.submittedAt).isEqualTo(placementAppWithMultipleDates.submittedAt)
      assertThat(it.decision).isEqualTo(placementAppWithMultipleDates.decision)
      assertThat(it.decisionMadeAt).isEqualTo(placementAppWithMultipleDates.decisionMadeAt)
      assertThat(it.placementType).isEqualTo(placementAppWithMultipleDates.placementType)
      assertThat(it.submissionGroupId).isEqualTo(placementAppWithMultipleDates.submissionGroupId)
    }

    val updatedPlacementAppWithOneDate = updatedPlacementApps.first { it.id == placementAppWithOneDate.id }
    val updatedPlacementApp1 = updatedPlacementApps.first { it.placementDates[0].duration == 5 }
    val updatedPlacementApp2 = updatedPlacementApps.first { it.placementDates[0].duration == 6 }
    val updatedPlacementApp3 = updatedPlacementApps.first { it.placementDates[0].duration == 7 }

    assertThat(updatedPlacementAppWithOneDate.placementRequests[0].id).isEqualTo(placementRequest0.id)
    assertThat(updatedPlacementAppWithOneDate.placementDates[0].id).isEqualTo(placementDate0.id)
    assertThat(updatedPlacementAppWithOneDate.placementDates[0].expectedArrival).isEqualTo(LocalDate.of(2021, 2, 3))
    assertThat(updatedPlacementAppWithOneDate.placementDates[0].duration).isEqualTo(3)
    assertThat(updatedPlacementAppWithOneDate.placementDates[0].placementRequest!!.id).isEqualTo(placementRequest0.id)

    assertThat(updatedPlacementApp1.placementRequests[0].id).isEqualTo(placementRequest1.id)
    assertThat(updatedPlacementApp1.placementDates[0].id).isEqualTo(placementDate1.id)
    assertThat(updatedPlacementApp1.placementDates[0].expectedArrival).isEqualTo(LocalDate.of(2024, 6, 1))
    assertThat(updatedPlacementApp1.placementDates[0].duration).isEqualTo(5)
    assertThat(updatedPlacementApp1.placementDates[0].placementRequest!!.id).isEqualTo(placementRequest1.id)

    assertThat(updatedPlacementApp2.placementRequests[0].id).isEqualTo(placementRequest2.id)
    assertThat(updatedPlacementApp2.placementDates[0].id).isEqualTo(placementDate2.id)
    assertThat(updatedPlacementApp2.placementDates[0].expectedArrival).isEqualTo(LocalDate.of(2024, 7, 1))
    assertThat(updatedPlacementApp2.placementDates[0].duration).isEqualTo(6)
    assertThat(updatedPlacementApp2.placementDates[0].placementRequest!!.id).isEqualTo(placementRequest2.id)

    assertThat(updatedPlacementApp3.placementRequests[0].id).isEqualTo(placementRequest3.id)
    assertThat(updatedPlacementApp3.placementDates[0].id).isEqualTo(placementDate3.id)
    assertThat(updatedPlacementApp3.placementDates[0].expectedArrival).isEqualTo(LocalDate.of(2024, 8, 1))
    assertThat(updatedPlacementApp3.placementDates[0].duration).isEqualTo(7)
    assertThat(updatedPlacementApp3.placementDates[0].placementRequest!!.id).isEqualTo(placementRequest3.id)
  }

  @Test
  fun `flatten placement apps with multiple dates and no linked placement requests`() {
    val placementAppWithOneDate = givenAPlacementApplication(
      decision = PlacementApplicationDecision.ACCEPTED,
      placementDates = listOf(
        PlacementDate(
          expectedArrival = LocalDate.of(2021, 2, 3),
          duration = 3,
        ),
      ),
    )
    val placementRequest0 = givenAPlacementRequest(placementApplication = placementAppWithOneDate).first
    val placementDate0 = placementAppWithOneDate.placementDates[0]
    placementDate0.placementRequest = placementRequest0
    placementDateRepository.save(placementDate0)

    val placementAppWithMultipleDates = givenAPlacementApplication(
      decision = PlacementApplicationDecision.REJECTED,
      placementDates = listOf(
        PlacementDate(
          expectedArrival = LocalDate.of(2024, 6, 1),
          duration = 5,
          placementRequest = null,
        ),
        PlacementDate(
          expectedArrival = LocalDate.of(2024, 7, 1),
          duration = 6,
          placementRequest = null,
        ),
        PlacementDate(
          expectedArrival = LocalDate.of(2024, 8, 1),
          duration = 7,
          placementRequest = null,
        ),
      ),
    )

    val placementDate1 = placementAppWithMultipleDates.placementDates.first { it.duration == 5 }
    val placementDate2 = placementAppWithMultipleDates.placementDates.first { it.duration == 6 }
    val placementDate3 = placementAppWithMultipleDates.placementDates.first { it.duration == 7 }

    assertThat(placementApplicationRepository.findAll()).hasSize(2)

    migrationJobService.runMigrationJob(MigrationJobType.cas1FlattenPlacementAppsWithMultipleDates)

    val updatedPlacementApps = placementApplicationRepository.findAll()

    assertThat(updatedPlacementApps).hasSize(4)

    updatedPlacementApps.filter { it.id != placementAppWithOneDate.id }.forEach {
      assertThat(it.placementRequests).isEmpty()
      assertThat(it.placementDates).hasSize(1)
      assertThat(it.application.id).isEqualTo(placementAppWithMultipleDates.application.id)
      assertThat(it.createdByUser.id).isEqualTo(placementAppWithMultipleDates.createdByUser.id)
      assertThat(it.data).isEqualTo(placementAppWithMultipleDates.data)
      assertThat(it.document).isEqualTo(placementAppWithMultipleDates.document)
      assertThat(it.createdAt).isEqualTo(placementAppWithMultipleDates.createdAt)
      assertThat(it.submittedAt).isEqualTo(placementAppWithMultipleDates.submittedAt)
      assertThat(it.decision).isEqualTo(placementAppWithMultipleDates.decision)
      assertThat(it.decisionMadeAt).isEqualTo(placementAppWithMultipleDates.decisionMadeAt)
      assertThat(it.placementType).isEqualTo(placementAppWithMultipleDates.placementType)
      assertThat(it.submissionGroupId).isEqualTo(placementAppWithMultipleDates.submissionGroupId)
    }

    val updatedPlacementAppWithOneDate = updatedPlacementApps.first { it.id == placementAppWithOneDate.id }
    val updatedPlacementApp1 = updatedPlacementApps.first { it.placementDates[0].duration == 5 }
    val updatedPlacementApp2 = updatedPlacementApps.first { it.placementDates[0].duration == 6 }
    val updatedPlacementApp3 = updatedPlacementApps.first { it.placementDates[0].duration == 7 }

    assertThat(updatedPlacementAppWithOneDate.placementRequests[0].id).isEqualTo(placementRequest0.id)
    assertThat(updatedPlacementAppWithOneDate.placementDates[0].id).isEqualTo(placementDate0.id)
    assertThat(updatedPlacementAppWithOneDate.placementDates[0].expectedArrival).isEqualTo(LocalDate.of(2021, 2, 3))
    assertThat(updatedPlacementAppWithOneDate.placementDates[0].duration).isEqualTo(3)
    assertThat(updatedPlacementAppWithOneDate.placementDates[0].placementRequest!!.id).isEqualTo(placementRequest0.id)

    assertThat(updatedPlacementApp1.placementRequests).isEmpty()
    assertThat(updatedPlacementApp1.placementDates[0].id).isEqualTo(placementDate1.id)
    assertThat(updatedPlacementApp1.placementDates[0].expectedArrival).isEqualTo(LocalDate.of(2024, 6, 1))
    assertThat(updatedPlacementApp1.placementDates[0].duration).isEqualTo(5)
    assertThat(updatedPlacementApp1.placementDates[0].placementRequest).isNull()

    assertThat(updatedPlacementApp2.placementRequests).isEmpty()
    assertThat(updatedPlacementApp2.placementDates[0].id).isEqualTo(placementDate2.id)
    assertThat(updatedPlacementApp2.placementDates[0].expectedArrival).isEqualTo(LocalDate.of(2024, 7, 1))
    assertThat(updatedPlacementApp2.placementDates[0].duration).isEqualTo(6)
    assertThat(updatedPlacementApp2.placementDates[0].placementRequest).isNull()

    assertThat(updatedPlacementApp3.placementRequests).isEmpty()
    assertThat(updatedPlacementApp3.placementDates[0].id).isEqualTo(placementDate3.id)
    assertThat(updatedPlacementApp3.placementDates[0].expectedArrival).isEqualTo(LocalDate.of(2024, 8, 1))
    assertThat(updatedPlacementApp3.placementDates[0].duration).isEqualTo(7)
    assertThat(updatedPlacementApp3.placementDates[0].placementRequest).isNull()
  }
}
