package uk.gov.justice.digital.hmpps.approvedpremisesapi.controller

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.transaction.Transactional
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
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas3SubmitApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Document
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewAppeal
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewApplicationTimelineNote
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewWithdrawal
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ReleaseTypeOption
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.RequestForPlacement
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SortDirection
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SubmitApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SubmitApprovedPremisesApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SubmitTemporaryAccommodationApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.UpdateApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.UpdateApprovedPremisesApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.UpdateTemporaryAccommodationApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Withdrawables
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.OfflineApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserQualification
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonInfoResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.deliuscontext.APDeliusDocument
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.ConflictProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.ForbiddenProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.NotFoundProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.AuthorisableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.CasResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.ApplicationService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.DocumentService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.HttpAuthService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.LaoStrategy
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.LaoStrategy.CheckUserAccess
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.OffenderService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1AppealService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1ApplicationCreationService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1ApplicationService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1ApplicationTimelineNoteService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1RequestForPlacementService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1WithdrawableService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.WithdrawableEntitiesWithNotes
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1LaoStrategy
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas3.Cas3ApplicationService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.AppealTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.ApplicationTimelineNoteTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.ApplicationsTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.DocumentTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.WithdrawableTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.ensureEntityFromCasResultIsSuccess
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.extractEntityFromCasResult
import java.net.URI
import java.util.UUID
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationSummary as JPAApplicationSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.ApprovedPremisesApplicationStatus.Companion as DomainApprovedPremisesApplicationStatus

@Service
@Suppress("LongParameterList", "TooManyFunctions")
class ApplicationsController(
  private val httpAuthService: HttpAuthService,
  private val applicationService: ApplicationService,
  private val cas1ApplicationService: Cas1ApplicationService,
  private val cas3ApplicationService: Cas3ApplicationService,
  private val applicationsTransformer: ApplicationsTransformer,
  private val objectMapper: ObjectMapper,
  private val offenderService: OffenderService,
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
) : ApplicationsApiDelegate {

  override fun applicationsGet(xServiceName: ServiceName?): ResponseEntity<List<ApplicationSummary>> {
    val serviceName = xServiceName ?: ServiceName.approvedPremises

    val user = userService.getUserForRequest()

    val applications = applicationService.getAllApplicationsForUsername(user, serviceName)

    return ResponseEntity.ok(
      getPersonDetailAndTransformToSummary(
        applications = applications,
        laoStrategy = CheckUserAccess(user.deliusUsername),
      ),
    )
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
      cas1ApplicationService.getAllApprovedPremisesApplications(
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
      getPersonDetailAndTransformToSummary(
        applications = applications,
        laoStrategy = user.cas1LaoStrategy(),
      ),
    )
  }

  override fun applicationsApplicationIdGet(applicationId: UUID): ResponseEntity<Application> {
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

  @Transactional
  override fun applicationsPost(body: NewApplication, xServiceName: ServiceName?, createWithRisks: Boolean?): ResponseEntity<Application> {
    val user = userService.getUserForRequest()

    val personInfo =
      when (val personInfoResult = offenderService.getPersonInfoResult(body.crn, user.deliusUsername, false)) {
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
        createWithRisks,
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

  @Transactional
  override fun applicationsApplicationIdPut(applicationId: UUID, body: UpdateApplication): ResponseEntity<Application> {
    val user = userService.getUserForRequest()

    val serializedData = objectMapper.writeValueAsString(body.data)

    val applicationResult = when (body) {
      is UpdateApprovedPremisesApplication -> cas1ApplicationCreationService.updateApprovedPremisesApplication(
        applicationId = applicationId,
        Cas1ApplicationCreationService.Cas1ApplicationUpdateFields(
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

    val updatedApplication = extractEntityFromCasResult(applicationResult)

    return ResponseEntity.ok(getPersonDetailAndTransform(updatedApplication, user))
  }

  override fun applicationsApplicationIdNotesPost(
    applicationId: UUID,
    body: NewApplicationTimelineNote,
  ): ResponseEntity<ApplicationTimelineNote> {
    val user = userService.getUserForRequest()
    val savedNote = cas1ApplicationTimelineNoteService.saveApplicationTimelineNote(applicationId, body.note, user)

    return ResponseEntity.ok(applicationTimelineNoteTransformer.transformJpaToApi(savedNote))
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

  override fun applicationsApplicationIdRequestsForPlacementGet(applicationId: UUID): ResponseEntity<List<RequestForPlacement>> = ResponseEntity.ok(
    extractEntityFromCasResult(cas1RequestForPlacementService.getRequestsForPlacementByApplication(applicationId, userService.getUserForRequest())),
  )

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
        cas1ApplicationCreationService.submitApprovedPremisesApplication(
          applicationId,
          submitApplication,
          userService.getUserForRequest(),
          apAreaId,
        )
      }

      is SubmitTemporaryAccommodationApplication -> {
        val cas3SubmitApplication = transformToCas3SubmitApplication(submitApplication)
        cas3ApplicationService.submitApplication(applicationId, cas3SubmitApplication)
      }
      else -> throw RuntimeException("Unsupported SubmitApplication type: ${submitApplication::class.qualifiedName}")
    }

    ensureEntityFromCasResultIsSuccess(submitResult)

    return ResponseEntity(HttpStatus.OK)
  }

  override fun applicationsApplicationIdDocumentsGet(applicationId: UUID): ResponseEntity<List<Document>> {
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

  override fun applicationsApplicationIdAppealsAppealIdGet(
    applicationId: UUID,
    appealId: UUID,
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

  override fun applicationsApplicationIdAppealsPost(applicationId: UUID, body: NewAppeal): ResponseEntity<Appeal> {
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

  override fun applicationsApplicationIdWithdrawablesWithNotesGet(
    applicationId: UUID,
    xServiceName: ServiceName,
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
    val personInfo = offenderService.getPersonInfoResult(application.crn, user.deliusUsername, ignoreLaoRestrictions)

    return applicationsTransformer.transformJpaToApi(application, personInfo)
  }

  private fun getPersonDetailAndTransformToSummary(
    applications: List<JPAApplicationSummary>,
    laoStrategy: LaoStrategy,
  ): List<ApplicationSummary> {
    val crns = applications.map { it.getCrn() }
    val personInfoResults = offenderService.getPersonInfoResults(crns.toSet(), laoStrategy)

    return applications.map {
      val crn = it.getCrn()
      applicationsTransformer.transformDomainToApiSummary(
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
    val personInfo = offenderService.getPersonInfoResult(offlineApplication.crn, user.deliusUsername, ignoreLaoRestrictions)

    return applicationsTransformer.transformJpaToApi(offlineApplication, personInfo)
  }

  private fun transformToCas3SubmitApplication(submitApplication: SubmitTemporaryAccommodationApplication) = Cas3SubmitApplication(
    arrivalDate = submitApplication.arrivalDate,
    summaryData = submitApplication.summaryData,
    isRegisteredSexOffender = submitApplication.isRegisteredSexOffender,
    needsAccessibleProperty = submitApplication.needsAccessibleProperty,
    hasHistoryOfArson = submitApplication.hasHistoryOfArson,
    isDutyToReferSubmitted = submitApplication.isDutyToReferSubmitted,
    dutyToReferSubmissionDate = submitApplication.dutyToReferSubmissionDate,
    dutyToReferOutcome = submitApplication.dutyToReferOutcome,
    isApplicationEligible = submitApplication.isApplicationEligible,
    eligibilityReason = submitApplication.eligibilityReason,
    dutyToReferLocalAuthorityAreaName = submitApplication.dutyToReferLocalAuthorityAreaName,
    personReleaseDate = submitApplication.personReleaseDate,
    probationDeliveryUnitId = submitApplication.probationDeliveryUnitId,
    isHistoryOfSexualOffence = submitApplication.isHistoryOfSexualOffence,
    isConcerningSexualBehaviour = submitApplication.isConcerningSexualBehaviour,
    isConcerningArsonBehaviour = submitApplication.isConcerningArsonBehaviour,
    prisonReleaseTypes = submitApplication.prisonReleaseTypes,
    translatedDocument = submitApplication.translatedDocument,
  )
}
