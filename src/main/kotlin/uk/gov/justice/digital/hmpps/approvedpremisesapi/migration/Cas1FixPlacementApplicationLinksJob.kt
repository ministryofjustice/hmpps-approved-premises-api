package uk.gov.justice.digital.hmpps.approvedpremisesapi.migration

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.data.repository.findByIdOrNull
import org.springframework.transaction.support.TransactionTemplate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementDateEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementRequestEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementRequestRepository
import java.util.UUID
import javax.persistence.EntityManager

/**
 * This job creates links between placement_applications and their corresponding placement_requests
 *
 * This link was only established in code in November 2023.
 *
 * Understanding this link is especially important for withdrawals functionality.
 *
 * We establish the link by matching on the expected arrival date and duration values in corresponding
 * entities
 *
 * There is one issue with this approach relates to application's that were withdrawn after
 * being accepted. Because the 'decision' property is used to record if the application is
 * ACCEPTED and if the application is WITHDRAWN, we have no way of knowing if the application
 * was ever accepted once it has been withdrawn
 */
class Cas1FixPlacementApplicationLinksJob(
  private val placementApplicationRepository: PlacementApplicationRepository,
  private val applicationRepository: ApplicationRepository,
  private val placementRequestRepository: PlacementRequestRepository,
  private val entityManager: EntityManager,
  private val transactionTemplate: TransactionTemplate,
) : MigrationJob() {
  override val shouldRunInTransaction = false
  var log: Logger = LoggerFactory.getLogger(this::class.java)

  override fun process() {
    val applicationIds = placementApplicationRepository.findApplicationsThatHaveAnAcceptedPlacementApplicationWithoutACorrespondingPlacementRequest()

    applicationIds.forEach { applicationId ->
      transactionTemplate.executeWithoutResult {
        try {
          updateApplication(UUID.fromString(applicationId))
        } catch (e: IllegalStateException) {
          log.error(e.message)
        }
      }
    }
    entityManager.clear()
  }

  fun updateApplication(applicationId: UUID) {
    val application = applicationRepository.findByIdOrNull(applicationId) as ApprovedPremisesApplicationEntity

    val unlinkedPlacementRequests = application.placementRequests
      .filter { !it.isReallocated() }
      .filter { it.placementApplication == null }
      .toMutableList()

    val placementAppsAndDate = placementApplicationRepository.findByApplication(application)
      .filter { it.isAccepted() }
      .flatMap { placementApp ->
        placementApp.placementDates.map {
          PlacementAppAndDate(
            placementApplication = placementApp,
            date = it,
            placementRequest = null,
          )
        }
     }

    val placementAppsAndDateAfterNov2023 = placementAppsAndDate
      // we only started capturing decisionMadeAt in Dec 2023
      .filter { it.placementApplication.decisionMadeAt != null }

    if(placementAppsAndDateAfterNov2023.isNotEmpty()) {
      log.error("Should not of received placement application with a decision date as this indicates it was created after we " +
        "started placement applications to placement requests. Placement applications are ${placementAppsAndDateAfterNov2023.map { describe(it) }}")
      return
    }

    log.debug("We have ${placementAppsAndDate.size} placement apps to match")

    placementAppsAndDate.forEach { placementAppAndDate ->
      val matchingPlacementRequests = unlinkedPlacementRequests.filter { placementRequest ->
        placementRequest.expectedArrival == placementAppAndDate.date.expectedArrival &&
        placementRequest.duration == placementAppAndDate.date.duration
      }

      if(matchingPlacementRequests.isEmpty()) {
        log.error("No placement request found for placement app ${describe(placementAppAndDate)}")
        return
      } else if (matchingPlacementRequests.size > 1) {
        log.error("More than one potential placement request found for placement app ${describe(placementAppAndDate)}")
        return
      } else {
        val placementRequest = matchingPlacementRequests.first()
        placementAppAndDate.placementRequest = placementRequest
        unlinkedPlacementRequests.remove(placementRequest)
        log.debug("Matched placement request ${placementRequest.id} to placement application ${placementAppAndDate.placementApplication.id}")
      }
    }

    // TODO: pretty sure this never happens as we return above
    val unresolvedPlacementAppsAndDate = placementAppsAndDate.filter { it.placementRequest == null }
    if(unresolvedPlacementAppsAndDate.isNotEmpty()) {
      log.error("Unmatched placement applications ${unresolvedPlacementAppsAndDate.map { describe(it) } }")
      return
    }

    if (application.arrivalDate != null && unlinkedPlacementRequests.size != 1) {
      log.error("Application ${application.id} has an arrival date set but after matching to placement apps, no placement requests remain")
      return
    } else if (unlinkedPlacementRequests.isNotEmpty()) {
      log.error("Application ${application.id} does not have an arrival date, yet there are unmatched placement requests")
      return
    }

    placementAppsAndDate.forEach { placementAppAndDate ->
      val placementApp = placementAppAndDate.placementApplication
      val placementRequest = placementAppAndDate.placementRequest!!
      placementRequest.placementApplication = placementApp
      placementRequestRepository.save(placementRequest)
    }
  }

  private fun describe(placementApp: PlacementAppAndDate)
    = "${placementApp.placementApplication.id} for date ${placementApp.date.expectedArrival} and duration ${placementApp.date.duration}"

  data class PlacementAppAndDate (
    val placementApplication: PlacementApplicationEntity,
    val date: PlacementDateEntity,
    var placementRequest: PlacementRequestEntity?
  )

}
