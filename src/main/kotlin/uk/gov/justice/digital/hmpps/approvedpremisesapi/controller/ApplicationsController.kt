package uk.gov.justice.digital.hmpps.approvedpremisesapi.controller

import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.ApplicationsApiDelegate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Application
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApplicationSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Assessment
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.AssessmentTask
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Document
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewWithdrawal
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PlacementApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PlacementApplicationTask
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PlacementRequestTask
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SubmitApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SubmitApprovedPremisesApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SubmitTemporaryAccommodationApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.TimelineEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.UpdateApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.UpdateApprovedPremisesApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.UpdateTemporaryAccommodationApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.OfflineApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementRequestEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserQualification
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.OffenderDetailSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.prisonsapi.InmateDetail
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.BadRequestProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.ConflictProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.ForbiddenProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.InternalServerErrorProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.NotFoundProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.AuthorisableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.ValidatableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.ApplicationService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.AssessmentService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.HttpAuthService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.OffenderService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.PlacementApplicationService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.ApplicationsTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.AssessmentTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.DocumentTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.PlacementApplicationTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.TaskTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.extractEntityFromNestedAuthorisableValidatableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.getFullInfoForPersonOrThrow
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.getInfoForPersonOrThrow
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.getInfoForPersonOrThrowInternalServerError
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.getNameFromOffenderDetailSummaryResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.getPersonDetailsForCrn
import java.net.URI
import java.util.UUID
import javax.transaction.Transactional

@Service
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
  private val taskTransformer: TaskTransformer,
) : ApplicationsApiDelegate {
  private val log = LoggerFactory.getLogger(this::class.java)

  override fun applicationsGet(xServiceName: ServiceName?): ResponseEntity<List<ApplicationSummary>> {
    val serviceName = xServiceName ?: ServiceName.approvedPremises

    val user = userService.getUserForRequest()

    val applications = applicationService.getAllApplicationsForUsername(user.deliusUsername, serviceName)

    return ResponseEntity.ok(applications.map { getPersonDetailAndTransformToSummary(it, user) })
  }

  override fun applicationsApplicationIdGet(applicationId: UUID): ResponseEntity<Application> {
    val user = userService.getUserForRequest()

    val application = when (val applicationResult = applicationService.getApplicationForUsername(applicationId, user.deliusUsername)) {
      is AuthorisableActionResult.NotFound -> null
      is AuthorisableActionResult.Unauthorised -> throw ForbiddenProblem()
      is AuthorisableActionResult.Success -> applicationResult.entity
    }

    if (application != null) {
      return ResponseEntity.ok(getPersonDetailAndTransform(application, user))
    }

    val offlineApplication = when (val offlineApplicationResult = applicationService.getOfflineApplicationForUsername(applicationId, user.deliusUsername)) {
      is AuthorisableActionResult.Unauthorised -> throw ForbiddenProblem()
      is AuthorisableActionResult.NotFound -> throw NotFoundProblem(applicationId, "Application")
      is AuthorisableActionResult.Success -> offlineApplicationResult.entity
    }

    return ResponseEntity.ok(getPersonDetailAndTransform(offlineApplication, user))
  }

  @Transactional
  override fun applicationsPost(body: NewApplication, xServiceName: ServiceName?, createWithRisks: Boolean?): ResponseEntity<Application> {
    val deliusPrincipal = httpAuthService.getDeliusPrincipalOrThrow()
    val user = userService.getUserForRequest()

    val personInfo = offenderService.getFullInfoForPersonOrThrow(body.crn, user)

    val applicationResult = when (xServiceName ?: ServiceName.approvedPremises) {
      ServiceName.approvedPremises -> applicationService.createApprovedPremisesApplication(personInfo.offenderDetailSummary, user, deliusPrincipal.token.tokenValue, body.convictionId, body.deliusEventNumber, body.offenceId, createWithRisks)
      ServiceName.temporaryAccommodation -> {
        when (val actionResult = applicationService.createTemporaryAccommodationApplication(body.crn, user, deliusPrincipal.token.tokenValue, body.convictionId, body.deliusEventNumber, body.offenceId, createWithRisks)) {
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

    val application = when (applicationResult) {
      is ValidatableActionResult.GeneralValidationError -> throw BadRequestProblem(errorDetail = applicationResult.message)
      is ValidatableActionResult.FieldValidationError -> throw BadRequestProblem(invalidParams = applicationResult.validationMessages)
      is ValidatableActionResult.ConflictError -> throw ConflictProblem(id = applicationResult.conflictingEntityId, conflictReason = applicationResult.message)
      is ValidatableActionResult.Success -> applicationResult.entity
    }

    return ResponseEntity
      .created(URI.create("/applications/${application.id}"))
      .body(applicationsTransformer.transformJpaToApi(application, personInfo))
  }

  @Transactional
  override fun applicationsApplicationIdPut(applicationId: UUID, body: UpdateApplication): ResponseEntity<Application> {
    val user = userService.getUserForRequest()

    val serializedData = objectMapper.writeValueAsString(body.data)

    val applicationResult = when (body) {
      is UpdateApprovedPremisesApplication -> applicationService.updateApprovedPremisesApplication(
        applicationId = applicationId,
        data = serializedData,
        isWomensApplication = body.isWomensApplication,
        isPipeApplication = body.isPipeApplication,
        isEmergencyApplication = body.isEmergencyApplication,
        isEsapApplication = body.isEsapApplication,
        releaseType = body.releaseType?.name,
        arrivalDate = body.arrivalDate,
        isInapplicable = body.isInapplicable,
        username = user.deliusUsername,
      )
      is UpdateTemporaryAccommodationApplication -> applicationService.updateTemporaryAccommodationApplication(
        applicationId = applicationId,
        data = serializedData,
        username = user.deliusUsername,
      )
      else -> throw RuntimeException("Unsupported UpdateApplication type: ${body::class.qualifiedName}")
    }

    val validationResult = when (applicationResult) {
      is AuthorisableActionResult.NotFound -> throw NotFoundProblem(applicationId, "Application")
      is AuthorisableActionResult.Unauthorised -> throw ForbiddenProblem()
      is AuthorisableActionResult.Success -> applicationResult.entity
    }

    val updatedApplication = when (validationResult) {
      is ValidatableActionResult.GeneralValidationError -> throw BadRequestProblem(errorDetail = validationResult.message)
      is ValidatableActionResult.FieldValidationError -> throw BadRequestProblem(invalidParams = validationResult.validationMessages)
      is ValidatableActionResult.ConflictError -> throw ConflictProblem(id = validationResult.conflictingEntityId, conflictReason = validationResult.message)
      is ValidatableActionResult.Success -> validationResult.entity
    }

    return ResponseEntity.ok(getPersonDetailAndTransform(updatedApplication, user))
  }

  override fun applicationsApplicationIdWithdrawalPost(applicationId: UUID, body: NewWithdrawal): ResponseEntity<Unit> {
    val user = userService.getUserForRequest()

    extractEntityFromNestedAuthorisableValidatableActionResult(
      applicationService.withdrawApprovedPremisesApplication(
        applicationId = applicationId,
        user = user,
        withdrawalReason = body.reason.value,
        otherReason = body.otherReason,
      ),
    )

    return ResponseEntity.ok(Unit)
  }

  override fun applicationsApplicationIdTimelineGet(applicationId: UUID, xServiceName: ServiceName): ResponseEntity<List<TimelineEvent>> {
    val user = userService.getUserForRequest()
    if (xServiceName != ServiceName.approvedPremises || !user.hasAnyRole(UserRole.CAS1_ADMIN, UserRole.CAS1_WORKFLOW_MANAGER)) {
      throw ForbiddenProblem()
    }
    val events = applicationService.getApplicationTimeline(applicationId)
    return ResponseEntity(events, HttpStatus.OK)
  }

  override fun applicationsApplicationIdSubmissionPost(
    applicationId: UUID,
    submitApplication: SubmitApplication,
  ): ResponseEntity<Unit> {
    val deliusPrincipal = httpAuthService.getDeliusPrincipalOrThrow()
    val username = deliusPrincipal.name

    val submitResult = when (submitApplication) {
      is SubmitApprovedPremisesApplication -> applicationService.submitApprovedPremisesApplication(applicationId, submitApplication, username, deliusPrincipal.token.tokenValue)
      is SubmitTemporaryAccommodationApplication -> applicationService.submitTemporaryAccommodationApplication(applicationId, submitApplication)
      else -> throw RuntimeException("Unsupported SubmitApplication type: ${submitApplication::class.qualifiedName}")
    }

    val validationResult = when (submitResult) {
      is AuthorisableActionResult.NotFound -> throw NotFoundProblem(applicationId, "Application")
      is AuthorisableActionResult.Unauthorised -> throw ForbiddenProblem()
      is AuthorisableActionResult.Success -> submitResult.entity
    }

    when (validationResult) {
      is ValidatableActionResult.GeneralValidationError -> throw BadRequestProblem(errorDetail = validationResult.message)
      is ValidatableActionResult.FieldValidationError -> throw BadRequestProblem(invalidParams = validationResult.validationMessages)
      is ValidatableActionResult.ConflictError -> throw ConflictProblem(id = validationResult.conflictingEntityId, conflictReason = validationResult.message)
      is ValidatableActionResult.Success -> Unit
    }

    return ResponseEntity(HttpStatus.OK)
  }

  override fun applicationsApplicationIdDocumentsGet(applicationId: UUID): ResponseEntity<List<Document>> {
    val deliusPrincipal = httpAuthService.getDeliusPrincipalOrThrow()
    val username = deliusPrincipal.name

    val application = when (val applicationResult = applicationService.getApplicationForUsername(applicationId, username)) {
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

  override fun applicationsApplicationIdAssessmentGet(applicationId: UUID): ResponseEntity<Assessment> {
    val user = userService.getUserForRequest()

    val assessment = when (val applicationResult = assessmentService.getAssessmentForUserAndApplication(user, applicationId)) {
      is AuthorisableActionResult.NotFound -> throw NotFoundProblem(applicationId, "Assessment")
      is AuthorisableActionResult.Unauthorised -> throw ForbiddenProblem()
      is AuthorisableActionResult.Success -> applicationResult.entity
    }

    val personInfo = offenderService.getFullInfoForPersonOrThrow(assessment.application.crn, user)

    return ResponseEntity.ok(assessmentTransformer.transformJpaToApi(assessment, personInfo))
  }

  override fun applicationsApplicationIdPlacementApplicationsGet(
    applicationId: UUID,
    xServiceName: ServiceName,
  ): ResponseEntity<List<PlacementApplication>> {
    if (xServiceName != ServiceName.approvedPremises) {
      throw ForbiddenProblem()
    }
    val placementApplicationEntities = placementApplicationService.getAllPlacementApplicationEntitiesForApplicationId(applicationId)
    val placementApplications = placementApplicationEntities.map {
      placementApplicationTransformer.transformJpaToApi(it)
    }

    return ResponseEntity.ok(placementApplications)
  }

  private fun getPersonDetail(crn: String, forceFullLaoCheck: Boolean = false): Pair<OffenderDetailSummary, InmateDetail?> {
    val user = userService.getUserForRequest()

    val ignoreLao = if (forceFullLaoCheck) {
      false
    } else {
      user.hasQualification(UserQualification.LAO)
    }

    return getPersonDetailsForCrn(log, crn, user.deliusUsername, offenderService, ignoreLao)
      ?: throw InternalServerErrorProblem("Unable to get Person via crn: $crn")
  }

  private fun getAssessmentTask(assessment: AssessmentEntity, user: UserEntity): AssessmentTask {
    val offenderDetailsResult = offenderService.getOffenderByCrn(assessment.application.crn, user.deliusUsername)

    return taskTransformer.transformAssessmentToTask(
      assessment = assessment,
      personName = getNameFromOffenderDetailSummaryResult(offenderDetailsResult),
    )
  }

  private fun getPlacementRequestTask(placementRequest: PlacementRequestEntity, user: UserEntity): PlacementRequestTask {
    val offenderDetailsResult = offenderService.getOffenderByCrn(placementRequest.application.crn, user.deliusUsername)

    return taskTransformer.transformPlacementRequestToTask(
      placementRequest = placementRequest,
      personName = getNameFromOffenderDetailSummaryResult(offenderDetailsResult),
    )
  }

  private fun getPlacementApplicationTask(placementApplication: PlacementApplicationEntity, user: UserEntity): PlacementApplicationTask {
    val offenderDetailsResult = offenderService.getOffenderByCrn(placementApplication.application.crn, user.deliusUsername)

    return taskTransformer.transformPlacementApplicationToTask(
      placementApplication = placementApplication,
      personName = getNameFromOffenderDetailSummaryResult(offenderDetailsResult),
    )
  }

  private fun getPersonDetailAndTransform(application: ApplicationEntity, user: UserEntity): Application {
    val personInfo = offenderService.getFullInfoForPersonOrThrow(application.crn, user)

    return applicationsTransformer.transformJpaToApi(application, personInfo)
  }

  private fun getPersonDetailAndTransformToSummary(application: uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationSummary, user: UserEntity): ApplicationSummary {
    val personInfo = offenderService.getInfoForPersonOrThrowInternalServerError(application.getCrn(), user)

    return applicationsTransformer.transformDomainToApiSummary(application, personInfo)
  }

  private fun getPersonDetailAndTransform(offlineApplication: OfflineApplicationEntity, user: UserEntity): Application {
    val personInfo = offenderService.getInfoForPersonOrThrow(offlineApplication.crn, user)

    return applicationsTransformer.transformJpaToApi(offlineApplication, personInfo)
  }
}
