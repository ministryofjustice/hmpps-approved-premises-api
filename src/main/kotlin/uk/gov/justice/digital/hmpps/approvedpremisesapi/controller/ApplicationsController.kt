package uk.gov.justice.digital.hmpps.approvedpremisesapi.controller

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.ApplicationsApiDelegate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Appeal
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Application
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApplicationSortField
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApplicationSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApplicationTimelineNote
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApprovedPremisesApplicationStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Assessment
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.DatePeriod
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Document
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewAppeal
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewApplicationTimelineNote
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewWithdrawal
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PlacementApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ReleaseTypeOption
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.RequestForPlacement
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SortDirection
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SubmitApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SubmitApprovedPremisesApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SubmitTemporaryAccommodationApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.TimelineEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.UpdateApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.UpdateApprovedPremisesApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.UpdateTemporaryAccommodationApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Withdrawable
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.WithdrawableType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.OfflineApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonInfoResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.BadRequestProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.ConflictProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.ForbiddenProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.NotFoundProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.NotImplementedProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.AuthorisableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.ValidatableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.AppealService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.ApplicationService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.AssessmentService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.HttpAuthService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.OffenderService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.PlacementApplicationService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.PlacementRequestService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.RequestForPlacementService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1WithdrawableService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.WithdrawableEntityType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.AppealTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.ApplicationsTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.AssessmentTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.DocumentTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.PlacementApplicationTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.extractEntityFromCasResult
import java.net.URI
import java.util.UUID
import javax.transaction.Transactional
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationSummary as JPAApplicationSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.ApprovedPremisesApplicationStatus.Companion as DomainApprovedPremisesApplicationStatus

@Service
@Suppress("LongParameterList", "TooManyFunctions")
class ApplicationsController(
  private val httpAuthService: HttpAuthService,
  private val applicationService: ApplicationService,
  private val placementApplicationService: PlacementApplicationService,
  private val applicationsTransformer: ApplicationsTransformer,
  private val assessmentTransformer: AssessmentTransformer,
  private val placementApplicationTransformer: PlacementApplicationTransformer,
  private val objectMapper: ObjectMapper,
  private val offenderService: OffenderService,
  private val documentTransformer: DocumentTransformer,
  private val assessmentService: AssessmentService,
  private val userService: UserService,
  private val cas1WithdrawableService: Cas1WithdrawableService,
  private val appealService: AppealService,
  private val appealTransformer: AppealTransformer,
  private val placementRequestService: PlacementRequestService,
  private val requestForPlacementService: RequestForPlacementService,
) : ApplicationsApiDelegate {

  override fun applicationsGet(xServiceName: ServiceName?): ResponseEntity<List<ApplicationSummary>> {
    val serviceName = xServiceName ?: ServiceName.approvedPremises

    val user = userService.getUserForRequest()

    val applications = applicationService.getAllApplicationsForUsername(user.deliusUsername, serviceName)

    return ResponseEntity.ok(applications.map { getPersonDetailAndTransformToSummary(it, user) })
  }

  override fun applicationsAllGet(
    xServiceName: ServiceName,
    page: Int?,
    crnOrName: String?,
    sortDirection: SortDirection?,
    status: List<ApprovedPremisesApplicationStatus>?,
    sortBy: ApplicationSortField?,
    apAreaId: UUID?,
    releaseType: ReleaseTypeOption?,
  ): ResponseEntity<List<ApplicationSummary>> {
    if (xServiceName != ServiceName.approvedPremises) {
      throw ForbiddenProblem()
    }
    val user = userService.getUserForRequest()
    val statusTransformed = status?.map { DomainApprovedPremisesApplicationStatus.valueOf(it) } ?: emptyList()

    val (applications, metadata) =
      applicationService.getAllApprovedPremisesApplications(
        page,
        crnOrName,
        sortDirection,
        statusTransformed,
        sortBy,
        apAreaId,
        releaseType?.name,
      )

    return ResponseEntity.ok().headers(
      metadata?.toHeaders(),
    ).body(
      applications.map {
        getPersonDetailAndTransformToSummary(it, user)
      },
    )
  }

  override fun applicationsApplicationIdGet(applicationId: UUID): ResponseEntity<Application> {
    val user = userService.getUserForRequest()

    val application = when (
      val applicationResult =
        applicationService.getApplicationForUsername(applicationId, user.deliusUsername)
    ) {
      is AuthorisableActionResult.NotFound -> null
      is AuthorisableActionResult.Unauthorised -> throw ForbiddenProblem()
      is AuthorisableActionResult.Success -> applicationResult.entity
    }

    if (application != null) {
      return ResponseEntity.ok(getPersonDetailAndTransform(application, user))
    }

    val offlineApplication = when (
      val offlineApplicationResult =
        applicationService.getOfflineApplicationForUsername(applicationId, user.deliusUsername)
    ) {
      is AuthorisableActionResult.Unauthorised -> throw ForbiddenProblem()
      is AuthorisableActionResult.NotFound -> throw NotFoundProblem(applicationId, "Application")
      is AuthorisableActionResult.Success -> offlineApplicationResult.entity
    }

    return ResponseEntity.ok(getPersonDetailAndTransform(offlineApplication, user))
  }

  @Transactional
  override fun applicationsPost(body: NewApplication, xServiceName: ServiceName?, createWithRisks: Boolean?):
    ResponseEntity<Application> {
    val user = userService.getUserForRequest()

    val personInfo =
      when (val personInfoResult = offenderService.getInfoForPerson(body.crn, user.deliusUsername, false)) {
        is PersonInfoResult.NotFound, is PersonInfoResult.Unknown -> throw NotFoundProblem(
          personInfoResult.crn,
          "Offender",
        )

        is PersonInfoResult.Success.Restricted -> throw ForbiddenProblem()
        is PersonInfoResult.Success.Full -> personInfoResult
      }

    val applicationResult = createApplication(
      xServiceName ?: ServiceName.approvedPremises,
      personInfo,
      user,
      body,
      createWithRisks,
    )

    val application = when (applicationResult) {
      is ValidatableActionResult.GeneralValidationError ->
        throw BadRequestProblem(errorDetail = applicationResult.message)

      is ValidatableActionResult.FieldValidationError ->
        throw BadRequestProblem(invalidParams = applicationResult.validationMessages)

      is ValidatableActionResult.ConflictError ->
        throw ConflictProblem(id = applicationResult.conflictingEntityId, conflictReason = applicationResult.message)

      is ValidatableActionResult.Success -> applicationResult.entity
    }

    return ResponseEntity
      .created(URI.create("/applications/${application.id}"))
      .body(applicationsTransformer.transformJpaToApi(application, personInfo))
  }

  private fun createApplication(
    serviceName: ServiceName,
    personInfo: PersonInfoResult.Success.Full,
    user: UserEntity,
    body: NewApplication,
    createWithRisks: Boolean?,
  ): ValidatableActionResult<ApplicationEntity> = when (serviceName) {
    ServiceName.approvedPremises ->
      applicationService.createApprovedPremisesApplication(
        personInfo.offenderDetailSummary,
        user,
        body.convictionId,
        body.deliusEventNumber,
        body.offenceId,
        createWithRisks,
      )

    ServiceName.temporaryAccommodation -> {
      when (
        val actionResult =
          applicationService.createTemporaryAccommodationApplication(
            body.crn,
            user,
            body.convictionId,
            body.deliusEventNumber,
            body.offenceId,
            createWithRisks,
            personInfo,
          )
      ) {
        is AuthorisableActionResult.NotFound -> throw NotFoundProblem(actionResult.id!!, actionResult.entityType!!)
        is AuthorisableActionResult.Unauthorised -> throw ForbiddenProblem()
        is AuthorisableActionResult.Success -> actionResult.entity
      }
    }

    ServiceName.cas2 -> throw RuntimeException(
      "CAS2 now has its own " +
        "Cas2ApplicationsController",
    )
  }

  @Transactional
  override fun applicationsApplicationIdPut(applicationId: UUID, body: UpdateApplication): ResponseEntity<Application> {
    val user = userService.getUserForRequest()

    val serializedData = objectMapper.writeValueAsString(body.data)

    val applicationResult = when (body) {
      is UpdateApprovedPremisesApplication -> applicationService.updateApprovedPremisesApplication(
        applicationId = applicationId,
        ApplicationService.Cas1ApplicationUpdateFields(
          data = serializedData,
          isWomensApplication = body.isWomensApplication,
          isPipeApplication = body.isPipeApplication,
          isEmergencyApplication = body.isEmergencyApplication,
          isEsapApplication = body.isEsapApplication,
          apType = body.apType,
          releaseType = body.releaseType?.name,
          arrivalDate = body.arrivalDate,
          isInapplicable = body.isInapplicable,
          noticeType = body.noticeType,
        ),
        userForRequest = user,
      )

      is UpdateTemporaryAccommodationApplication -> applicationService.updateTemporaryAccommodationApplication(
        applicationId = applicationId,
        data = serializedData,
      )

      else -> throw RuntimeException("Unsupported UpdateApplication type: ${body::class.qualifiedName}")
    }

    val validationResult = when (applicationResult) {
      is AuthorisableActionResult.NotFound -> throw NotFoundProblem(applicationId, "Application")
      is AuthorisableActionResult.Unauthorised -> throw ForbiddenProblem()
      is AuthorisableActionResult.Success -> applicationResult.entity
    }

    val updatedApplication = when (validationResult) {
      is ValidatableActionResult.GeneralValidationError ->
        throw BadRequestProblem(errorDetail = validationResult.message)

      is ValidatableActionResult.FieldValidationError ->
        throw BadRequestProblem(invalidParams = validationResult.validationMessages)

      is ValidatableActionResult.ConflictError ->
        throw ConflictProblem(id = validationResult.conflictingEntityId, conflictReason = validationResult.message)

      is ValidatableActionResult.Success -> validationResult.entity
    }

    return ResponseEntity.ok(getPersonDetailAndTransform(updatedApplication, user))
  }

  override fun applicationsApplicationIdNotesPost(
    applicationId: UUID,
    body: NewApplicationTimelineNote,
  ): ResponseEntity<ApplicationTimelineNote> {
    val user = userService.getUserForRequest()
    val savedNote = applicationService.addNoteToApplication(applicationId, body.note, user)
    return ResponseEntity.ok(savedNote)
  }

  override fun applicationsApplicationIdWithdrawalPost(applicationId: UUID, body: NewWithdrawal): ResponseEntity<Unit> {
    val user = userService.getUserForRequest()

    return ResponseEntity.ok(
      extractEntityFromCasResult(
        cas1WithdrawableService.withdrawApplication(
          applicationId = applicationId,
          user = user,
          withdrawalReason = body.reason.value,
          otherReason = body.otherReason,
        ),
      ),
    )
  }

  override fun applicationsApplicationIdTimelineGet(applicationId: UUID, xServiceName: ServiceName):
    ResponseEntity<List<TimelineEvent>> {
    if (xServiceName != ServiceName.approvedPremises) {
      throw NotImplementedProblem("Timeline is only supported for Approved Premises applications")
    }
    val events = applicationService.getApplicationTimeline(applicationId)
    return ResponseEntity(events, HttpStatus.OK)
  }

  override fun applicationsApplicationIdRequestsForPlacementGet(applicationId: UUID): ResponseEntity<List<RequestForPlacement>> {
    val requestsForPlacement = when (val result = requestForPlacementService.getRequestsForPlacementByApplication(applicationId, userService.getUserForRequest())) {
      is AuthorisableActionResult.Success -> result.entity
      is AuthorisableActionResult.NotFound -> throw NotFoundProblem(applicationId, "Application")
      is AuthorisableActionResult.Unauthorised -> throw ForbiddenProblem()
    }

    return ResponseEntity.ok(requestsForPlacement)
  }

  override fun applicationsApplicationIdRequestsForPlacementRequestForPlacementIdGet(
    applicationId: UUID,
    requestForPlacementId: UUID,
  ): ResponseEntity<RequestForPlacement> {
    val application = applicationService.getApplication(applicationId) ?: throw NotFoundProblem(applicationId, "Application")

    val requestForPlacement = when (val result = requestForPlacementService.getRequestForPlacement(application, requestForPlacementId, userService.getUserForRequest())) {
      is AuthorisableActionResult.Success -> result.entity
      is AuthorisableActionResult.NotFound -> throw NotFoundProblem(applicationId, "RequestForPlacement")
      is AuthorisableActionResult.Unauthorised -> throw ForbiddenProblem()
    }

    return ResponseEntity.ok(requestForPlacement)
  }

  override fun applicationsApplicationIdSubmissionPost(
    applicationId: UUID,
    submitApplication: SubmitApplication,
  ): ResponseEntity<Unit> {
    val deliusPrincipal = httpAuthService.getDeliusPrincipalOrThrow()
    val username = deliusPrincipal.name

    val submitResult = when (submitApplication) {
      is SubmitApprovedPremisesApplication -> {
        var apAreaId = submitApplication.apAreaId

        if (apAreaId == null) {
          val user = userService.getUserForRequest()
          apAreaId = user.apArea!!.id
        }
        applicationService.submitApprovedPremisesApplication(
          applicationId,
          submitApplication,
          username,
          apAreaId,
        )
      }

      is SubmitTemporaryAccommodationApplication ->
        applicationService.submitTemporaryAccommodationApplication(applicationId, submitApplication)

      else -> throw RuntimeException("Unsupported SubmitApplication type: ${submitApplication::class.qualifiedName}")
    }

    val validationResult = when (submitResult) {
      is AuthorisableActionResult.NotFound -> throw NotFoundProblem(applicationId, "Application")
      is AuthorisableActionResult.Unauthorised -> throw ForbiddenProblem()
      is AuthorisableActionResult.Success -> submitResult.entity
    }

    when (validationResult) {
      is ValidatableActionResult.GeneralValidationError ->
        throw BadRequestProblem(errorDetail = validationResult.message)

      is ValidatableActionResult.FieldValidationError ->
        throw BadRequestProblem(invalidParams = validationResult.validationMessages)

      is ValidatableActionResult.ConflictError ->
        throw ConflictProblem(id = validationResult.conflictingEntityId, conflictReason = validationResult.message)

      is ValidatableActionResult.Success -> Unit
    }

    return ResponseEntity(HttpStatus.OK)
  }

  override fun applicationsApplicationIdDocumentsGet(applicationId: UUID): ResponseEntity<List<Document>> {
    val deliusPrincipal = httpAuthService.getDeliusPrincipalOrThrow()
    val username = deliusPrincipal.name

    val application = when (
      val applicationResult =
        applicationService.getApplicationForUsername(applicationId, username)
    ) {
      is AuthorisableActionResult.NotFound -> throw NotFoundProblem(applicationId, "Application")
      is AuthorisableActionResult.Unauthorised -> throw ForbiddenProblem()
      is AuthorisableActionResult.Success -> applicationResult.entity
    }

    val convictionId = when (application) {
      is ApprovedPremisesApplicationEntity -> application.convictionId
      is TemporaryAccommodationApplicationEntity -> application.convictionId
      else -> throw RuntimeException("Unsupported Application type: ${application::class.qualifiedName}")
    }

    val documents = when (val documentsResult = offenderService.getDocuments(application.crn)) {
      is AuthorisableActionResult.NotFound -> throw NotFoundProblem(application.crn, "Person")
      is AuthorisableActionResult.Unauthorised -> throw ForbiddenProblem()
      is AuthorisableActionResult.Success -> documentsResult.entity
    }

    return ResponseEntity(documentTransformer.transformToApi(documents, convictionId), HttpStatus.OK)
  }

  override fun applicationsApplicationIdAppealsAppealIdGet(
    applicationId: UUID,
    appealId: UUID,
  ): ResponseEntity<Appeal> {
    val user = userService.getUserForRequest()
    val applicationResult = applicationService.getApplicationForUsername(applicationId, user.deliusUsername)

    val application = when (applicationResult) {
      is AuthorisableActionResult.NotFound -> throw NotFoundProblem(applicationId, "Application")
      is AuthorisableActionResult.Unauthorised -> throw ForbiddenProblem()
      is AuthorisableActionResult.Success -> applicationResult.entity
    }

    val appeal = when (val getAppealResult = appealService.getAppeal(appealId, application)) {
      is AuthorisableActionResult.NotFound -> throw NotFoundProblem(appealId, "Appeal")
      is AuthorisableActionResult.Unauthorised -> throw ForbiddenProblem()
      is AuthorisableActionResult.Success -> getAppealResult.entity
    }
    return ResponseEntity.ok(appealTransformer.transformJpaToApi(appeal))
  }

  override fun applicationsApplicationIdAppealsPost(applicationId: UUID, body: NewAppeal): ResponseEntity<Appeal> {
    val user = userService.getUserForRequest()
    val applicationResult = applicationService.getApplicationForUsername(applicationId, user.deliusUsername)

    val application = when (applicationResult) {
      is AuthorisableActionResult.NotFound -> throw NotFoundProblem(applicationId, "Application")
      is AuthorisableActionResult.Unauthorised -> throw ForbiddenProblem()
      is AuthorisableActionResult.Success -> applicationResult.entity
    }

    val assessment = application.getLatestAssessment()
      ?: throw ConflictProblem(
        applicationId,
        "An appeal cannot be created when the application does not have an assessment",
      )

    val createAppealResult = appealService.createAppeal(
      appealDate = body.appealDate,
      appealDetail = body.appealDetail,
      decision = body.decision,
      decisionDetail = body.decisionDetail,
      application = application,
      assessment = assessment,
      createdBy = user,
    )

    val validationResult = when (createAppealResult) {
      is AuthorisableActionResult.NotFound -> throw NotFoundProblem(applicationId, "Application")
      is AuthorisableActionResult.Unauthorised -> throw ForbiddenProblem()
      is AuthorisableActionResult.Success -> createAppealResult.entity
    }

    val appeal = when (validationResult) {
      is ValidatableActionResult.GeneralValidationError -> throw BadRequestProblem(errorDetail = validationResult.message)
      is ValidatableActionResult.FieldValidationError -> throw BadRequestProblem(invalidParams = validationResult.validationMessages)
      is ValidatableActionResult.ConflictError -> throw ConflictProblem(id = validationResult.conflictingEntityId, conflictReason = validationResult.message)
      is ValidatableActionResult.Success -> validationResult.entity
    }

    return ResponseEntity
      .created(URI.create("/applications/${application.id}/appeals/${appeal.id}"))
      .body(appealTransformer.transformJpaToApi(appeal))
  }

  override fun applicationsApplicationIdAssessmentGet(applicationId: UUID): ResponseEntity<Assessment> {
    val user = userService.getUserForRequest()

    val assessment = when (
      val applicationResult =
        assessmentService.getAssessmentForUserAndApplication(
          user,
          applicationId,
        )
    ) {
      is AuthorisableActionResult.NotFound -> throw NotFoundProblem(applicationId, "Assessment")
      is AuthorisableActionResult.Unauthorised -> throw ForbiddenProblem()
      is AuthorisableActionResult.Success -> applicationResult.entity
    }

    val personInfo = offenderService.getInfoForPerson(assessment.application.crn, user.deliusUsername, false)

    return ResponseEntity.ok(assessmentTransformer.transformJpaToApi(assessment, personInfo))
  }

  override fun applicationsApplicationIdPlacementApplicationsGet(
    applicationId: UUID,
    xServiceName: ServiceName,
    includeInitialRequestForPlacement: Boolean?,
  ): ResponseEntity<List<PlacementApplication>> {
    if (xServiceName != ServiceName.approvedPremises) {
      throw ForbiddenProblem()
    }

    val initialPlacementRequests = if (includeInitialRequestForPlacement == true) {
      placementRequestService.getPlacementRequestForInitialApplicationDates(applicationId).map {
        placementApplicationTransformer.transformPlacementRequestJpaToApi(it)
      }
    } else { emptyList() }

    val placementApplicationEntities =
      placementApplicationService.getAllActivePlacementApplicationsForApplicationId(applicationId)
    val additionalPlacementRequests = placementApplicationEntities.map {
      placementApplicationTransformer.transformJpaToApi(it)
    }

    return ResponseEntity.ok(initialPlacementRequests.plus(additionalPlacementRequests))
  }

  override fun applicationsApplicationIdWithdrawablesGet(
    applicationId: UUID,
    xServiceName: ServiceName,
  ): ResponseEntity<List<Withdrawable>> {
    if (xServiceName != ServiceName.approvedPremises) {
      throw ForbiddenProblem()
    }

    val user = userService.getUserForRequest()

    val application = when (
      val applicationResult = applicationService.getApplicationForUsername(applicationId, user.deliusUsername)
    ) {
      is AuthorisableActionResult.NotFound -> throw NotFoundProblem(applicationId, "Application")
      is AuthorisableActionResult.Unauthorised -> throw ForbiddenProblem()
      is AuthorisableActionResult.Success -> applicationResult.entity
    }

    if (application !is ApprovedPremisesApplicationEntity) {
      throw RuntimeException("Unsupported Application type: ${application::class.qualifiedName}")
    }

    val withdrawables = cas1WithdrawableService.allDirectlyWithdrawables(application, user)

    return ResponseEntity.ok(
      withdrawables.map { entity ->
        Withdrawable(
          entity.id,
          when (entity.type) {
            WithdrawableEntityType.Application -> WithdrawableType.application
            WithdrawableEntityType.PlacementRequest -> WithdrawableType.placementRequest
            WithdrawableEntityType.PlacementApplication -> WithdrawableType.placementApplication
            WithdrawableEntityType.Booking -> WithdrawableType.booking
          },
          entity.dates.map { DatePeriod(it.startDate, it.endDate) },
        )
      },
    )
  }

  private fun getPersonDetailAndTransform(application: ApplicationEntity, user: UserEntity): Application {
    val personInfo = offenderService.getInfoForPerson(application.crn, user.deliusUsername, false)

    return applicationsTransformer.transformJpaToApi(application, personInfo)
  }

  private fun getPersonDetailAndTransformToSummary(
    application: JPAApplicationSummary,
    user: UserEntity,
  ): ApplicationSummary {
    val personInfo = offenderService.getInfoForPerson(application.getCrn(), user.deliusUsername, false)

    return applicationsTransformer.transformDomainToApiSummary(application, personInfo)
  }

  private fun getPersonDetailAndTransform(offlineApplication: OfflineApplicationEntity, user: UserEntity): Application {
    val personInfo = offenderService.getInfoForPerson(offlineApplication.crn, user.deliusUsername, false)

    return applicationsTransformer.transformJpaToApi(offlineApplication, personInfo)
  }
}
