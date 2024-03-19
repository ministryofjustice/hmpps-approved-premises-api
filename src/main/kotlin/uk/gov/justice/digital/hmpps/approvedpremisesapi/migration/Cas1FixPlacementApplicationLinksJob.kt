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
    applyManualFixes()
    applyAutomatedFixes()

    entityManager.clear()
  }

  private fun applyManualFixes() {
    log.info("Applying manual link fixes for ${manualLinkFixes.size} fixes")

    manualLinkFixes.forEach { fix ->
      try {
        transactionTemplate.executeWithoutResult {
          applyManualFix(fix)
        }
      } catch (e: IllegalStateException) {
        log.error(e.message)
      }
    }
  }

  private fun applyAutomatedFixes() {
    val applicationIds = placementApplicationRepository.findApplicationsThatHaveAnAcceptedPlacementApplicationWithoutACorrespondingPlacementRequest()

    log.info("Automatically Fixing PlacementApplication to PlacementRequest relationships for ${applicationIds.size} applications")

    applicationIds.forEach { applicationId ->
      try {
        transactionTemplate.executeWithoutResult {
          applyAutomatedFixes(UUID.fromString(applicationId))
        }
      } catch (e: IllegalStateException) {
        log.error(e.message)
      }
    }
  }

  @SuppressWarnings("ReturnCount")
  fun applyManualFix(manualLinkFix: ManualLinkFix) {
    val application = applicationRepository.findByIdOrNull(manualLinkFix.applicationId)
    if (application == null) {
      log.error("Could not find application for id ${manualLinkFix.applicationId} when applying manual fix")
      return
    }

    log.info("Applying manual fix to application ${application.id} (application created ${application.createdAt})")

    val approvedPremisesApplicationEntity = application as ApprovedPremisesApplicationEntity
    val placementRequest = approvedPremisesApplicationEntity
      .placementRequests
      .firstOrNull { it.id == manualLinkFix.placementRequestId }

    if (placementRequest == null) {
      log.error("Could not find placement request for id ${manualLinkFix.placementRequestId} when applying manual fix")
      return
    }

    val placementApplication = placementApplicationRepository.findByIdOrNull(manualLinkFix.placementApplicationId)

    if (placementApplication == null) {
      log.error("Could not find placement application for id ${manualLinkFix.placementApplicationId} when applying manual fix")
      return
    }

    if (placementApplication.application.id != application.id) {
      log.error("Placement application ${manualLinkFix.placementApplicationId} does not apply to application ${manualLinkFix.applicationId}")
      return
    }

    linkPlacementAppToRequest(
      placementApplication = placementApplication,
      placementRequest = placementRequest,
    )
  }

  @SuppressWarnings("ReturnCount")
  fun applyAutomatedFixes(applicationId: UUID) {
    val application = applicationRepository.findByIdOrNull(applicationId) as ApprovedPremisesApplicationEntity

    log.info(
      "Fixing PlacementApplication to PlacementRequest relationships for Application $applicationId " +
        "(application created ${application.createdAt})",
    )

    val unlinkedPlacementRequests = application.placementRequests
      .filter { !it.isReallocated() }
      .filter { it.placementApplication == null }
      .toMutableList()

    val placementAppsAndDate = placementApplicationRepository.findByApplication(application)
      .filter { it.placementRequests.isEmpty() }
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

    if (placementAppsWithDecisionMadeDateSet.size == placementAppsAndDate.size) {
      log.error(
        "We should not be considering applications where all PlacementApplications have a non-null decisionMadeAt, because this was " +
          "only set for decisions made after linking PlacementApplications to PlacementRequests was automatically " +
          "managed in code. This suggests an error in the migration logic. " +
          "Placement applications are ${placementAppsWithDecisionMadeDateSet.map { describe(it) }}",
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
      linkPlacementAppToRequest(
        placementApplication = placementAppAndDate.placementApplication,
        placementRequest = placementAppAndDate.placementRequest!!,
      )
    }
  }

  private fun linkPlacementAppToRequest(
    placementApplication: PlacementApplicationEntity,
    placementRequest: PlacementRequestEntity,
  ) {
    placementRequest.placementApplication = placementApplication
    log.info("Linked PlacementRequest ${placementRequest.id} to PlacementApplication ${placementApplication.id}")
    placementRequestRepository.save(placementRequest)
  }

  private fun describe(placementApp: PlacementAppAndDate) =
    "${placementApp.placementApplication.id} for date ${placementApp.date.expectedArrival} and duration ${placementApp.date.duration}"

  data class PlacementAppAndDate(
    val placementApplication: PlacementApplicationEntity,
    val date: PlacementDateEntity,
    var placementRequest: PlacementRequestEntity?,
  )

  data class ManualLinkFix(
    val applicationId: UUID,
    val placementRequestId: UUID,
    val placementApplicationId: UUID,
  )

  // Some links could not be fixed automatically by this migration job. After manual (human) analysis
  // several of these relationships should be determined.
  // See https://dsdmoj.atlassian.net/wiki/spaces/AP/pages/4781965318/CAS-1+PlacementApplication+to+PlacementRequest+link+Backfill
  val manualLinkFixes = listOf(
    ManualLinkFix(
      applicationId = UUID.fromString("fcba419e-7802-4c05-947f-7b24988aa795"),
      placementRequestId = UUID.fromString("af242c35-07ff-4545-839a-b4b89e735e63"),
      placementApplicationId = UUID.fromString("09b26c6d-570d-4b4f-804c-72ac71325cc0"),
    ),
    ManualLinkFix(
      applicationId = UUID.fromString("75337979-e4c9-45f9-b829-948d8516f364"),
      placementRequestId = UUID.fromString("c1131478-2b3c-4dd8-84c4-88698184d632"),
      placementApplicationId = UUID.fromString("218b7567-460b-447c-9481-7315ba53c23e"),
    ),
    ManualLinkFix(
      applicationId = UUID.fromString("9c4597e3-a615-4f08-b697-39b5cb2fc152"),
      placementRequestId = UUID.fromString("44f15784-3497-4df5-9035-f65b741685c9"),
      placementApplicationId = UUID.fromString("2ef871f6-a4da-46a9-96f7-a2901f80aa40"),
    ),
    ManualLinkFix(
      applicationId = UUID.fromString("fe998691-61c6-489a-b94a-f62f112acd43"),
      placementRequestId = UUID.fromString("d70a8012-bc3d-43ad-93f2-d0b5724dc3bc"),
      placementApplicationId = UUID.fromString("4af4831b-a930-4de7-b612-1999b3e0313f"),
    ),
    ManualLinkFix(
      applicationId = UUID.fromString("4434ef73-abc1-451d-8c3e-eb9d43cd10ae"),
      placementRequestId = UUID.fromString("d2478632-1616-467d-8b6b-f11bfe03729b"),
      placementApplicationId = UUID.fromString("532f7117-f5f9-498e-92ee-eb961de98af0"),
    ),
    ManualLinkFix(
      applicationId = UUID.fromString("4434ef73-abc1-451d-8c3e-eb9d43cd10ae"),
      placementRequestId = UUID.fromString("5bccc0d4-f541-438a-a803-152e1d3ebd54"),
      placementApplicationId = UUID.fromString("8848e951-b51d-43f3-a205-d7404a732d5f"),
    ),
    ManualLinkFix(
      applicationId = UUID.fromString("95b2aaa5-9314-49de-b272-b9e0bb8ee643"),
      placementRequestId = UUID.fromString("bfdee07a-f0f8-428d-9d34-28455fc10ec7"),
      placementApplicationId = UUID.fromString("81d60dd8-df26-4820-9b8c-828b2bdbaaa4"),
    ),
    ManualLinkFix(
      applicationId = UUID.fromString("9fff5bbd-c538-446e-a3c8-5c770e05bdb6"),
      placementRequestId = UUID.fromString("46765be3-b46b-4202-9cc1-118cb6d73928"),
      placementApplicationId = UUID.fromString("8cf4c9de-0e29-4af7-8621-437e6f7bc644"),
    ),
    ManualLinkFix(
      applicationId = UUID.fromString("ce51f404-3f8e-400c-b235-89e2ce5461d7"),
      placementRequestId = UUID.fromString("a8e380ee-10ce-48ba-98fa-9c115d722649"),
      placementApplicationId = UUID.fromString("9ef0080c-f83f-4f81-8219-4a7c61a70388"),
    ),
    ManualLinkFix(
      applicationId = UUID.fromString("e3a87d85-fa54-4654-b804-4d0fb9253121"),
      placementRequestId = UUID.fromString("f681fa85-69df-46b6-bd05-cf4191c1d6d6"),
      placementApplicationId = UUID.fromString("af74d224-04d5-4598-a9a9-6e086ec2b60b"),
    ),
    ManualLinkFix(
      applicationId = UUID.fromString("e3a87d85-fa54-4654-b804-4d0fb9253121"),
      placementRequestId = UUID.fromString("7a4f4108-9874-474a-8c70-528986cb3368"),
      placementApplicationId = UUID.fromString("6ebea4ff-34e7-4c64-ac20-b8b218c0f706"),
    ),
    ManualLinkFix(
      applicationId = UUID.fromString("ab02981b-a7f3-4075-8cff-27e5edad597d"),
      placementRequestId = UUID.fromString("55b0e01f-4006-4c5b-b61e-dfe1d47a27d3"),
      placementApplicationId = UUID.fromString("c0886c1c-11ac-4ce7-93a8-ce180e61bf36"),
    ),
    ManualLinkFix(
      applicationId = UUID.fromString("cd508e65-6f2b-4bb1-8aa6-6ae3fee57c74"),
      placementRequestId = UUID.fromString("1aab2f12-d71b-47cc-af74-4e3a72d4af04"),
      placementApplicationId = UUID.fromString("c45764f2-98d6-4af5-a784-2faa16e6b2c9"),
    ),
    ManualLinkFix(
      applicationId = UUID.fromString("cd508e65-6f2b-4bb1-8aa6-6ae3fee57c74"),
      placementRequestId = UUID.fromString("e7a98cc9-8a71-4c9d-b5f5-470e321bf24b"),
      placementApplicationId = UUID.fromString("2d6e7e0d-7202-47d9-847c-f50614018d9b"),
    ),
    ManualLinkFix(
      applicationId = UUID.fromString("cd508e65-6f2b-4bb1-8aa6-6ae3fee57c74"),
      placementRequestId = UUID.fromString("18a532f7-7893-47a1-ab8e-1e34d7b3ff46"),
      placementApplicationId = UUID.fromString("ec618a23-2ad3-4df0-bb91-a81cdaaab677"),
    ),
  )
}
