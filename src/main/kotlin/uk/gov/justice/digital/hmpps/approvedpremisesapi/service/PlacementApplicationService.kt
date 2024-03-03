package uk.gov.justice.digital.hmpps.approvedpremisesapi.service

import arrow.core.Either
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.allocations.UserAllocator
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PlacementApplicationDecisionEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.NotifyConfig
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesPlacementApplicationJsonSchemaEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentDecision
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationDecision
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationWithdrawalReason
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementDateEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementDateRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.ApprovedPremisesApplicationStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.ValidationErrors
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.validated
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.InternalServerErrorProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.AuthorisableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.CasResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.ValidatableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.ValidatableActionResult.ValidatableActionResultError
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1PlacementApplicationDomainEventService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1PlacementApplicationEmailService
import java.time.OffsetDateTime
import java.util.UUID
import javax.annotation.PostConstruct
import javax.transaction.Transactional
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PlacementApplicationDecision as ApiPlacementApplicationDecision
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PlacementDates as ApiPlacementDates
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PlacementType as ApiPlacementType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationDecision as JpaPlacementApplicationDecision

@Service
@Suppress("ReturnCount")
class PlacementApplicationService(
  private val placementApplicationRepository: PlacementApplicationRepository,
  private val jsonSchemaService: JsonSchemaService,
  private val userService: UserService,
  private val placementDateRepository: PlacementDateRepository,
  private val placementRequestService: PlacementRequestService,
  private val userAllocator: UserAllocator,
  private val emailNotificationService: EmailNotificationService,
  private val notifyConfig: NotifyConfig,
  private val userAccessService: UserAccessService,
  private val cas1PlacementApplicationEmailService: Cas1PlacementApplicationEmailService,
  private val cas1PlacementApplicationDomainEventService: Cas1PlacementApplicationDomainEventService,
  @Value("\${notify.send-placement-request-notifications}")
  private val sendPlacementRequestNotifications: Boolean,
  private val taskDeadlineService: TaskDeadlineService,
  @Value("\${feature-flags.cas1-use-new-withdrawal-logic}") private val useNewWithdrawalLogic: Boolean,
  private val withdrawableService: WithdrawableService,
) {

  var log: Logger = LoggerFactory.getLogger(this::class.java)

  @PostConstruct
  fun init() {
    if (!useNewWithdrawalLogic) {
      log.warn(
        "Old withdrawal logic is being used. This will add multiple dates to the same placement application " +
          "on submission which limits potential withdrawal options. This behaviour is deprecated.",
      )
    }
  }

  fun getAllPlacementApplicationEntitiesForApplicationId(applicationId: UUID): List<PlacementApplicationEntity> {
    return placementApplicationRepository.findAllSubmittedNonReallocatedAndNonWithdrawnApplicationsForApplicationId(
      applicationId,
    )
  }

  fun createPlacementApplication(
    application: ApprovedPremisesApplicationEntity,
    user: UserEntity,
  ) = validated<PlacementApplicationEntity> {
    val assessment = application.getLatestAssessment()

    if (assessment?.decision !== AssessmentDecision.ACCEPTED) {
      return generalError("You cannot request a placement request for an application that has not been approved")
    }

    if (application.status == ApprovedPremisesApplicationStatus.WITHDRAWN) {
      return generalError("You cannot request a placement request for an application that has been withdrawn")
    }

    val placementApplication = placementApplicationRepository.save(
      PlacementApplicationEntity(
        id = UUID.randomUUID(),
        application = application,
        createdByUser = user,
        schemaVersion = jsonSchemaService.getNewestSchema(ApprovedPremisesPlacementApplicationJsonSchemaEntity::class.java),
        schemaUpToDate = true,
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
        placementDates = mutableListOf(),
        placementRequests = mutableListOf(),
        withdrawalReason = null,
        dueAt = null,
        submissionGroupId = UUID.randomUUID(),
      ),
    )

    val createdApplication = placementApplicationRepository.save(placementApplication)

    return success(createdApplication.apply { schemaUpToDate = true })
  }

  fun getApplication(id: UUID): AuthorisableActionResult<PlacementApplicationEntity> {
    val placementApplication =
      placementApplicationRepository.findByIdOrNull(id) ?: return AuthorisableActionResult.NotFound()

    return AuthorisableActionResult.Success(setSchemaUpToDate(placementApplication))
  }

  fun reallocateApplication(
    assigneeUser: UserEntity,
    id: UUID,
  ): AuthorisableActionResult<ValidatableActionResult<PlacementApplicationEntity>> {
    val currentPlacementApplication = placementApplicationRepository.findByIdOrNull(id)
      ?: return AuthorisableActionResult.NotFound()

    if (currentPlacementApplication.decision != null) {
      return AuthorisableActionResult.Success(
        ValidatableActionResult.GeneralValidationError("This placement application has already been completed"),
      )
    }

    if (!assigneeUser.hasRole(UserRole.CAS1_MATCHER)) {
      return AuthorisableActionResult.Success(
        ValidatableActionResult.FieldValidationError(
          ValidationErrors().apply {
            this["$.userId"] = "lackingMatcherRole"
          },
        ),
      )
    }

    currentPlacementApplication.reallocatedAt = OffsetDateTime.now()
    placementApplicationRepository.save(currentPlacementApplication)

    // Make the timestamp precision less precise, so we don't have any issues with microsecond resolution in tests
    val dateTimeNow = OffsetDateTime.now().withNano(0)

    val newPlacementApplication = currentPlacementApplication.copy(
      id = UUID.randomUUID(),
      reallocatedAt = null,
      allocatedToUser = assigneeUser,
      createdAt = dateTimeNow,
      dueAt = null,
    )

    newPlacementApplication.dueAt = taskDeadlineService.getDeadline(newPlacementApplication)

    placementApplicationRepository.save(newPlacementApplication)

    val newPlacementDates = placementDateRepository.saveAll(
      currentPlacementApplication.placementDates.map {
        PlacementDateEntity(
          id = UUID.randomUUID(),
          expectedArrival = it.expectedArrival,
          duration = it.duration,
          placementApplication = newPlacementApplication,
          createdAt = dateTimeNow,
        )
      },
    )

    val applicationWasntPreviouslyAllocated = currentPlacementApplication.allocatedToUser == null
    if (applicationWasntPreviouslyAllocated) {
      cas1PlacementApplicationEmailService.placementApplicationAllocated(newPlacementApplication)
    }

    newPlacementApplication.placementDates = newPlacementDates

    return AuthorisableActionResult.Success(
      ValidatableActionResult.Success(
        newPlacementApplication,
      ),
    )
  }

  fun getWithdrawablePlacementApplicationsForUser(user: UserEntity, application: ApprovedPremisesApplicationEntity) =
    placementApplicationRepository
      .findByApplication(application)
      .filter { it.isInWithdrawableState() && userAccessService.userMayWithdrawPlacementApplication(user, it) }

  fun getWithdrawableState(placementApplication: PlacementApplicationEntity, user: UserEntity): WithdrawableState {
    return WithdrawableState(
      withdrawable = placementApplication.isInWithdrawableState(),
      userMayDirectlyWithdraw = userAccessService.userMayWithdrawPlacementApplication(user, placementApplication),
    )
  }

  @Transactional
  fun withdrawPlacementApplication(
    id: UUID,
    userProvidedReason: PlacementApplicationWithdrawalReason?,
    withdrawalContext: WithdrawalContext,
  ): CasResult<PlacementApplicationEntity> {
    val user = requireNotNull(withdrawalContext.triggeringUser)

    val placementApplication =
      placementApplicationRepository.findByIdOrNull(id) ?: return CasResult.NotFound()

    if (placementApplication.isWithdrawn()) {
      return CasResult.Success(placementApplication)
    }

    val isUserRequestedWithdrawal = withdrawalContext.triggeringEntityType == WithdrawableEntityType.PlacementApplication
    if (isUserRequestedWithdrawal && !userAccessService.userMayWithdrawPlacementApplication(user, placementApplication)) {
      return CasResult.Unauthorised()
    }

    if (!placementApplication.isInWithdrawableState()) {
      return CasResult.GeneralValidationError("The Placement Application cannot be withdrawn as it's not in a withdrawable state")
    }

    val wasBeingAssessedBy = if (placementApplication.isBeingAssessed()) { placementApplication.allocatedToUser } else null

    placementApplication.decision = PlacementApplicationDecision.WITHDRAW
    placementApplication.decisionMadeAt = OffsetDateTime.now()
    placementApplication.withdrawalReason = when (withdrawalContext.triggeringEntityType) {
      WithdrawableEntityType.Application -> PlacementApplicationWithdrawalReason.RELATED_APPLICATION_WITHDRAWN
      WithdrawableEntityType.PlacementApplication -> userProvidedReason
      WithdrawableEntityType.PlacementRequest -> throw InternalServerErrorProblem("Withdrawing a PlacementRequest should not cascade to PlacementApplications")
      WithdrawableEntityType.Booking -> throw InternalServerErrorProblem("Withdrawing a Booking should not cascade to PlacementApplications")
    }

    val savedApplication = placementApplicationRepository.save(placementApplication)

    cas1PlacementApplicationDomainEventService.placementApplicationWithdrawn(placementApplication, withdrawalContext)
    cas1PlacementApplicationEmailService.placementApplicationWithdrawn(placementApplication, wasBeingAssessedBy)

    withdrawableService.withdrawPlacementApplicationDescendants(placementApplication, withdrawalContext)

    return CasResult.Success(savedApplication)
  }

  fun updateApplication(
    id: UUID,
    data: String,
  ): AuthorisableActionResult<ValidatableActionResult<PlacementApplicationEntity>> {
    val placementApplicationAuthorisationResult = getApplicationForUpdateOrSubmit<PlacementApplicationEntity>(id)

    if (placementApplicationAuthorisationResult is Either.Left) {
      return placementApplicationAuthorisationResult.value
    }

    val placementApplicationEntity = (placementApplicationAuthorisationResult as Either.Right).value

    placementApplicationEntity.data = data

    val savedApplication = placementApplicationRepository.save(placementApplicationEntity)

    return AuthorisableActionResult.Success(
      ValidatableActionResult.Success(savedApplication),
    )
  }

  @Transactional
  fun submitApplication(
    id: UUID,
    translatedDocument: String,
    apiPlacementType: ApiPlacementType,
    apiPlacementDates: List<ApiPlacementDates>,
  ): AuthorisableActionResult<ValidatableActionResult<List<PlacementApplicationEntity>>> {
    val placementApplicationAuthorisationResult = getApplicationForUpdateOrSubmit<List<PlacementApplicationEntity>>(id)

    if (placementApplicationAuthorisationResult is Either.Left) {
      return placementApplicationAuthorisationResult.value
    }

    val submittedPlacementApplication = (placementApplicationAuthorisationResult as Either.Right).value

    val allocatedUser = userAllocator.getUserForPlacementApplicationAllocation(submittedPlacementApplication)

    submittedPlacementApplication.apply {
      document = translatedDocument
      allocatedToUser = allocatedUser
      submittedAt = OffsetDateTime.now()
      allocatedAt = OffsetDateTime.now()
      placementType = getPlacementType(apiPlacementType)
      submissionGroupId = UUID.randomUUID()
    }

    submittedPlacementApplication.dueAt = taskDeadlineService.getDeadline(submittedPlacementApplication)

    val baselinePlacementApplication = placementApplicationRepository.save(submittedPlacementApplication)

    val placementApplicationsWithDates = if (useNewWithdrawalLogic) {
      saveDatesOnSubmissionToAnAppPerDate(baselinePlacementApplication, apiPlacementDates)
    } else {
      saveDatesOnSubmissionToASingleApp(baselinePlacementApplication, apiPlacementDates)
    }

    placementApplicationsWithDates.forEach { placementApplication ->
      cas1PlacementApplicationEmailService.placementApplicationSubmitted(placementApplication)
      if (baselinePlacementApplication.allocatedToUser != null) {
        cas1PlacementApplicationEmailService.placementApplicationAllocated(placementApplication)
      }
    }

    return AuthorisableActionResult.Success(
      ValidatableActionResult.Success(placementApplicationsWithDates),
    )
  }

  @Deprecated("This is legacy behaviour that will be removed once the new withdrawals functionality has been released")
  private fun saveDatesOnSubmissionToASingleApp(
    baselinePlacementApplication: PlacementApplicationEntity,
    apiPlacementDates: List<ApiPlacementDates>,
  ): List<PlacementApplicationEntity> {
    val placementDates = apiPlacementDates.map {
      PlacementDateEntity(
        id = UUID.randomUUID(),
        expectedArrival = it.expectedArrival,
        duration = it.duration,
        placementApplication = baselinePlacementApplication,
        createdAt = OffsetDateTime.now(),
      )
    }.toMutableList()

    placementDateRepository.saveAll(placementDates)
    baselinePlacementApplication.placementDates = placementDates
    return listOf(baselinePlacementApplication)
  }

  private fun saveDatesOnSubmissionToAnAppPerDate(
    baselinePlacementApplication: PlacementApplicationEntity,
    apiPlacementDates: List<ApiPlacementDates>,
  ): List<PlacementApplicationEntity> {
    val additionalPlacementApps = List(apiPlacementDates.size - 1) {
      placementApplicationRepository.save(
        baselinePlacementApplication.copy(id = UUID.randomUUID()),
      )
    }

    val allPlacementApps = listOf(baselinePlacementApplication) + additionalPlacementApps

    allPlacementApps.zip(apiPlacementDates) { placementApp, apiDate ->
      val placementDate = placementDateRepository.save(
        PlacementDateEntity(
          id = UUID.randomUUID(),
          expectedArrival = apiDate.expectedArrival,
          duration = apiDate.duration,
          placementApplication = placementApp,
          createdAt = OffsetDateTime.now(),
        ),
      )

      placementApp.placementDates = mutableListOf(placementDate)
    }

    return allPlacementApps
  }

  @Transactional
  fun recordDecision(
    id: UUID,
    placementApplicationDecisionEnvelope: PlacementApplicationDecisionEnvelope,
  ): AuthorisableActionResult<ValidatableActionResult<PlacementApplicationEntity>> {
    val user = userService.getUserForRequest()
    val placementApplicationEntity =
      placementApplicationRepository.findByIdOrNull(id) ?: return AuthorisableActionResult.NotFound()

    if (placementApplicationEntity.allocatedToUser != user) {
      return AuthorisableActionResult.Unauthorised()
    }

    if (placementApplicationEntity.decision != null) {
      return AuthorisableActionResult.Success(
        ValidatableActionResult.GeneralValidationError("This application has already had a decision set"),
      )
    }

    if (placementApplicationDecisionEnvelope.decision == ApiPlacementApplicationDecision.accepted) {
      val placementRequestResult =
        placementRequestService.createPlacementRequestsFromPlacementApplication(
          placementApplicationEntity,
          placementApplicationDecisionEnvelope.decisionSummary,
        )

      if (placementRequestResult is AuthorisableActionResult.NotFound) {
        return AuthorisableActionResult.NotFound(placementRequestResult.entityType, placementRequestResult.id)
      }
    }

    placementApplicationEntity.apply {
      decision = when (placementApplicationDecisionEnvelope.decision) {
        ApiPlacementApplicationDecision.accepted -> JpaPlacementApplicationDecision.ACCEPTED
        ApiPlacementApplicationDecision.rejected -> JpaPlacementApplicationDecision.REJECTED
        ApiPlacementApplicationDecision.withdraw -> JpaPlacementApplicationDecision.WITHDRAW
        ApiPlacementApplicationDecision.withdrawnByPp -> JpaPlacementApplicationDecision.WITHDRAWN_BY_PP
      }
      decisionMadeAt = OffsetDateTime.now()
    }

    val savedApplication = placementApplicationRepository.save(placementApplicationEntity)

    sendAcceptedRejectedNotification(
      placementApplicationEntity,
      placementApplicationDecisionEnvelope,
    )

    return AuthorisableActionResult.Success(
      ValidatableActionResult.Success(savedApplication),
    )
  }

  private fun sendAcceptedRejectedNotification(
    placementApplicationEntity: PlacementApplicationEntity,
    placementApplicationDecisionEnvelope: PlacementApplicationDecisionEnvelope,
  ) {
    if (!sendPlacementRequestNotifications) {
      return
    }

    val applicationCreatedBy = placementApplicationEntity.createdByUser
    applicationCreatedBy.email?.let { email ->

      val template = when (placementApplicationDecisionEnvelope.decision) {
        ApiPlacementApplicationDecision.accepted -> notifyConfig.templates.placementRequestDecisionAccepted
        ApiPlacementApplicationDecision.rejected -> notifyConfig.templates.placementRequestDecisionRejected
        ApiPlacementApplicationDecision.withdraw -> notifyConfig.templates.placementRequestDecisionRejected
        ApiPlacementApplicationDecision.withdrawnByPp -> notifyConfig.templates.placementRequestDecisionRejected
      }

      emailNotificationService.sendEmail(
        email,
        template,
        mapOf(
          "crn" to placementApplicationEntity.application.crn,
        ),
      )
    }
  }

  private fun getPlacementType(apiPlacementType: ApiPlacementType): PlacementType {
    return when (apiPlacementType) {
      ApiPlacementType.additionalPlacement -> PlacementType.ADDITIONAL_PLACEMENT
      ApiPlacementType.rotl -> PlacementType.ROTL
      ApiPlacementType.releaseFollowingDecision -> PlacementType.RELEASE_FOLLOWING_DECISION
    }
  }

  private fun setSchemaUpToDate(placementApplicationEntity: PlacementApplicationEntity): PlacementApplicationEntity {
    val latestSchema = jsonSchemaService.getNewestSchema(
      ApprovedPremisesPlacementApplicationJsonSchemaEntity::class.java,
    )

    placementApplicationEntity.schemaUpToDate = placementApplicationEntity.schemaVersion.id == latestSchema.id

    return placementApplicationEntity
  }

  private fun <T> getApplicationForUpdateOrSubmit(id: UUID): Either<AuthorisableActionResult<ValidatableActionResult<T>>, PlacementApplicationEntity> {
    val placementApplication = placementApplicationRepository.findByIdOrNull(id)
      ?: return Either.Left(AuthorisableActionResult.NotFound())
    val user = userService.getUserForRequest()

    if (placementApplication.createdByUser != user) {
      return Either.Left(AuthorisableActionResult.Unauthorised())
    }

    val validationError = confirmApplicationCanBeUpdatedOrSubmitted<T>(placementApplication)
    if (validationError != null) {
      return Either.Left(AuthorisableActionResult.Success(validationError))
    }

    return Either.Right(placementApplication)
  }

  private fun <T> confirmApplicationCanBeUpdatedOrSubmitted(
    placementApplicationEntity: PlacementApplicationEntity,
  ): ValidatableActionResultError<T>? {
    val latestSchema = jsonSchemaService.getNewestSchema(
      ApprovedPremisesPlacementApplicationJsonSchemaEntity::class.java,
    )
    placementApplicationEntity.schemaUpToDate = placementApplicationEntity.schemaVersion.id == latestSchema.id

    if (!placementApplicationEntity.schemaUpToDate) {
      return ValidatableActionResult.GeneralValidationError("The schema version is outdated")
    }

    if (placementApplicationEntity.submittedAt != null) {
      return ValidatableActionResult.GeneralValidationError("This application has already been submitted")
    }

    return null
  }
}
