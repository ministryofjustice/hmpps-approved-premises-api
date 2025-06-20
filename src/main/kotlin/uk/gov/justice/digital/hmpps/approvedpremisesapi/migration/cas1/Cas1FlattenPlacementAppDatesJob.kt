package uk.gov.justice.digital.hmpps.approvedpremisesapi.migration.cas1

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementDateEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementDateRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementRequestRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.migration.MigrationJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.migration.MigrationLogger
import java.util.UUID

@Service
class Cas1FlattenPlacementAppDatesJob(
  private val placementApplicationRepository: PlacementApplicationRepository,
  private val placementDateRepository: PlacementDateRepository,
  private val placementRequestRepository: PlacementRequestRepository,
  private val migrationLogger: MigrationLogger,
  override val shouldRunInTransaction: Boolean = true,
) : MigrationJob() {
  override fun process(pageSize: Int) {
    val placementApplications = placementApplicationRepository.findAllWithMultipleDates();

    migrationLogger.info("There are ${placementApplications.size} placement applications with multiple dates.")

    placementApplications.forEach { flattenPlacementApp(it) }

    // 3. TODO: do a summary of things updated. make sure we log out sufficient info to revert if
    // for some reason we need to

  }

  private fun flattenPlacementApp(id: UUID) {

    val originalPlacementApp = placementApplicationRepository.findByIdOrNull(id)!!
    val dates = originalPlacementApp.placementDates.sortedBy { it.expectedArrival }

    migrationLogger.info("Flattening placement application $id with ${originalPlacementApp.placementDates.size} dates and decision ${originalPlacementApp.decision}")

    val retainedDate = dates[0]
    migrationLogger.info("Leaving date ${retainedDate.expectedArrival} (${retainedDate.id}) with placement request ${retainedDate.placementRequest?.id}" +
      " linked to original application ${originalPlacementApp.id}")

    // the first date remains attached to the original placement application
    // all others will be assigned to a new placement application
    val datesToMoveToNewPlacementApplication = dates.subList(1, originalPlacementApp.placementDates.size)

    datesToMoveToNewPlacementApplication.map {
      createForDate(originalPlacementApp, it)
    }
  }

  private fun createForDate(
    originalPlacementApp: PlacementApplicationEntity,
    date: PlacementDateEntity,
  ) {
    val newPlacementApp = placementApplicationRepository.save(
      originalPlacementApp.copy(
        id = UUID.randomUUID(),
        placementDates = mutableListOf(),
        placementRequests = mutableListOf(),
      )
    )

    date.placementApplication = newPlacementApp
    placementDateRepository.save(date)

    date.placementRequest?.let { placementRequest ->
      placementRequest.placementApplication = newPlacementApp
      placementRequestRepository.save(placementRequest)
    }

    migrationLogger.info("Have created placement app ${newPlacementApp.id} from placement app ${originalPlacementApp.id} for date ${date.expectedArrival} (${date.id}), " +
      "linked to placement request ${newPlacementApp.placementRequests.map { it.id }}")
  }
}