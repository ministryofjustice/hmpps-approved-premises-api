package uk.gov.justice.digital.hmpps.approvedpremisesapi.migration.cas1

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementDateRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.migration.MigrationJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.migration.MigrationLogger
import java.util.UUID

/**
 * We maintain two (redundant) links from a placement application to placement requests
 *
 * 1. Placement Application <-> Placement Request
 * 2. Placement Application <-> Placement Date <-> Placement Request
 *
 * Both (1) and (2) should refer to the same set of placement requests
 *
 * Some Placement Requests in (1) are different to those referenced by (2).
 * Investigations has shown in the case of (2) that the placement requests
 * referred to have been reallocated, and the new versions of hte placement
 * requests are correctly linked in (1). This job fixes the references in (2)
 * to match those in (1)
 */
@Service
class Cas1FixDatesLinkedToReallocatedPlacementRequestsJob(
  private val placementDateRepository: PlacementDateRepository,
  private val placementApplicationRepository: PlacementApplicationRepository,
  private val migrationLogger: MigrationLogger,
  override val shouldRunInTransaction: Boolean = true,
) : MigrationJob() {
  override fun process(pageSize: Int) {
    val placementApplicationsToFix = placementDateRepository.findPlacementAppIdsWithDatesLinkedToReallocatedPlacementRequest().toSet()

    placementApplicationsToFix.forEach {
      fixPlacementDateFk(it)
    }
  }

  private fun fixPlacementDateFk(placementAppId: UUID) {
    val placementApp = placementApplicationRepository.findByIdOrNull(placementAppId)!!
    val placementDates = placementApp.placementDates

    migrationLogger.info("Fixing placement dates for placement app $placementAppId with ${placementDates.size} dates")

    val unassignedPlacementRequests = placementApp.placementRequests.toMutableList()

    placementDates.forEach { date ->
      val toAssign = unassignedPlacementRequests
        .find { it.expectedArrival == date.expectedArrival && it.duration == date.duration }

      if (toAssign == null) {
        error("Couldn't find a placement request for placement date ${date.expectedArrival} with duration ${date.duration} linked to placement app $placementAppId")
      }

      if (toAssign.isReallocated()) {
        error("Placement request ${toAssign.id} is unexpectedly reallocated")
      }

      migrationLogger.info("Reassigning placement date ${date.id} from reallocated PR ${date.placementRequest?.id} to ${toAssign.id}")

      date.placementRequest = toAssign
      placementDateRepository.save(date)

      unassignedPlacementRequests.remove(toAssign)
    }

    if (unassignedPlacementRequests.isNotEmpty()) {
      error("Still have remaining placement requests $unassignedPlacementRequests for placement app $placementAppId")
    }
  }
}
