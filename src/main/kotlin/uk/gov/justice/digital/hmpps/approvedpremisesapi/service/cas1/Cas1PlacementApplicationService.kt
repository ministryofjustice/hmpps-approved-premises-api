package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1

import arrow.core.Either
import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.transaction.Transactional
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.cas1.Cas1RequestedPlacementPeriod
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PlacementApplicationDecisionEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ReleaseTypeOption
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SubmitPlacementApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.reporting.Cas1RequestForPlacementReportRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesAssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentDecision
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.LockablePlacementApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationWithdrawalReason
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserPermission
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.ApprovedPremisesApplicationStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.ValidationErrors
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.validatedCasResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.InternalServerErrorProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.CasResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.allocations.UserAllocator
import java.time.Clock
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PlacementApplicationDecision as ApiPlacementApplicationDecision
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PlacementType as ApiPlacementType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationDecision as JpaPlacementApplicationDecision

@Service
@Suppress("ReturnCount")
class Cas1PlacementApplicationService(
  private val placementApplicationRepository: PlacementApplicationRepository,
  private val userService: UserService,
  private val placementRequestService: Cas1PlacementRequestService,
  private val userAllocator: UserAllocator,
  private val userAccessService: Cas1UserAccessService,
  private val cas1PlacementApplicationEmailService: Cas1PlacementApplicationEmailService,
  private val cas1PlacementApplicationDomainEventService: Cas1PlacementApplicationDomainEventService,
  private val cas1TaskDeadlineService: Cas1TaskDeadlineService,
  private val clock: Clock,
  private val lockablePlacementApplicationRepository: LockablePlacementApplicationRepository,
  private val objectMapper: ObjectMapper,
) {

  var log: Logger = LoggerFactory.getLogger(this::class.java)

  fun getAllSubmittedNonReallocatedApplications(applicationId: UUID): List<PlacementApplicationEntity> = placementApplicationRepository.findAllSubmittedNonReallocatedApplicationsForApplicationId(applicationId)

  fun createPlacementApplication(
    application: ApprovedPremisesApplicationEntity,
    user: UserEntity,
  ) = validatedCasResult<PlacementApplicationEntity> {
    val assessment = application.getLatestAssessment()

    if (assessment?.decision !== AssessmentDecision.ACCEPTED) {
      return generalError("You cannot request a placement request for an application that has not been approved")
    }

    if (application.status == ApprovedPremisesApplicationStatus.WITHDRAWN) {
      return generalError("You cannot request a placement request for an application that has been withdrawn")
    }

    if (application.status == ApprovedPremisesApplicationStatus.EXPIRED) {
      return generalError("Placement requests cannot be made for an expired application")
    }

    val placementApplication = placementApplicationRepository.save(
      PlacementApplicationEntity(
        id = UUID.randomUUID(),
        application = application,
        createdByUser = user,
        data = null,
        document = null,
        createdAt = OffsetDateTime.now(),
        submittedAt = null,
        allocatedToUser = null,
        allocatedAt = null,
        reallocatedAt = null,
        decision = null,
        decisionMadeAt = null,
        placementType = null,
        automatic = false,
        placementRequest = null,
        withdrawalReason = null,
        dueAt = null,
        submissionGroupId = UUID.randomUUID(),
      ),
    )

    val createdApplication = placementApplicationRepository.save(placementApplication)

    return success(createdApplication)
  }

  /**
   * Create a placement application that represents the request for placement implicitly
   * made in an application.
   *
   * This is created when a decision has been made on an application's assessment.
   *
   * Before the application has been assessed, an entry in `placement_applications_placeholder`
   * is created to represent the implicit request for placement for reporting purposes (see
   * [Cas1RequestForPlacementReportRepository]). When this function is called, the corresponding
   * `placement_applications_placeholder` entry will be archived, and its ID used for this
   * placement application's ID, ensuring consistency in reporting (i.e. the change from
   * a `placement_applications_placeholder` entry to a `placement_applications` entry is
   * transparent to the reporting user).
   *
   * Currently, we only create these if an application's assessment has been accepted, as the
   * initial purpose of this entity is to provide us with a way to capture attributes specific
   * to each individual request for placement in a single data model.
   *
   * The ultimate goal is to create a `placement_application` on application submission (when an
   * implicit request for placement is made), dropping the `placement_applications_placeholder` table
   * completely. At this point we will also track rejected implicit requests for placements via
   * `placement_applications`. This is complicated though because we need to consider the
   * assessment lifecycle and how that impacts the request for placement, including appeals.
   *
   * Note that the caller of this function triggers emails related to the request for placement
   * being accepted and creation of the placement request. Logically this should probably be
   * moved into this function (or at least under the ownership of this service), similar
   * to the `recordDecision` function
   */
  fun createAutomaticPlacementApplication(
    id: UUID,
    assessment: ApprovedPremisesAssessmentEntity,
    authorisedExpectedArrival: LocalDate,
    authorisedDurationDays: Int,
  ): PlacementApplicationEntity {
    val application = assessment.cas1Application()
    return placementApplicationRepository.save(
      placementApplicationRepository.save(
        PlacementApplicationEntity(
          id = id,
          application = application,
          createdByUser = application.createdByUser,
          createdAt = application.createdAt,
          expectedArrival = authorisedExpectedArrival,
          requestedDuration = application.duration,
          authorisedDuration = authorisedDurationDays,
          submittedAt = application.submittedAt!!,
          decision = JpaPlacementApplicationDecision.ACCEPTED,
          decisionMadeAt = assessment.decisionMadeAt(),
          placementType = PlacementType.AUTOMATIC,
          automatic = true,
          placementRequest = null,
          submissionGroupId = UUID.randomUUID(),
          withdrawalReason = null,
          data = null,
          document = null,
          allocatedToUser = assessment.allocatedToUser,
          allocatedAt = assessment.allocatedAt,
          reallocatedAt = null,
          dueAt = null,
        ),
      ),
    )
  }

  fun getApplication(id: UUID): CasResult<PlacementApplicationEntity> {
    val placementApplication =
      placementApplicationRepository.findByIdOrNull(id) ?: return CasResult.NotFound("placement application", id.toString())

    return CasResult.Success(placementApplication)
  }

  fun getApplicationOrNull(id: UUID) = placementApplicationRepository.findByIdOrNull(id)

  fun reallocateApplication(
    assigneeUser: UserEntity,
    id: UUID,
  ): CasResult<PlacementApplicationEntity> {
    lockablePlacementApplicationRepository.acquirePessimisticLock(id)

    val currentPlacementApplication = placementApplicationRepository.findByIdOrNull(id)
      ?: return CasResult.NotFound("placement application", id.toString())

    if (currentPlacementApplication.reallocatedAt != null) {
      return CasResult.ConflictError(
        currentPlacementApplication.id,
        "This placement application has already been reallocated",
      )
    }

    if (currentPlacementApplication.decision != null) {
      return CasResult.GeneralValidationError("This placement application has already been completed")
    }

    if (!assigneeUser.hasPermission(UserPermission.CAS1_ASSESS_PLACEMENT_APPLICATION)) {
      return CasResult.FieldValidationError(
        ValidationErrors().apply {
          this["$.userId"] = "lackingMatcherRole"
        },
      )
    }

    currentPlacementApplication.reallocatedAt = OffsetDateTime.now()
    placementApplicationRepository.save(currentPlacementApplication)

    // Make the timestamp precision less precise, so we don't have any issues with microsecond resolution in tests
    val dateTimeNow = OffsetDateTime.now(clock).withNano(0)

    val newPlacementApplication = currentPlacementApplication.copy(
      id = UUID.randomUUID(),
      reallocatedAt = null,
      allocatedAt = currentPlacementApplication.reallocatedAt,
      allocatedToUser = assigneeUser,
      createdAt = dateTimeNow,
      dueAt = null,
    )

    newPlacementApplication.dueAt = cas1TaskDeadlineService.getDeadline(newPlacementApplication)

    placementApplicationRepository.save(newPlacementApplication)
    cas1PlacementApplicationDomainEventService.placementApplicationAllocated(
      placementApplication = newPlacementApplication,
      allocatedByUser = userService.getUserForRequest(),
    )

    val applicationWasntPreviouslyAllocated = currentPlacementApplication.allocatedToUser == null
    if (applicationWasntPreviouslyAllocated) {
      cas1PlacementApplicationEmailService.placementApplicationAllocated(newPlacementApplication)
    }

    return CasResult.Success(newPlacementApplication)
  }

  fun getWithdrawableState(placementApplication: PlacementApplicationEntity, user: UserEntity): WithdrawableState = WithdrawableState(
    withdrawable = placementApplication.isInWithdrawableState(),
    withdrawn = placementApplication.isWithdrawn,
    userMayDirectlyWithdraw = userAccessService.userMayWithdrawPlacementApplication(user, placementApplication),
  )

  /**
   * This function should not be called directly. Instead, use [Cas1WithdrawableService.withdrawPlacementApplication] that
   * will indirectly invoke this function. It will also ensure that:
   *
   * 1. The entity is withdrawable, and error if not
   * 2. The user is allowed to withdraw it, and error if not
   * 3. If withdrawn, all descendents entities are withdrawn, where applicable
   */
  @SuppressWarnings("ThrowsCount")
  @Transactional
  fun withdrawPlacementApplication(
    id: UUID,
    userProvidedReason: PlacementApplicationWithdrawalReason?,
    withdrawalContext: WithdrawalContext,
  ): CasResult<PlacementApplicationEntity> {
    val placementApplication =
      placementApplicationRepository.findByIdOrNull(id) ?: return CasResult.NotFound(
        entityType = "PlacementApplication",
        id = id.toString(),
      )

    if (placementApplication.isWithdrawn) {
      return CasResult.Success(placementApplication)
    }

    val wasBeingAssessedBy = if (placementApplication.isBeingAssessed()) {
      placementApplication.allocatedToUser
    } else {
      null
    }

    placementApplication.isWithdrawn = true
    placementApplication.withdrawalReason = when (withdrawalContext.triggeringEntityType) {
      WithdrawableEntityType.Application -> PlacementApplicationWithdrawalReason.RELATED_APPLICATION_WITHDRAWN
      WithdrawableEntityType.PlacementApplication -> userProvidedReason
      WithdrawableEntityType.PlacementRequest -> throw InternalServerErrorProblem("Withdrawing a PlacementRequest should not cascade to PlacementApplications")
      WithdrawableEntityType.SpaceBooking -> throw InternalServerErrorProblem("Withdrawing a SpaceBooking should not cascade to PlacementApplications")
    }

    val savedApplication = placementApplicationRepository.save(placementApplication)

    cas1PlacementApplicationDomainEventService.placementApplicationWithdrawn(placementApplication, withdrawalContext)
    cas1PlacementApplicationEmailService.placementApplicationWithdrawn(
      placementApplication = placementApplication,
      wasBeingAssessedBy = wasBeingAssessedBy,
      withdrawalTriggeredBy = withdrawalContext.withdrawalTriggeredBy,
    )

    return CasResult.Success(savedApplication)
  }

  fun updateApplication(
    id: UUID,
    data: String,
  ): CasResult<PlacementApplicationEntity> {
    val placementApplicationAuthorisationResult = getApplicationForUpdateOrSubmit<PlacementApplicationEntity>(id)

    if (placementApplicationAuthorisationResult is Either.Left) {
      return placementApplicationAuthorisationResult.value
    }

    val placementApplicationEntity = (placementApplicationAuthorisationResult as Either.Right).value

    if (placementApplicationEntity.application.status == ApprovedPremisesApplicationStatus.EXPIRED) {
      return CasResult.GeneralValidationError("Placement requests cannot be made for an expired application")
    }

    placementApplicationEntity.data = data

    val savedApplication = placementApplicationRepository.save(placementApplicationEntity)

    return CasResult.Success(savedApplication)
  }

  @Transactional
  fun submitApplication(
    id: UUID,
    submitPlacementApplication: SubmitPlacementApplication,
  ): CasResult<List<PlacementApplicationEntity>> {
    if (submitPlacementApplication.placementDates.isNullOrEmpty() && submitPlacementApplication.requestedPlacementPeriods.isNullOrEmpty()) {
      return CasResult.GeneralValidationError("Please provide at least one of placement dates or requested placement periods.")
    }

    if (submitPlacementApplication.placementType == null && submitPlacementApplication.releaseType == null) {
      return CasResult.GeneralValidationError("Please provide at least one of placementType or releaseType.")
    }

    val translatedDocument = objectMapper.writeValueAsString(submitPlacementApplication.translatedDocument)

    val cas1RequestedPlacementPeriod = deriveRequestedPlacementPeriods(submitPlacementApplication)!!

    var placementTypeValue = getPlacementTypeForApplication(submitPlacementApplication)

    val placementApplicationAuthorisationResult = getApplicationForUpdateOrSubmit<List<PlacementApplicationEntity>>(id)

    if (placementApplicationAuthorisationResult is Either.Left) {
      return placementApplicationAuthorisationResult.value
    }

    val submittedPlacementApplication = (placementApplicationAuthorisationResult as Either.Right).value

    if (submittedPlacementApplication.application.status == ApprovedPremisesApplicationStatus.EXPIRED) {
      return CasResult.GeneralValidationError("Placement requests cannot be made for an expired application")
    }

    val allocatedUser = userAllocator.getUserForPlacementApplicationAllocation(submittedPlacementApplication)

    val now = OffsetDateTime.now(clock)

    submittedPlacementApplication.apply {
      document = translatedDocument
      allocatedToUser = allocatedUser
      submittedAt = now
      allocatedAt = now
      placementType = placementTypeValue
      submissionGroupId = UUID.randomUUID()
      releaseType = submitPlacementApplication.releaseType?.toString()
      sentenceType = submitPlacementApplication.sentenceType?.toString()
      situation = submitPlacementApplication.situationType?.toString()
    }

    submittedPlacementApplication.dueAt = cas1TaskDeadlineService.getDeadline(submittedPlacementApplication)

    val baselinePlacementApplication = placementApplicationRepository.save(submittedPlacementApplication)

    val placementApplicationsWithDates = saveDatesOnSubmissionToAnAppPerDate(baselinePlacementApplication, cas1RequestedPlacementPeriod)

    placementApplicationsWithDates.forEach { placementApplication ->
      cas1PlacementApplicationDomainEventService.placementApplicationSubmitted(
        placementApplication,
        userService.getDeliusUserNameForRequest(),
      )
      cas1PlacementApplicationEmailService.placementApplicationSubmitted(placementApplication)
      if (baselinePlacementApplication.allocatedToUser != null) {
        cas1PlacementApplicationEmailService.placementApplicationAllocated(placementApplication)
        cas1PlacementApplicationDomainEventService.placementApplicationAllocated(
          placementApplication = placementApplication,
          allocatedByUser = null,
        )
      }
    }

    return CasResult.Success(placementApplicationsWithDates)
  }

  private fun deriveRequestedPlacementPeriods(submitPlacementApplication: SubmitPlacementApplication): List<Cas1RequestedPlacementPeriod>? = if (!submitPlacementApplication.placementDates.isNullOrEmpty()) {
    submitPlacementApplication.placementDates.map { placementDate ->
      Cas1RequestedPlacementPeriod(
        arrival = placementDate.expectedArrival,
        arrivalFlexible = null,
        duration = placementDate.duration,
      )
    }
  } else {
    submitPlacementApplication.requestedPlacementPeriods
  }

  private fun getPlacementTypeForApplication(submitPlacementApplication: SubmitPlacementApplication): PlacementType {
    var placementTypeValue = when {
      submitPlacementApplication.placementType != null -> getPlacementType(submitPlacementApplication.placementType)

      submitPlacementApplication.releaseType == ReleaseTypeOption.paroleDirectedLicence -> PlacementType.RELEASE_FOLLOWING_DECISION
      submitPlacementApplication.releaseType == ReleaseTypeOption.rotl -> PlacementType.ROTL
      else -> PlacementType.ADDITIONAL_PLACEMENT
    }
    return placementTypeValue
  }

  private fun saveDatesOnSubmissionToAnAppPerDate(
    baselinePlacementApplication: PlacementApplicationEntity,
    requestedPlacementPeriods: List<Cas1RequestedPlacementPeriod>,
  ): List<PlacementApplicationEntity> {
    val additionalPlacementApps = List(requestedPlacementPeriods.size - 1) {
      placementApplicationRepository.save(
        baselinePlacementApplication.copy(id = UUID.randomUUID()),
      )
    }

    val allPlacementApps = listOf(baselinePlacementApplication) + additionalPlacementApps

    allPlacementApps.zip(requestedPlacementPeriods) { placementApp, apiDate ->
      placementApp.expectedArrival = apiDate.arrival
      placementApp.requestedDuration = apiDate.duration
      placementApp.expectedArrivalFlexible = apiDate.arrivalFlexible
    }

    return allPlacementApps
  }

  @Transactional
  fun recordDecision(
    id: UUID,
    placementApplicationDecisionEnvelope: PlacementApplicationDecisionEnvelope,
  ): CasResult<PlacementApplicationEntity> {
    val user = userService.getUserForRequest()
    val placementApplicationEntity =
      placementApplicationRepository.findByIdOrNull(id) ?: return CasResult.NotFound(
        entityType = "PlacementApplication",
        id = id.toString(),
      )

    if (placementApplicationEntity.allocatedToUser != user) {
      return CasResult.Unauthorised()
    }

    if (placementApplicationEntity.decision != null) {
      return CasResult.GeneralValidationError("This application has already had a decision set")
    }

    if (placementApplicationDecisionEnvelope.decision == ApiPlacementApplicationDecision.accepted) {
      val placementRequestResult =
        placementRequestService.createPlacementRequestsFromPlacementApplication(
          placementApplicationEntity,
          placementApplicationDecisionEnvelope.decisionSummary,
        )

      if (placementRequestResult is CasResult.Error) {
        return placementRequestResult.reviseType()
      }

      placementApplicationEntity.authorisedDuration = placementApplicationEntity.requestedDuration
    }

    placementApplicationEntity.apply {
      decision = JpaPlacementApplicationDecision.valueOf(placementApplicationDecisionEnvelope.decision)
      decisionMadeAt = OffsetDateTime.now(clock)
    }

    val savedApplication = placementApplicationRepository.save(placementApplicationEntity)

    when (placementApplicationDecisionEnvelope.decision) {
      ApiPlacementApplicationDecision.accepted -> cas1PlacementApplicationEmailService.placementApplicationAccepted(placementApplicationEntity)
      ApiPlacementApplicationDecision.rejected -> cas1PlacementApplicationEmailService.placementApplicationRejected(placementApplicationEntity)
      ApiPlacementApplicationDecision.withdraw -> cas1PlacementApplicationEmailService.placementApplicationRejected(placementApplicationEntity)
      ApiPlacementApplicationDecision.withdrawnByPp -> cas1PlacementApplicationEmailService.placementApplicationRejected(placementApplicationEntity)
    }

    cas1PlacementApplicationDomainEventService.placementApplicationAssessed(
      savedApplication,
      user,
      placementApplicationDecisionEnvelope,
    )

    return CasResult.Success(savedApplication)
  }

  private fun getPlacementType(apiPlacementType: ApiPlacementType): PlacementType = when (apiPlacementType) {
    ApiPlacementType.additionalPlacement -> PlacementType.ADDITIONAL_PLACEMENT
    ApiPlacementType.rotl -> PlacementType.ROTL
    ApiPlacementType.releaseFollowingDecision -> PlacementType.RELEASE_FOLLOWING_DECISION
  }

  private fun <T> getApplicationForUpdateOrSubmit(id: UUID): Either<CasResult<T>, PlacementApplicationEntity> {
    val placementApplication = placementApplicationRepository.findByIdOrNull(id)
      ?: return Either.Left(
        CasResult.NotFound(
          entityType = "PlacementApplication",
          id = id.toString(),
        ),
      )
    val user = userService.getUserForRequest()

    if (placementApplication.createdByUser != user) {
      return Either.Left(CasResult.Unauthorised())
    }

    val validationError = confirmApplicationCanBeUpdatedOrSubmitted<T>(placementApplication)
    if (validationError != null) {
      return Either.Left(validationError)
    }

    return Either.Right(placementApplication)
  }

  private fun <T> confirmApplicationCanBeUpdatedOrSubmitted(
    placementApplicationEntity: PlacementApplicationEntity,
  ): CasResult<T>? {
    if (placementApplicationEntity.submittedAt != null) {
      return CasResult.GeneralValidationError("This application has already been submitted")
    }

    return null
  }
}
