package uk.gov.justice.digital.hmpps.approvedpremisesapi.controller

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.ApplicationsApiDelegate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Application
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Assessment
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Document
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Reallocation
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SubmitApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.UpdateApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.OfflineApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.OffenderDetailSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.prisonsapi.InmateDetail
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.BadRequestProblem
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
  private val userService: UserService
) : ApplicationsApiDelegate {
  override fun applicationsGet(xServiceName: ServiceName?): ResponseEntity<List<Application>> {
    val serviceName = xServiceName ?: ServiceName.approvedPremises

    val deliusPrincipal = httpAuthService.getDeliusPrincipalOrThrow()
    val username = deliusPrincipal.name
    val applications = applicationService.getAllApplicationsForUsername(username, serviceName)

    return ResponseEntity.ok(applications.map { getPersonDetailAndTransform(it) })
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
    val serviceName = xServiceName?.value ?: ServiceName.approvedPremises.value

    val deliusPrincipal = httpAuthService.getDeliusPrincipalOrThrow()
    val username = deliusPrincipal.name

    val (offender, inmate) = getPersonDetail(body.crn)

    val application = when (val applicationResult = applicationService.createApplication(body.crn, username, deliusPrincipal.token.tokenValue, serviceName, body.convictionId, body.deliusEventNumber, body.offenceId, createWithRisks)) {
      is ValidatableActionResult.GeneralValidationError -> throw BadRequestProblem(errorDetail = applicationResult.message)
      is ValidatableActionResult.FieldValidationError -> throw BadRequestProblem(invalidParams = applicationResult.validationMessages)
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

    val applicationResult = applicationService.updateApplication(applicationId, serializedData, username)

    val validationResult = when (applicationResult) {
      is AuthorisableActionResult.NotFound -> throw NotFoundProblem(applicationId, "Application")
      is AuthorisableActionResult.Unauthorised -> throw ForbiddenProblem()
      is AuthorisableActionResult.Success -> applicationResult.entity
    }

    val updatedApplication = when (validationResult) {
      is ValidatableActionResult.GeneralValidationError -> throw BadRequestProblem(errorDetail = validationResult.message)
      is ValidatableActionResult.FieldValidationError -> throw BadRequestProblem(invalidParams = validationResult.validationMessages)
      is ValidatableActionResult.Success -> validationResult.entity
    }

    return ResponseEntity.ok(getPersonDetailAndTransform(updatedApplication))
  }

  override fun applicationsApplicationIdSubmissionPost(
    applicationId: UUID,
    submitApplication: SubmitApplication
  ): ResponseEntity<Unit> {
    val deliusPrincipal = httpAuthService.getDeliusPrincipalOrThrow()
    val username = deliusPrincipal.name

    val document = objectMapper.writeValueAsString(submitApplication.translatedDocument)

    val submitResult = applicationService.submitApplication(applicationId, document, username, deliusPrincipal.token.tokenValue)

    val validationResult = when (submitResult) {
      is AuthorisableActionResult.NotFound -> throw NotFoundProblem(applicationId, "Application")
      is AuthorisableActionResult.Unauthorised -> throw ForbiddenProblem()
      is AuthorisableActionResult.Success -> submitResult.entity
    }

    when (validationResult) {
      is ValidatableActionResult.GeneralValidationError -> throw BadRequestProblem(errorDetail = validationResult.message)
      is ValidatableActionResult.FieldValidationError -> throw BadRequestProblem(invalidParams = validationResult.validationMessages)
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

    if (application !is ApprovedPremisesApplicationEntity) {
      throw BadRequestProblem(errorDetail = "Only CAS1 is currently supported")
    }

    val documents = when (val documentsResult = offenderService.getDocuments(application.crn)) {
      is AuthorisableActionResult.NotFound -> throw NotFoundProblem(application.crn, "Person")
      is AuthorisableActionResult.Unauthorised -> throw ForbiddenProblem()
      is AuthorisableActionResult.Success -> documentsResult.entity
    }

    return ResponseEntity(documentTransformer.transformToApi(documents, application.convictionId), HttpStatus.OK)
  }

  @Transactional
  override fun applicationsApplicationIdAllocationsPost(applicationId: UUID, body: Reallocation): ResponseEntity<Assessment> {
    val user = userService.getUserForRequest()

    val authorisationResult = assessmentService.reallocateAssessment(user, body.userId, applicationId)

    val validationResult = when (authorisationResult) {
      is AuthorisableActionResult.NotFound -> throw NotFoundProblem(applicationId, "Application")
      is AuthorisableActionResult.Unauthorised -> throw ForbiddenProblem()
      is AuthorisableActionResult.Success -> authorisationResult.entity
    }

    val assessment = when (validationResult) {
      is ValidatableActionResult.GeneralValidationError -> throw BadRequestProblem(errorDetail = validationResult.message)
      is ValidatableActionResult.FieldValidationError -> throw BadRequestProblem(invalidParams = validationResult.validationMessages)
      is ValidatableActionResult.Success -> validationResult.entity
    }

    val applicationCrn = assessment.application.crn

    val offenderDetails = when (val offenderDetailsResult = offenderService.getOffenderByCrn(applicationCrn, user.deliusUsername)) {
      is AuthorisableActionResult.Success -> offenderDetailsResult.entity
      else -> throw InternalServerErrorProblem("Could not get Offender Details for CRN: $applicationCrn")
    }

    if (offenderDetails.otherIds.nomsNumber == null) {
      throw InternalServerErrorProblem("No NOMS number for CRN: $applicationCrn")
    }

    val inmateDetails = when (val inmateDetailsResult = offenderService.getInmateDetailByNomsNumber(offenderDetails.otherIds.nomsNumber)) {
      is AuthorisableActionResult.Success -> inmateDetailsResult.entity
      else -> throw InternalServerErrorProblem("Could not get Inmate Details for NOMS: ${offenderDetails.otherIds.nomsNumber}")
    }

    return ResponseEntity(assessmentTransformer.transformJpaToApi(assessment, offenderDetails, inmateDetails), HttpStatus.CREATED)
  }

  private fun getPersonDetail(crn: String): Pair<OffenderDetailSummary, InmateDetail> {
    val deliusPrincipal = httpAuthService.getDeliusPrincipalOrThrow()
    val username = deliusPrincipal.name

    val offenderResult = offenderService.getOffenderByCrn(crn, username)

    if (offenderResult !is AuthorisableActionResult.Success) {
      throw InternalServerErrorProblem("Unable to get Person via crn: $crn")
    }

    if (offenderResult.entity.otherIds.nomsNumber == null) {
      throw InternalServerErrorProblem("No nomsNumber present for CRN")
    }

    val inmateDetailResult = offenderService.getInmateDetailByNomsNumber(offenderResult.entity.otherIds.nomsNumber)

    if (inmateDetailResult !is AuthorisableActionResult.Success) {
      throw InternalServerErrorProblem("Unable to get InmateDetail via crn: $crn")
    }

    return Pair(offenderResult.entity, inmateDetailResult.entity)
  }

  private fun getPersonDetailAndTransform(application: ApplicationEntity): Application {
    val (offender, inmate) = getPersonDetail(application.crn)

    return applicationsTransformer.transformJpaToApi(application, offender, inmate)
  }

  private fun getPersonDetailAndTransform(offlineApplication: OfflineApplicationEntity): Application {
    val (offender, inmate) = getPersonDetail(offlineApplication.crn)

    return applicationsTransformer.transformJpaToApi(offlineApplication, offender, inmate)
  }
}
