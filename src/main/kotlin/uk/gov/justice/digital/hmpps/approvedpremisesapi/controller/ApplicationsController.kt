package uk.gov.justice.digital.hmpps.approvedpremisesapi.controller

import com.fasterxml.jackson.databind.ObjectMapper
import io.swagger.v3.oas.annotations.Operation
import jakarta.transaction.Transactional
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Appeal
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Application
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApplicationTimelineNote
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Document
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewAppeal
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewApplicationTimelineNote
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewWithdrawal
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.RequestForPlacement
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.UpdateApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.UpdateApprovedPremisesApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.UpdateTemporaryAccommodationApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Withdrawables
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.deliuscontext.APDeliusDocument
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationEntity
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
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.OffenderDetailService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1AppealService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1ApplicationCreationService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1ApplicationTimelineNoteService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1RequestForPlacementService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1WithdrawableService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.WithdrawableEntitiesWithNotes
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.AppealTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.ApplicationTimelineNoteTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.ApplicationsTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.DocumentTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.WithdrawableTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.extractEntityFromCasResult
import java.net.URI
import java.util.UUID

@RestController
@RequestMapping("\${openapi.approvedPremises.base-path:}")
@Suppress("LongParameterList", "TooManyFunctions")
class ApplicationsController(
  private val httpAuthService: HttpAuthService,
  private val applicationService: ApplicationService,
  private val applicationsTransformer: ApplicationsTransformer,
  private val objectMapper: ObjectMapper,
  private val documentTransformer: DocumentTransformer,
  private val userService: UserService,
  private val cas1WithdrawableService: Cas1WithdrawableService,
  private val cas1AppealService: Cas1AppealService,
  private val appealTransformer: AppealTransformer,
  private val cas1RequestForPlacementService: Cas1RequestForPlacementService,
  private val withdrawableTransformer: WithdrawableTransformer,
  private val cas1ApplicationTimelineNoteService: Cas1ApplicationTimelineNoteService,
  private val applicationTimelineNoteTransformer: ApplicationTimelineNoteTransformer,
  private val documentService: DocumentService,
  private val cas1ApplicationCreationService: Cas1ApplicationCreationService,
  private val offenderDetailService: OffenderDetailService,
) {
  @Operation(
    summary = "Gets a single application by its ID",
    description = """deprecated for cas3, use /cas3/applications/{applicationId}""",
  )
  @GetMapping("/applications/{applicationId}")
  fun applicationsApplicationIdGet(
    @PathVariable applicationId: UUID,
  ): ResponseEntity<Application> {
    val user = userService.getUserForRequest()

    val applicationResult = applicationService.getApplicationForUsername(applicationId, user.deliusUsername)
    // check for offlineApplication if not found
    if (applicationResult !is CasResult.NotFound) {
      val application = extractEntityFromCasResult(applicationResult)
      return ResponseEntity.ok(
        getPersonDetailAndTransform(
          application = application,
          user = user,
          ignoreLaoRestrictions = application is ApprovedPremisesApplicationEntity &&
            user.hasQualification(
              UserQualification.LAO,
            ),
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
  fun applicationsPost(
    @RequestBody body: NewApplication,
    @RequestHeader(value = "X-Service-Name", required = false) xServiceName: ServiceName?,
    @RequestParam(value = "createWithRisks", required = false) createWithRisks: Boolean?,
  ): ResponseEntity<Application> {
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

    val applicationResult = createApplication(
      xServiceName ?: ServiceName.approvedPremises,
      personInfo,
      user,
      body,
      createWithRisks,
    )

    val application = extractEntityFromCasResult(applicationResult)

    return ResponseEntity
      .created(URI.create("/applications/${application.id}"))
      .body(applicationsTransformer.transformJpaToApi(application, personInfo))
  }

  @Suppress("TooGenericExceptionThrown")
  private fun createApplication(
    serviceName: ServiceName,
    personInfo: PersonInfoResult.Success.Full,
    user: UserEntity,
    body: NewApplication,
    createWithRisks: Boolean?,
  ): CasResult<out ApplicationEntity> = when (serviceName) {
    ServiceName.approvedPremises ->
      cas1ApplicationCreationService.createApprovedPremisesApplication(
        personInfo.offenderDetailSummary,
        user,
        body.convictionId,
        body.deliusEventNumber,
        body.offenceId,
      )

    ServiceName.temporaryAccommodation -> {
      applicationService.createTemporaryAccommodationApplication(
        body.crn,
        user,
        body.convictionId,
        body.deliusEventNumber,
        body.offenceId,
        createWithRisks,
        personInfo,
      )
    }

    ServiceName.cas2 -> throw RuntimeException(
      "CAS2 now has its own " +
        "Cas2ApplicationsController",
    )

    ServiceName.cas2v2 -> throw RuntimeException(
      "CAS2v2 now has its own " +
        "Cas2v2ApplicationsController",
    )
  }

  @Operation(summary = "Updates an application")
  @PutMapping("/applications/{applicationId}")
  @Transactional
  fun applicationsApplicationIdPut(
    @PathVariable applicationId: UUID,
    @RequestBody body: UpdateApplication,
  ): ResponseEntity<Application> {
    val user = userService.getUserForRequest()

    val serializedData = objectMapper.writeValueAsString(body.data)

    val applicationResult = when (body) {
      is UpdateApprovedPremisesApplication -> cas1ApplicationCreationService.updateApplication(
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

      is UpdateTemporaryAccommodationApplication -> applicationService.updateTemporaryAccommodationApplication(
        applicationId = applicationId,
        data = serializedData,
      )

      else -> throw RuntimeException("Unsupported UpdateApplication type: ${body::class.qualifiedName}")
    }

    val updatedApplication = extractEntityFromCasResult(applicationResult)

    return ResponseEntity.ok(getPersonDetailAndTransform(updatedApplication, user))
  }

  @Operation(summary = "Add a note on applications")
  @PostMapping("/applications/{applicationId}/notes")
  fun applicationsApplicationIdNotesPost(
    @PathVariable applicationId: UUID,
    @RequestBody body: NewApplicationTimelineNote,
  ): ResponseEntity<ApplicationTimelineNote> {
    val user = userService.getUserForRequest()
    val savedNote = cas1ApplicationTimelineNoteService.saveApplicationTimelineNote(applicationId, body.note, user)

    return ResponseEntity.ok(applicationTimelineNoteTransformer.transformJpaToApi(savedNote))
  }

  @Operation(summary = "Withdraws an application with a reason")
  @PostMapping("/applications/{applicationId}/withdrawal")
  fun applicationsApplicationIdWithdrawalPost(
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

  @Operation(summary = "Returns a list of Requests for Placement for the given application.")
  @GetMapping("/applications/{applicationId}/requests-for-placement")
  fun applicationsApplicationIdRequestsForPlacementGet(
    @PathVariable applicationId: UUID,
  ): ResponseEntity<List<RequestForPlacement>> = ResponseEntity.ok(
    extractEntityFromCasResult(cas1RequestForPlacementService.getRequestsForPlacementByApplication(applicationId, userService.getUserForRequest())),
  )

  @Operation(summary = "Returns meta info on documents at the person level or at the Conviction level for the index Offence of this application.")
  @GetMapping("/applications/{applicationId}/documents")
  fun applicationsApplicationIdDocumentsGet(
    @PathVariable applicationId: UUID,
  ): ResponseEntity<List<Document>> {
    val deliusPrincipal = httpAuthService.getDeliusPrincipalOrThrow()
    val username = deliusPrincipal.name
    val application = extractEntityFromCasResult(applicationService.getApplicationForUsername(applicationId, username))

    val documentsResult = getDocuments(application.crn)
    val apiDocuments = documentTransformer.transformToApi(documentsResult)

    return ResponseEntity(apiDocuments, HttpStatus.OK)
  }

  private fun getDocuments(crn: String): List<APDeliusDocument> = extractEntityFromCasResult(
    documentService.getDocumentsFromApDeliusApi(crn),
  )

  @Operation(summary = "Get an appeal on an application")
  @GetMapping("/applications/{applicationId}/appeals/{appealId}")
  fun applicationsApplicationIdAppealsAppealIdGet(
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
  fun applicationsApplicationIdAppealsPost(
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
  fun applicationsApplicationIdWithdrawablesWithNotesGet(
    @PathVariable applicationId: UUID,
    @RequestHeader(value = "X-Service-Name", required = true) xServiceName: ServiceName,
  ): ResponseEntity<Withdrawables> {
    val result = getWithdrawables(applicationId, xServiceName)

    return ResponseEntity.ok(
      Withdrawables(
        notes = result.notes,
        withdrawables = result.withdrawables.map { withdrawableTransformer.toApi(it) },
      ),
    )
  }

  @SuppressWarnings("ThrowsCount")
  private fun getWithdrawables(applicationId: UUID, xServiceName: ServiceName): WithdrawableEntitiesWithNotes {
    if (xServiceName != ServiceName.approvedPremises) {
      throw ForbiddenProblem()
    }

    val user = userService.getUserForRequest()
    val application =
      extractEntityFromCasResult(applicationService.getApplicationForUsername(applicationId, user.deliusUsername))

    if (application !is ApprovedPremisesApplicationEntity) {
      throw RuntimeException("Unsupported Application type: ${application::class.qualifiedName}")
    }

    return cas1WithdrawableService.allDirectlyWithdrawables(application, user)
  }

  private fun getPersonDetailAndTransform(
    application: ApplicationEntity,
    user: UserEntity,
    ignoreLaoRestrictions: Boolean = false,
  ): Application {
    val personInfo = offenderDetailService.getPersonInfoResult(application.crn, user.deliusUsername, ignoreLaoRestrictions)

    return applicationsTransformer.transformJpaToApi(application, personInfo)
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
