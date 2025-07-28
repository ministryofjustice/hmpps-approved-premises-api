package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.migration.cas1

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.data.repository.findByIdOrNull
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.MigrationJobType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenABookingNotMade
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenACas1Application
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAPlacementRequest
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.migration.MigrationJobTestBase
import java.time.LocalDate
import java.time.OffsetDateTime

class Cas1BookingNotMadeReallocationJobTest : MigrationJobTestBase() {

  @Test
  fun success() {
    val application = givenACas1Application()

    val reallocated1 = givenAPlacementRequest(
      application = application,
      reallocated = true,
      expectedArrival = LocalDate.now().minusDays(1),
      duration = 5,
    ).first
    val bnmToMove1 = givenABookingNotMade(reallocated1)
    val bnmToMove2 = givenABookingNotMade(reallocated1)

    val reallocated2 = givenAPlacementRequest(
      application = application,
      reallocated = true,
      expectedArrival = LocalDate.now().minusDays(1),
      duration = 5,
    ).first
    val bnmToMove3 = givenABookingNotMade(reallocated2)

    val nonReallocatedSlightlyDifferentDate = givenAPlacementRequest(
      application = application,
      reallocated = false,
      expectedArrival = LocalDate.now().minusDays(2),
      duration = 5,
    ).first
    val unaffectedBnm1 = givenABookingNotMade(nonReallocatedSlightlyDifferentDate)

    val nonReallocatedSlightlyDifferentDuration = givenAPlacementRequest(
      application = application,
      reallocated = false,
      expectedArrival = LocalDate.now().minusDays(1),
      duration = 6,
    ).first
    val unaffectedBnm2 = givenABookingNotMade(nonReallocatedSlightlyDifferentDuration)

    val notReallocatedMatching = givenAPlacementRequest(
      application = application,
      reallocated = false,
      expectedArrival = LocalDate.now().minusDays(1),
      duration = 5,
    ).first

    givenAPlacementRequest(
      application = application,
      reallocated = true,
      expectedArrival = LocalDate.now().minusDays(1),
      duration = 5,
    )

    migrationJobService.runMigrationJob(MigrationJobType.cas1BookingNotMadeReallocation)

    assertThat(placementRequestRepository.findByIdOrNull(reallocated1.id)!!.bookingNotMades).isEmpty()
    assertThat(placementRequestRepository.findByIdOrNull(reallocated2.id)!!.bookingNotMades).isEmpty()
    assertThat(placementRequestRepository.findByIdOrNull(notReallocatedMatching.id)!!.bookingNotMades.map { it.id })
      .containsExactlyInAnyOrder(bnmToMove1.id, bnmToMove2.id, bnmToMove3.id)

    assertThat(bookingNotMadeRepository.findByIdOrNull(unaffectedBnm1.id)!!.placementRequest.id).isEqualTo(nonReallocatedSlightlyDifferentDate.id)
    assertThat(bookingNotMadeRepository.findByIdOrNull(unaffectedBnm2.id)!!.placementRequest.id).isEqualTo(nonReallocatedSlightlyDifferentDuration.id)
  }

  @Test
  fun `if multiple candidates and all withdrawn, pick latest`() {
    val application = givenACas1Application()

    val reallocated1 = givenAPlacementRequest(
      application = application,
      reallocated = true,
      expectedArrival = LocalDate.now().minusDays(1),
      duration = 5,
    ).first
    val bnmToMove1 = givenABookingNotMade(reallocated1)

    // matching, but older
    givenAPlacementRequest(
      application = application,
      reallocated = false,
      expectedArrival = LocalDate.now().minusDays(1),
      duration = 5,
      isWithdrawn = true,
      createdAt = OffsetDateTime.now().minusDays(5),
    ).first

    val notReallocatedMatching2 = givenAPlacementRequest(
      application = application,
      reallocated = false,
      expectedArrival = LocalDate.now().minusDays(1),
      duration = 5,
      isWithdrawn = true,
      createdAt = OffsetDateTime.now().minusDays(4),
    ).first

    migrationJobService.runMigrationJob(MigrationJobType.cas1BookingNotMadeReallocation)

    assertThat(placementRequestRepository.findByIdOrNull(reallocated1.id)!!.bookingNotMades).isEmpty()
    assertThat(placementRequestRepository.findByIdOrNull(notReallocatedMatching2.id)!!.bookingNotMades.map { it.id })
      .containsExactlyInAnyOrder(bnmToMove1.id)
  }

  @Test
  fun `if multiple candidates and all but one withdrawn, pick non withdrawn version`() {
    val application = givenACas1Application()

    val reallocated1 = givenAPlacementRequest(
      application = application,
      reallocated = true,
      expectedArrival = LocalDate.now().minusDays(1),
      duration = 5,
    ).first
    val bnmToMove1 = givenABookingNotMade(reallocated1)

    // matching, but withdrawn
    givenAPlacementRequest(
      application = application,
      reallocated = false,
      expectedArrival = LocalDate.now().minusDays(1),
      duration = 5,
      isWithdrawn = true,
    ).first

    val notReallocatedMatchingNotWithdrawn = givenAPlacementRequest(
      application = application,
      reallocated = false,
      expectedArrival = LocalDate.now().minusDays(1),
      duration = 5,
      isWithdrawn = false,
    ).first

    migrationJobService.runMigrationJob(MigrationJobType.cas1BookingNotMadeReallocation)

    assertThat(placementRequestRepository.findByIdOrNull(reallocated1.id)!!.bookingNotMades).isEmpty()
    assertThat(placementRequestRepository.findByIdOrNull(notReallocatedMatchingNotWithdrawn.id)!!.bookingNotMades.map { it.id })
      .containsExactlyInAnyOrder(bnmToMove1.id)
  }
}
