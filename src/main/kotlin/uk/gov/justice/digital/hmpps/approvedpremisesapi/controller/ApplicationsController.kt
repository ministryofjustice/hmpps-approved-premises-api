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
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PlacementApplicationTask
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PlacementRequestTask
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SubmitApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SubmitApprovedPremisesApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SubmitTemporaryAccommodationApplication
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
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.ApplicationsTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.AssessmentTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.DocumentTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.TaskTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.getNameFromOffenderDetailSummaryResult
import java.net.URI
import java.util.UUID
import javax.transaction.Transactional

@Service
class ApplicationsController(
  private val httpAuthService: HttpAuthService,
  private val applicationService: ApplicationService,
  private val applicationsTransformer: ApplicationsTransformer,
  private val assessmentTransformer: AssessmentTransformer,
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

    val deliusPrincipal = httpAuthService.getDeliusPrincipalOrThrow()
    val username = deliusPrincipal.name
    val applications = applicationService.getAllApplicationsForUsername(username, serviceName)

    return ResponseEntity.ok(applications.map(::getPersonDetailAndTransformToSummary))
  }

  override fun applicationsApplicationIdGet(applicationId: UUID): ResponseEntity<Application> {
    val deliusPrincipal = httpAuthService.getDeliusPrincipalOrThrow()
    val username = deliusPrincipal.name

    val application = when (val applicationResult = applicationService.getApplicationForUsername(applicationId, username)) {
      is AuthorisableActionResult.NotFound -> null
      is AuthorisableActionResult.Unauthorised -> throw ForbiddenProblem()
      is AuthorisableActionResult.Success -> applicationResult.entity
    }

    if (application != null) {
      return ResponseEntity.ok(getPersonDetailAndTransform(application))
    }

    val offlineApplication = when (val offlineApplicationResult = applicationService.getOfflineApplicationForUsername(applicationId, username)) {
      is AuthorisableActionResult.Unauthorised -> throw ForbiddenProblem()
      is AuthorisableActionResult.NotFound -> throw NotFoundProblem(applicationId, "Application")
      is AuthorisableActionResult.Success -> offlineApplicationResult.entity
    }

    return ResponseEntity.ok(getPersonDetailAndTransform(offlineApplication))
  }

  @Transactional
  override fun applicationsPost(body: NewApplication, xServiceName: ServiceName?, createWithRisks: Boolean?): ResponseEntity<Application> {
    val deliusPrincipal = httpAuthService.getDeliusPrincipalOrThrow()
    val user = userService.getUserForRequest()

    val (offender, inmate) = getPersonDetail(body.crn)

    val applicationResult = when (xServiceName ?: ServiceName.approvedPremises) {
      ServiceName.approvedPremises -> applicationService.createApprovedPremisesApplication(body.crn, user, deliusPrincipal.token.tokenValue, body.convictionId, body.deliusEventNumber, body.offenceId, createWithRisks)
      ServiceName.cas2 -> applicationService.createCas2Application(body.crn, user, deliusPrincipal.token.tokenValue)
      ServiceName.temporaryAccommodation -> {
        when (val actionResult = applicationService.createTemporaryAccommodationApplication(body.crn, user, deliusPrincipal.token.tokenValue, body.convictionId, body.deliusEventNumber, body.offenceId, createWithRisks)) {
          is AuthorisableActionResult.NotFound -> throw NotFoundProblem(actionResult.id!!, actionResult.entityType!!)
          is AuthorisableActionResult.Unauthorised -> throw ForbiddenProblem()
          is AuthorisableActionResult.Success -> actionResult.entity
        }
      }
    }

    val application = when (applicationResult) {
      is ValidatableActionResult.GeneralValidationError -> throw BadRequestProblem(errorDetail = applicationResult.message)
      is ValidatableActionResult.FieldValidationError -> throw BadRequestProblem(invalidParams = applicationResult.validationMessages)
      is ValidatableActionResult.ConflictError -> throw ConflictProblem(id = applicationResult.conflictingEntityId, conflictReason = applicationResult.message)
      is ValidatableActionResult.Success -> applicationResult.entity
    }

    return ResponseEntity
      .created(URI.create("/applications/${application.id}"))
      .body(applicationsTransformer.transformJpaToApi(application, offender, inmate))
  }

  @Transactional
  override fun applicationsApplicationIdPut(applicationId: UUID, body: UpdateApplication): ResponseEntity<Application> {
    val deliusPrincipal = httpAuthService.getDeliusPrincipalOrThrow()
    val username = deliusPrincipal.name

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
        username = username,
      )
      is UpdateTemporaryAccommodationApplication -> applicationService.updateTemporaryAccommodationApplication(
        applicationId = applicationId,
        data = serializedData,
        username = username,
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

    return ResponseEntity.ok(getPersonDetailAndTransform(updatedApplication))
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

    val (offender, inmate) = getPersonDetail(assessment.application.crn)
    return ResponseEntity.ok(assessmentTransformer.transformJpaToApi(assessment, offender, inmate))
  }

  private fun getPersonDetail(crn: String): Pair<OffenderDetailSummary, InmateDetail> {
    val user = userService.getUserForRequest()

    val offenderResult = offenderService.getOffenderByCrn(crn, user.deliusUsername, user.hasQualification(UserQualification.LAO))

    if (offenderResult !is AuthorisableActionResult.Success) {
      throw InternalServerErrorProblem("Unable to get Person via crn: $crn")
    }

    if (offenderResult.entity.otherIds.nomsNumber == null) {
      throw InternalServerErrorProblem("No nomsNumber present for CRN")
    }

    val inmateDetailResult = offenderService.getInmateDetailByNomsNumber(crn, offenderResult.entity.otherIds.nomsNumber)

    if (inmateDetailResult !is AuthorisableActionResult.Success) {
      throw InternalServerErrorProblem("Unable to get InmateDetail via crn: $crn")
    }

    return Pair(offenderResult.entity, inmateDetailResult.entity)
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

  private fun getPersonDetailAndTransform(application: ApplicationEntity): Application {
    val (offender, inmate) = getPersonDetail(application.crn)

    return applicationsTransformer.transformJpaToApi(application, offender, inmate)
  }

  private fun getPersonDetailAndTransformToSummary(application: uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationSummary): ApplicationSummary {
    val (offender, inmate) = getPersonDetail(application.getCrn())

    return applicationsTransformer.transformDomainToApiSummary(application, offender, inmate)
  }

  private fun getPersonDetailAndTransform(offlineApplication: OfflineApplicationEntity): Application {
    val (offender, inmate) = getPersonDetail(offlineApplication.crn)

    return applicationsTransformer.transformJpaToApi(offlineApplication, offender, inmate)
  }
}
