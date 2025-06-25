package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.migration.cas1

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.data.repository.findByIdOrNull
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.MigrationJobType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.PlacementDate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAPlacementApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAPlacementRequest
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.migration.MigrationJobTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationDecision
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

class Cas1FixDatesLinkedToReallocatedPlacementRequestsJobTest : MigrationJobTestBase() {

  @Test
  fun success() {
    val placementAppWithMultipleDates = givenAPlacementApplication(
      decision = PlacementApplicationDecision.ACCEPTED,
      placementDates = listOf(
        PlacementDate(
          expectedArrival = LocalDate.of(2024, 6, 1),
          duration = 1,
        ),
        PlacementDate(
          expectedArrival = LocalDate.of(2024, 7, 1),
          duration = 2,
        ),
        PlacementDate(
          expectedArrival = LocalDate.of(2024, 8, 1),
          duration = 3,
        ),
      ),
    )

    val placementRequest1 = givenAPlacementRequest(
      placementApplication = placementAppWithMultipleDates,
      expectedArrival = LocalDate.of(2024, 6, 1),
      duration = 1,
    ).first
    val placementRequest1Reallocated = placementRequestRepository.save(
      placementRequest1.copy(id = UUID.randomUUID(), reallocatedAt = OffsetDateTime.now()),
    )

    val placementRequest2 = givenAPlacementRequest(
      placementApplication = placementAppWithMultipleDates,
      expectedArrival = LocalDate.of(2024, 7, 1),
      duration = 2,
    ).first
    val placementRequest2Reallocated = placementRequestRepository.save(
      placementRequest2.copy(id = UUID.randomUUID(), reallocatedAt = OffsetDateTime.now()),
    )

    val placementRequest3 = givenAPlacementRequest(
      placementApplication = placementAppWithMultipleDates,
      expectedArrival = LocalDate.of(2024, 8, 1),
      duration = 3,
    ).first
    val placementRequest3Reallocated = placementRequestRepository.save(
      placementRequest3.copy(id = UUID.randomUUID(), reallocatedAt = OffsetDateTime.now()),
    )

    val placementDate1 = placementAppWithMultipleDates.placementDates.first { it.duration == 1 }
    val placementDate2 = placementAppWithMultipleDates.placementDates.first { it.duration == 2 }
    val placementDate3 = placementAppWithMultipleDates.placementDates.first { it.duration == 3 }

    placementDate1.placementRequest = placementRequest1Reallocated
    placementDateRepository.save(placementDate1)

    placementDate2.placementRequest = placementRequest2Reallocated
    placementDateRepository.save(placementDate2)

    placementDate3.placementRequest = placementRequest3Reallocated
    placementDateRepository.save(placementDate3)

    migrationJobService.runMigrationJob(MigrationJobType.updateCas1FixDatesLinkedToReallocatedPlacementRequests)

    assertThat(placementDateRepository.findByIdOrNull(placementDate1.id)!!.placementRequest!!.id).isEqualTo(placementRequest1.id)
    assertThat(placementDateRepository.findByIdOrNull(placementDate2.id)!!.placementRequest!!.id).isEqualTo(placementRequest2.id)
    assertThat(placementDateRepository.findByIdOrNull(placementDate3.id)!!.placementRequest!!.id).isEqualTo(placementRequest3.id)
  }
}
