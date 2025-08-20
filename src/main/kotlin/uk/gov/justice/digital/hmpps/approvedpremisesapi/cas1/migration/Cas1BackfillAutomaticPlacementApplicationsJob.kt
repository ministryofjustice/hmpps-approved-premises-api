package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.migration

import org.slf4j.LoggerFactory
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Repository
import org.springframework.stereotype.Service
import org.springframework.transaction.support.TransactionTemplate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.LockablePlacementRequestRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationDecision
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationPlaceholderRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationWithdrawalReason
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementRequestEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementRequestWithdrawalReason
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.migration.MigrationJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1PlacementApplicationDomainEventService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.WithdrawableEntityType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.WithdrawalContext
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.WithdrawalTriggeredByUser
import java.time.Instant
import java.util.UUID

@Service
class Cas1BackfillAutomaticPlacementApplicationsJob(
  private val transactionTemplate: TransactionTemplate,
  private val cas1BackfillAutomaticPlacementApplicationsRepository: Cas1BackfillAutomaticPlacementApplicationsRepository,
  private val approvedPremisesApplicationRepository: ApprovedPremisesApplicationRepository,
  private val placementApplicationRepository: PlacementApplicationRepository,
  private val placementApplicationPlaceholderRepository: PlacementApplicationPlaceholderRepository,
  private val lockablePlacementRequestRepository: LockablePlacementRequestRepository,
  private val cas1PlacementApplicationDomainEventService: Cas1PlacementApplicationDomainEventService,
  private val userRepository: UserRepository,
) : MigrationJob() {

  private val log = LoggerFactory.getLogger(this::class.java)

  override val shouldRunInTransaction = false

  override fun process(pageSize: Int) {
    val applicationIds = cas1BackfillAutomaticPlacementApplicationsRepository.applicationIdsWithStandardPlacementRequestsWithoutAPlacementApp()

    log.info("There are ${applicationIds.size} applications with 'initial' placement requests to update")

    applicationIds.forEach { applicationId ->
      transactionTemplate.executeWithoutResult {
        processApplication(
          approvedPremisesApplicationRepository.findByIdOrNull(applicationId)!!,
        )
      }
    }
  }

  private fun processApplication(application: ApprovedPremisesApplicationEntity) {
    val applicationId = application.id

    val placeholder = placementApplicationPlaceholderRepository.findByApplicationAndArchivedIsFalse(application)
    if (placeholder != null) {
      placeholder.archived = true
      placementApplicationPlaceholderRepository.save(placeholder)
    }

    val applicablePlacementRequests = application.placementRequests.filter { it.placementApplication == null }

    if (applicablePlacementRequests.size > 1) {
      log.warn("Have ${applicablePlacementRequests.size} applicable placement requests for application $applicationId")
    }

    applicablePlacementRequests.sortedBy { it.expectedArrival }.forEachIndexed { i, placementRequest ->
      /**
       * For almost all cases there should be one placement request and a non-archived
       * placeholder available. There are some cases where:
       *
       * 1. There are multiple 'initial' placement requests linked to an application
       * 2. There is no placeholder linked to the application, because there is no arrival_date on the application
       *
       * Both of these are due to bugs or original application logic that hasn't been
       * in place since late 2023. In these cases we have no choice but to generate
       * a new ID for these placement applications. This only affects the
       * request for placement reports as these entries will now appear with new IDs,
       * instead of using duplicate IDs
       */
      val newPlacementApplicationId = if (i == 0 && placeholder != null) {
        placeholder.id
      } else {
        val generatedId = UUID.randomUUID()
        log.warn("Using generated UUID $generatedId for placement request ${placementRequest.id} for application $applicationId")
        generatedId
      }

      createAutomaticPlacementApplication(newPlacementApplicationId, placementRequest)
    }
  }

  private fun createAutomaticPlacementApplication(
    newPlacementApplicationId: UUID,
    placementRequest: PlacementRequestEntity,
  ) {
    lockablePlacementRequestRepository.acquirePessimisticLock(placementRequest.id)

    val application = placementRequest.application

    // this should never happen
    if (placementApplicationRepository.existsById(newPlacementApplicationId)) {
      error("A placement application already exists with the ID $newPlacementApplicationId")
    }

    val requestedDuration = if (application.duration != null) {
      application.duration
    } else {
      log.warn("No duration defined on the application ${application.id}, using the placement request duration")
      placementRequest.duration
    }

    val placementApplication = placementApplicationRepository.save(
      PlacementApplicationEntity(
        id = newPlacementApplicationId,
        backfilledAutomatic = true,
        application = application,
        createdByUser = application.createdByUser,
        createdAt = application.createdAt,
        expectedArrival = placementRequest.expectedArrival,
        expectedArrivalFlexible = null,
        requestedDuration = requestedDuration,
        authorisedDuration = placementRequest.duration,
        submittedAt = application.submittedAt!!,
        decision = PlacementApplicationDecision.ACCEPTED,
        decisionMadeAt = placementRequest.createdAt,
        placementType = PlacementType.AUTOMATIC,
        automatic = true,
        placementRequest = null,
        submissionGroupId = UUID.randomUUID(),
        withdrawalReason = determineWithdrawalReason(placementRequest),
        isWithdrawn = placementRequest.isWithdrawn,
        data = null,
        document = null,
        allocatedToUser = placementRequest.assessment.allocatedToUser,
        allocatedAt = placementRequest.assessment.allocatedAt,
        reallocatedAt = null,
        dueAt = null,
      ),
    )

    placementRequest.placementApplication = placementApplication

    if (placementApplication.isWithdrawn) {
      val withdrawalInfo = cas1BackfillAutomaticPlacementApplicationsRepository.findPlacementRequestWithdrawalEventInfo(
        applicationId = application.id,
        placementRequestId = placementRequest.id,
      ) ?: error("cannot find withdrawal domain event for placement request ${placementRequest.id}")

      val withdrawingUserName = withdrawalInfo.getWithdrawnBy()
        ?: error("cannot find withdrawal domain event triggered by for placement request ${placementRequest.id}")

      val withdrawnByUser = userRepository.findByDeliusUsernameIgnoreCase(withdrawingUserName)
        ?: error("cannot find user with username $withdrawingUserName for placement request ${placementRequest.id}")

      cas1PlacementApplicationDomainEventService.placementApplicationWithdrawn(
        placementApplication = placementApplication,
        withdrawalContext = WithdrawalContext(
          withdrawalTriggeredBy = WithdrawalTriggeredByUser(withdrawnByUser),
          triggeringEntityType = WithdrawableEntityType.PlacementApplication,
          triggeringEntityId = placementApplication.id,
        ),
        eventOccurredAt = withdrawalInfo.getOccurredAt(),
      )
    }
  }

  private fun determineWithdrawalReason(placementRequest: PlacementRequestEntity) = when (placementRequest.withdrawalReason) {
    PlacementRequestWithdrawalReason.DUPLICATE_PLACEMENT_REQUEST -> PlacementApplicationWithdrawalReason.DUPLICATE_PLACEMENT_REQUEST
    PlacementRequestWithdrawalReason.ALTERNATIVE_PROVISION_IDENTIFIED -> PlacementApplicationWithdrawalReason.ALTERNATIVE_PROVISION_IDENTIFIED
    PlacementRequestWithdrawalReason.WITHDRAWN_BY_PP -> PlacementApplicationWithdrawalReason.WITHDRAWN_BY_PP
    PlacementRequestWithdrawalReason.CHANGE_IN_CIRCUMSTANCES -> PlacementApplicationWithdrawalReason.CHANGE_IN_CIRCUMSTANCES
    PlacementRequestWithdrawalReason.CHANGE_IN_RELEASE_DECISION -> PlacementApplicationWithdrawalReason.CHANGE_IN_RELEASE_DECISION
    PlacementRequestWithdrawalReason.NO_CAPACITY_DUE_TO_LOST_BED -> PlacementApplicationWithdrawalReason.NO_CAPACITY_DUE_TO_LOST_BED
    PlacementRequestWithdrawalReason.NO_CAPACITY_DUE_TO_PLACEMENT_PRIORITISATION -> PlacementApplicationWithdrawalReason.NO_CAPACITY_DUE_TO_PLACEMENT_PRIORITISATION
    PlacementRequestWithdrawalReason.NO_CAPACITY -> PlacementApplicationWithdrawalReason.NO_CAPACITY
    PlacementRequestWithdrawalReason.ERROR_IN_PLACEMENT_REQUEST -> PlacementApplicationWithdrawalReason.ERROR_IN_PLACEMENT_REQUEST
    PlacementRequestWithdrawalReason.RELATED_APPLICATION_WITHDRAWN -> PlacementApplicationWithdrawalReason.RELATED_APPLICATION_WITHDRAWN
    PlacementRequestWithdrawalReason.RELATED_PLACEMENT_APPLICATION_WITHDRAWN -> error("placement request ${placementRequest.id} has 'placement app withdrawn' withdrawal reason")
    null -> null
  }
}

@Repository
interface Cas1BackfillAutomaticPlacementApplicationsRepository : JpaRepository<ApplicationEntity, UUID> {
  @Query(
    value = """SELECT a.id FROM approved_premises_applications a 
      INNER JOIN placement_requests pr on pr.application_Id = a.id
      WHERE pr.placement_application_id IS NULL""",
    nativeQuery = true,
  )
  fun applicationIdsWithStandardPlacementRequestsWithoutAPlacementApp(): List<UUID>

  @Query(
    value = """
        SELECT 
         data -> 'eventDetails' -> 'withdrawnBy' -> 'staffMember' ->> 'username' as withdrawnBy,
         occurred_at
       FROM domain_events
        WHERE
          application_id = :applicationId AND
          type = 'APPROVED_PREMISES_MATCH_REQUEST_WITHDRAWN' AND
          cast(data -> 'eventDetails' ->> 'matchRequestId' AS uuid) = :placementRequestId
    """,
    nativeQuery = true,
  )
  fun findPlacementRequestWithdrawalEventInfo(applicationId: UUID, placementRequestId: UUID): PlacementRequestWithdrawnInfo?

  interface PlacementRequestWithdrawnInfo {
    fun getWithdrawnBy(): String?
    fun getOccurredAt(): Instant
  }
}
