package uk.gov.justice.digital.hmpps.approvedpremisesapi.controller.cas1

import com.fasterxml.jackson.databind.ObjectMapper
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.transaction.Transactional
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Appeal
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Application
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApplicationSortField
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApplicationTimelineNote
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApprovedPremisesApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApprovedPremisesApplicationStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1ApplicationSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1ExpireApplicationReason
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1TimelineEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Document
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewAppeal
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewApplicationTimelineNote
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewWithdrawal
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ReleaseTypeOption
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.RequestForPlacement
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SortDirection
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SubmitApprovedPremisesApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.UpdateApprovedPremisesApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Withdrawables
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.OfflineApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserQualification
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonInfoResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.ConflictProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.ForbiddenProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.NotFoundProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.AuthorisableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.CasResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.ApplicationService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.DocumentService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.HttpAuthService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.LaoStrategy
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.OffenderDetailService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1AppealService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1ApplicationCreationService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1ApplicationService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1ApplicationTimelineNoteService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1RequestForPlacementService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1WithdrawableService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1LaoStrategy
import uk.gov.justice.digital.hmpps.approvedpremisesapi.swagger.PaginationHeaders
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.AppealTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.ApplicationTimelineNoteTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.ApplicationsTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.DocumentTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.WithdrawableTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.ensureEntityFromCasResultIsSuccess
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.extractEntityFromCasResult
import java.net.URI
import java.util.UUID
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationSummary as DomainApprovedPremisesApplicationSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.ApprovedPremisesApplicationStatus.Companion as DomainApprovedPremisesApplicationStatus

@SuppressWarnings("TooManyFunctions", "TooGenericExceptionThrown")
@Cas1Controller
@Tag(name = "CAS1 Applications")
class Cas1ApplicationsController(
  private val cas1TimelineService: Cas1TimelineService,
  private val cas1ApplicationService: Cas1ApplicationService,
  private val userService: UserService,
  private val offenderDetailService: OffenderDetailService,
  private val applicationsTransformer: ApplicationsTransformer,
  private val httpAuthService: HttpAuthService,
  private val applicationService: ApplicationService,
  private val objectMapper: ObjectMapper,
  private val documentTransformer: DocumentTransformer,
  private val cas1WithdrawableService: Cas1WithdrawableService,
  private val cas1AppealService: Cas1AppealService,
  private val appealTransformer: AppealTransformer,
  private val cas1RequestForPlacementService: Cas1RequestForPlacementService,
  private val withdrawableTransformer: WithdrawableTransformer,
  private val cas1ApplicationTimelineNoteService: Cas1ApplicationTimelineNoteService,
  private val applicationTimelineNoteTransformer: ApplicationTimelineNoteTransformer,
  private val documentService: DocumentService,
  private val cas1ApplicationCreationService: Cas1ApplicationCreationService,
) {

  @Operation(summary = "Returns domain event summary")
  @GetMapping("/applications/{applicationId}/timeline")
  fun getApplicationTimeline(
    @PathVariable applicationId: UUID,
  ): ResponseEntity<List<Cas1TimelineEvent>> {
    val cas1timelineEvents = cas1TimelineService.getApplicationTimelineEvents(applicationId)
    return ResponseEntity.ok(cas1timelineEvents)
  }

  @PaginationHeaders
  @Operation(summary = "Lists all applications that any user has created")
  @GetMapping("/applications/all")
  fun getAllApplications(
    @RequestParam page: Int?,
    @RequestParam crnOrName: String?,
    @RequestParam sortDirection: SortDirection?,
    @RequestParam status: List<ApprovedPremisesApplicationStatus>?,
    @RequestParam sortBy: ApplicationSortField?,
    @RequestParam apAreaId: UUID?,
    @RequestParam releaseType: ReleaseTypeOption?,
    @RequestParam pageSize: Int?,
  ): ResponseEntity<List<Cas1ApplicationSummary>> {
    val user = userService.getUserForRequest()
    val statusTransformed = status?.map { DomainApprovedPremisesApplicationStatus.valueOf(it) } ?: emptyList()

    val (applications, metadata) =
      cas1ApplicationService.getAllApprovedPremisesApplications(
        page,
        crnOrName,
        sortDirection,
        statusTransformed,
        sortBy,
        apAreaId,
        releaseType?.name,
        pageSize,
      )

    return ResponseEntity.ok().headers(
      metadata?.toHeaders(),
    ).body(
      getPersonDetailAndTransformToSummary(
        applications = applications,
        laoStrategy = user.cas1LaoStrategy(),
      ),
    )
  }

  @Operation(summary = "Lists all applications that the user has created")
  @GetMapping("/applications/me")
  fun getMyApplications(): ResponseEntity<List<Cas1ApplicationSummary>> {
    val user = userService.getUserForRequest()

    val (applications, metadata) =
      cas1ApplicationService.getAllApprovedPremisesApplications(
        page = null,
        crnOrName = null,
        sortDirection = SortDirection.asc,
        status = emptyList(),
        sortBy = ApplicationSortField.createdAt,
        apAreaId = null,
        releaseType = null,
        createdByUserId = user.id,
      )

    return ResponseEntity.ok().headers(
      metadata?.toHeaders(),
    ).body(
      getPersonDetailAndTransformToSummary(
        applications = applications,
        laoStrategy = user.cas1LaoStrategy(),
      ),
    )
  }

  @GetMapping("/applications/{applicationId}")
  fun getApplication(
    @PathVariable applicationId: UUID,
  ): ResponseEntity<Application> {
    val user = userService.getUserForRequest()

    val applicationResult = cas1ApplicationService.getApplicationForUsername(applicationId, user.deliusUsername)
    // check for offlineApplication if not found
    if (applicationResult !is CasResult.NotFound) {
      val application = extractEntityFromCasResult(applicationResult)
      return ResponseEntity.ok(
        getPersonDetailAndTransform(
          application = application,
          user = user,
          ignoreLaoRestrictions = user.hasQualification(UserQualification.LAO),
        ),
      )
    } else {
      val offlineApplication = extractEntityFromCasResult(
        applicationService.getOfflineApplicationForUsername(applicationId, user.deliusUsername),
      )

      return ResponseEntity.ok(
        getPersonDetailAndTransform(
          offlineApplication = offlineApplication,
          user = user,
          ignoreLaoRestrictions = user.hasQualification(UserQualification.LAO),
        ),
      )
    }
  }

  @Operation(
    summary = "Creates an application",
    description = """deprecated for cas3, use /cas3/applications""",
  )
  @PostMapping("/applications")
  @Transactional
  fun createApplication(
    @RequestBody body: NewApplication,
  ): ResponseEntity<ApprovedPremisesApplication> {
    val user = userService.getUserForRequest()

    val personInfo =
      when (val personInfoResult = offenderDetailService.getPersonInfoResult(body.crn, user.deliusUsername, false)) {
        is PersonInfoResult.NotFound, is PersonInfoResult.Unknown -> throw NotFoundProblem(
          personInfoResult.crn,
          "Offender",
        )

        is PersonInfoResult.Success.Restricted -> throw ForbiddenProblem()
        is PersonInfoResult.Success.Full -> personInfoResult
      }

    val applicationResult = cas1ApplicationCreationService.createApprovedPremisesApplication(
      personInfo.offenderDetailSummary,
      user,
      body.convictionId,
      body.deliusEventNumber,
      body.offenceId,
    )

    val application = extractEntityFromCasResult(applicationResult)

    return ResponseEntity
      .created(URI.create("/cas1/applications/${application.id}"))
      .body(applicationsTransformer.transformCas1JpaToApi(application, personInfo))
  }

  @Operation(summary = "Updates an application")
  @PutMapping("/applications/{applicationId}")
  @Transactional
  fun updateApplication(
    @PathVariable applicationId: UUID,
    @RequestBody body: UpdateApprovedPremisesApplication,
  ): ResponseEntity<ApprovedPremisesApplication> {
    val user = userService.getUserForRequest()

    val serializedData = objectMapper.writeValueAsString(body.data)

    val applicationResult = cas1ApplicationCreationService.updateApplication(
      applicationId = applicationId,
      Cas1ApplicationCreationService.Cas1ApplicationUpdateFields(
        data = serializedData,
        isWomensApplication = body.isWomensApplication,
        isEmergencyApplication = body.isEmergencyApplication,
        apType = body.apType,
        releaseType = body.releaseType?.name,
        arrivalDate = body.arrivalDate,
        isInapplicable = body.isInapplicable,
        noticeType = body.noticeType,
      ),
      userForRequest = user,
    )

    val updatedApplication = extractEntityFromCasResult(applicationResult)

    return ResponseEntity.ok(getPersonDetailAndTransform(updatedApplication, user))
  }

  @Operation(summary = "Add a note on applications")
  @PostMapping("/applications/{applicationId}/notes")
  fun createApplicationNote(
    @PathVariable applicationId: UUID,
    @RequestBody body: NewApplicationTimelineNote,
  ): ResponseEntity<ApplicationTimelineNote> {
    val user = userService.getUserForRequest()
    val savedNote = cas1ApplicationTimelineNoteService.saveApplicationTimelineNote(applicationId, body.note, user)

    return ResponseEntity.ok(applicationTimelineNoteTransformer.transformJpaToApi(savedNote))
  }

  @Operation(summary = "Withdraws an application with a reason")
  @PostMapping("/applications/{applicationId}/withdrawal")
  fun withdrawApplication(
    @PathVariable applicationId: UUID,
    @RequestBody body: NewWithdrawal,
  ): ResponseEntity<Unit> {
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

  @Operation(summary = "Expire an application with a reason")
  @PostMapping("/applications/{applicationId}/expire")
  fun expireApplication(
    @PathVariable applicationId: UUID,
    @RequestBody body: Cas1ExpireApplicationReason,
  ): ResponseEntity<Unit> {
    val user = userService.getUserForRequest()

    return ResponseEntity.ok(
      extractEntityFromCasResult(
        cas1ApplicationService.expireApprovedPremisesApplication(
          applicationId = applicationId,
          user = user,
          expiredReason = body.reason,
        ),
      ),
    )
  }

  @Operation(summary = "Returns a list of Requests for Placement for the given application.")
  @GetMapping("/applications/{applicationId}/requests-for-placement")
  fun getRequestForPlacements(
    @PathVariable applicationId: UUID,
  ): ResponseEntity<List<RequestForPlacement>> = ResponseEntity.ok(
    extractEntityFromCasResult(cas1RequestForPlacementService.getRequestsForPlacementByApplication(applicationId, userService.getUserForRequest())),
  )

  @Operation(summary = "Submits an Application")
  @PostMapping("/applications/{applicationId}/submission")
  fun submitApplication(
    @PathVariable applicationId: UUID,
    @RequestBody submitApplication: SubmitApprovedPremisesApplication,
  ): ResponseEntity<Unit> {
    var apAreaId = submitApplication.apAreaId

    if (apAreaId == null) {
      val user = userService.getUserForRequest()
      apAreaId = user.apArea!!.id
    }
    val submitResult = cas1ApplicationCreationService.submitApplication(
      applicationId,
      submitApplication,
      userService.getUserForRequest(),
      apAreaId,
    )

    ensureEntityFromCasResultIsSuccess(submitResult)

    return ResponseEntity(HttpStatus.OK)
  }

  @Operation(summary = "Returns meta info on documents at the person level or at the Conviction level for the index Offence of this application.")
  @GetMapping("/applications/{applicationId}/documents")
  fun getDocuments(
    @PathVariable applicationId: UUID,
  ): ResponseEntity<List<Document>> {
    val deliusPrincipal = httpAuthService.getDeliusPrincipalOrThrow()
    val username = deliusPrincipal.name
    val application = extractEntityFromCasResult(applicationService.getApplicationForUsername(applicationId, username))

    val documentsResult = extractEntityFromCasResult(
      documentService.getDocumentsFromApDeliusApi(application.crn),
    )
    val apiDocuments = documentTransformer.transformToApi(documentsResult)

    return ResponseEntity(apiDocuments, HttpStatus.OK)
  }

  @Operation(summary = "Get an appeal on an application")
  @GetMapping("/applications/{applicationId}/appeals/{appealId}")
  fun getAppeal(
    @PathVariable applicationId: UUID,
    @PathVariable appealId: UUID,
  ): ResponseEntity<Appeal> {
    val user = userService.getUserForRequest()
    val application =
      extractEntityFromCasResult(applicationService.getApplicationForUsername(applicationId, user.deliusUsername))

    val appeal = when (val getAppealResult = cas1AppealService.getAppeal(appealId, application)) {
      is AuthorisableActionResult.NotFound -> throw NotFoundProblem(appealId, "Appeal")
      is AuthorisableActionResult.Unauthorised -> throw ForbiddenProblem()
      is AuthorisableActionResult.Success -> getAppealResult.entity
    }
    return ResponseEntity.ok(appealTransformer.transformJpaToApi(appeal))
  }

  @Operation(summary = "Add an appeal to an application")
  @PostMapping("/applications/{applicationId}/appeals")
  fun createAppeal(
    @PathVariable applicationId: UUID,
    @RequestBody body: NewAppeal,
  ): ResponseEntity<Appeal> {
    val user = userService.getUserForRequest()
    val application =
      extractEntityFromCasResult(applicationService.getApplicationForUsername(applicationId, user.deliusUsername))

    if (application !is ApprovedPremisesApplicationEntity) {
      throw ConflictProblem(applicationId, "Only CAS1 applications are supported")
    }

    val assessment = application.getLatestAssessment()
      ?: throw ConflictProblem(
        applicationId,
        "An appeal cannot be created when the application does not have an assessment",
      )

    val createAppealResult = cas1AppealService.createAppeal(
      appealDate = body.appealDate,
      appealDetail = body.appealDetail,
      decision = body.decision,
      decisionDetail = body.decisionDetail,
      application = application,
      assessment = assessment,
      createdBy = user,
    )

    val appeal = extractEntityFromCasResult(createAppealResult)

    return ResponseEntity
      .created(URI.create("/applications/${application.id}/appeals/${appeal.id}"))
      .body(appealTransformer.transformJpaToApi(appeal))
  }

  @Operation(summary = "Returns a list of withdrawable items associated with this application, including the application itself, if withdrawable")
  @GetMapping("/applications/{applicationId}/withdrawablesWithNotes")
  fun getWithdrawablesWithNotes(
    @PathVariable applicationId: UUID,
  ): ResponseEntity<Withdrawables> {
    val user = userService.getUserForRequest()
    val application =
      extractEntityFromCasResult(applicationService.getApplicationForUsername(applicationId, user.deliusUsername))

    if (application !is ApprovedPremisesApplicationEntity) {
      throw RuntimeException("Unsupported Application type: ${application::class.qualifiedName}")
    }

    val result = cas1WithdrawableService.allDirectlyWithdrawables(application, user)

    return ResponseEntity.ok(
      Withdrawables(
        notes = result.notes,
        withdrawables = result.withdrawables.map { withdrawableTransformer.toApi(it) },
      ),
    )
  }

  private fun getPersonDetailAndTransform(
    application: ApprovedPremisesApplicationEntity,
    user: UserEntity,
    ignoreLaoRestrictions: Boolean = false,
  ): ApprovedPremisesApplication {
    val personInfo = offenderDetailService.getPersonInfoResult(application.crn, user.deliusUsername, ignoreLaoRestrictions)

    return applicationsTransformer.transformCas1JpaToApi(application, personInfo)
  }

  private fun getPersonDetailAndTransformToSummary(
    applications: List<DomainApprovedPremisesApplicationSummary>,
    laoStrategy: LaoStrategy,
  ): List<Cas1ApplicationSummary> {
    val crns = applications.map { it.getCrn() }
    val personInfoResults = offenderDetailService.getPersonInfoResults(crns.toSet(), laoStrategy)

    return applications.map {
      val crn = it.getCrn()
      applicationsTransformer.transformDomainToCas1ApplicationSummary(
        it,
        personInfoResults.firstOrNull { it.crn == crn } ?: PersonInfoResult.Unknown(crn),
      )
    }
  }
  private fun getPersonDetailAndTransform(
    offlineApplication: OfflineApplicationEntity,
    user: UserEntity,
    ignoreLaoRestrictions: Boolean = false,
  ): Application {
    val personInfo = offenderDetailService.getPersonInfoResult(offlineApplication.crn, user.deliusUsername, ignoreLaoRestrictions)

    return applicationsTransformer.transformJpaToApi(offlineApplication, personInfo)
  }
}
