package uk.gov.justice.digital.hmpps.approvedpremisesapi.migration.cas1

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BookingNotMadeRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementRequestEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementRequestRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.migration.MigrationJob

@Component
class Cas1BookingNotMadeReallocationJob(
  val placementRequestRepository: PlacementRequestRepository,
  val bookingNotMadeRepository: BookingNotMadeRepository,
) : MigrationJob() {
  private val log = LoggerFactory.getLogger(this::class.java)
  override val shouldRunInTransaction = true
  override fun process(pageSize: Int) {
    val toUpdate = placementRequestRepository.reallocatedAtWithBookingNotMade()

    log.info("Have found ${toUpdate.size} reallocated placement requests with one or more booking not made records")

    toUpdate.forEach { moveBookingNotMades(it) }

    log.info("Placement requests updated")
  }

  private fun moveBookingNotMades(placementRequest: PlacementRequestEntity) {
    val id = placementRequest.id
    val expectedArrival = placementRequest.expectedArrival
    val duration = placementRequest.duration

    log.info(
      "Looking to move ${placementRequest.bookingNotMades.size} booking not mades for " +
        "placement request $id with arrival date $expectedArrival and duration $duration",
    )

    val nonReallocatedEquivalent = findNonReallocatedEquivalent(placementRequest)

    placementRequest.bookingNotMades.forEach {
      log.info("Moving booking not made ${it.id} from placement request $id to ${nonReallocatedEquivalent.id}")
      it.placementRequest = nonReallocatedEquivalent
      bookingNotMadeRepository.save(it)
    }
  }

  private fun findNonReallocatedEquivalent(placementRequest: PlacementRequestEntity): PlacementRequestEntity {
    val id = placementRequest.id
    val expectedArrival = placementRequest.expectedArrival
    val duration = placementRequest.duration

    val allPlacementRequests = placementRequest.application.placementRequests
    val nonReallocatedEquivalents = allPlacementRequests
      .filter { it.reallocatedAt == null }
      .filter { it.expectedArrival == expectedArrival && it.duration == duration }

    if (nonReallocatedEquivalents.isEmpty()) {
      error("Could not find an equivalent non-reallocated placement request for $id. This should not happen")
    }

    if (nonReallocatedEquivalents.size > 1) {
      error("Found multiple non-reallocated placement request for $id. Not sure what to do")
    }

    return nonReallocatedEquivalents.first()
  }
}
