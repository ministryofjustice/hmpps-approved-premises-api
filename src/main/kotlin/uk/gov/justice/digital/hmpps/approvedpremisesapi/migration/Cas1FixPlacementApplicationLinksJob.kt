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
 * As of November 2023 this link was populated when a placement_applications was accepted, so this backfill
 * should only apply to placement_applications accepted before November 2023
 *
 * We establish the link by finding pairs of placement_requests and placement_applications on an application
 * that hav the same arrival date and duration
 *
 * Whilst this approach should 'fix' the link for most placement_applications, if a placement_application
 * has already been withdrawn there is no way to determine if it was ACCEPTED before the withdrawal occurred so these
 * can't be considered. This is because when withdrawing the placement_application.decision value is updated to
 * WITHDRAWN/WITHDRAWN_BY_PP overwriting 'ACCEPTED' (if set). This will result in an error being logged for the particular
 * application
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

    log.info("Fixing PlacementApplication to PlacementRequest relationships for ${applicationIds.size} applications")

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

  @SuppressWarnings("ReturnCount")
  fun updateApplication(applicationId: UUID) {
    log.info("Fixing PlacementApplication to PlacementRequest relationships for Application $applicationId")

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

    val placementAppsWithDecisionMadeDateSet = placementAppsAndDate
      // we only started capturing decisionMadeAt in Dec 2023
      .filter { it.placementApplication.decisionMadeAt != null }

    if (placementAppsWithDecisionMadeDateSet.isNotEmpty()) {
      log.error(
        "We should not be considering PlacementApplications with a non-null decisionMadeAt, because this was " +
          "only set for decisions made after linking PlacementApplications to PlacementRequests was automatically " +
          "managed in code. Placement applications are ${placementAppsWithDecisionMadeDateSet.map { describe(it) }}",
      )
      return
    }

    log.info("We have ${placementAppsAndDate.size} PlacementApplications to match for application $applicationId")

    placementAppsAndDate.forEach { placementAppAndDate ->
      val matchingPlacementRequests = unlinkedPlacementRequests.filter { placementRequest ->
        placementRequest.expectedArrival == placementAppAndDate.date.expectedArrival &&
          placementRequest.duration == placementAppAndDate.date.duration
      }

      if (matchingPlacementRequests.isEmpty()) {
        log.error("No placement request found for placement app ${describe(placementAppAndDate)} for application $applicationId")
        return
      } else if (matchingPlacementRequests.size > 1) {
        log.error("More than one potential placement request found for placement app ${describe(placementAppAndDate)} for application $applicationId")
        return
      } else {
        val placementRequest = matchingPlacementRequests.first()
        placementAppAndDate.placementRequest = placementRequest
        unlinkedPlacementRequests.remove(placementRequest)
      }
    }

    val applicationArrivalDate = application.arrivalDate?.toLocalDate()
    if (applicationArrivalDate != null) {
      if (unlinkedPlacementRequests.size != 1) {
        log.error(
          "Application ${application.id} has an arrival date set on the initial application," +
            " but after matching to placement apps, no placement requests remain that represents this date",
        )
        return
      } else {
        val remainingPlacementRequestDate = unlinkedPlacementRequests[0].expectedArrival
        if (applicationArrivalDate != remainingPlacementRequestDate) {
          log.error(
            "Application ${application.id} has an arrival date set on the initial application, but after matching to placement apps," +
              " the only remaining placement request doesn't have the expected arrival date " +
              "(expected $applicationArrivalDate was $remainingPlacementRequestDate)",
          )
          return
        }
      }
    } else if (unlinkedPlacementRequests.isNotEmpty()) {
      log.error("Application ${application.id} does not have an arrival date set on the initial application, yet there are unmatched placement requests.")
      return
    }

    placementAppsAndDate.forEach { placementAppAndDate ->
      val placementApp = placementAppAndDate.placementApplication
      val placementRequest = placementAppAndDate.placementRequest!!
      placementRequest.placementApplication = placementApp
      log.info("Linked PlacementRequest ${placementRequest.id} to PlacementApplication ${placementAppAndDate.placementApplication.id}")
      placementRequestRepository.save(placementRequest)
    }
  }

  private fun describe(placementApp: PlacementAppAndDate) =
    "${placementApp.placementApplication.id} for date ${placementApp.date.expectedArrival} and duration ${placementApp.date.duration}"

  data class PlacementAppAndDate(
    val placementApplication: PlacementApplicationEntity,
    val date: PlacementDateEntity,
    var placementRequest: PlacementRequestEntity?,
  )
}
