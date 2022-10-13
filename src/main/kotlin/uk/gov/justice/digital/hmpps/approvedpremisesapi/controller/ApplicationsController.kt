package uk.gov.justice.digital.hmpps.approvedpremisesapi.controller

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.ApplicationsApiDelegate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Application
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.UpdateApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.AuthorisableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.ValidatableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.OffenderDetailSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.prisonsapi.InmateDetail
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.shouldNotBeReached
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.BadRequestProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.ForbiddenProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.InternalServerErrorProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.NotFoundProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.ApplicationService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.HttpAuthService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.OffenderService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.ApplicationsTransformer
import java.net.URI
import java.util.UUID
import javax.transaction.Transactional

@Service
class ApplicationsController(
  private val httpAuthService: HttpAuthService,
  private val applicationService: ApplicationService,
  private val applicationsTransformer: ApplicationsTransformer,
  private val objectMapper: ObjectMapper,
  private val offenderService: OffenderService
) : ApplicationsApiDelegate {
  override fun applicationsGet(): ResponseEntity<List<Application>> {
    val deliusPrincipal = httpAuthService.getDeliusPrincipalOrThrow()
    val username = deliusPrincipal.name
    val applications = applicationService.getAllApplicationsForUsername(username)

    return ResponseEntity.ok(applications.map { getPersonDetailAndTransform(it) })
  }

  override fun applicationsApplicationIdGet(applicationId: UUID): ResponseEntity<Application> {
    val deliusPrincipal = httpAuthService.getDeliusPrincipalOrThrow()
    val username = deliusPrincipal.name

    val application = when (val applicationResult = applicationService.getApplicationForUsername(applicationId, username)) {
      is AuthorisableActionResult.NotFound -> throw NotFoundProblem(applicationId, "Application")
      is AuthorisableActionResult.Unauthorised -> throw ForbiddenProblem()
      is AuthorisableActionResult.Success -> applicationResult.entity
      else -> shouldNotBeReached()
    }

    return ResponseEntity.ok(getPersonDetailAndTransform(application))
  }

  @Transactional
  override fun applicationsPost(body: NewApplication): ResponseEntity<Application> {
    val deliusPrincipal = httpAuthService.getDeliusPrincipalOrThrow()
    val username = deliusPrincipal.name

    val (offender, inmate) = getPersonDetail(body.crn)

    val application = when (val applicationResult = applicationService.createApplication(body.crn, username)) {
      is ValidatableActionResult.GeneralValidationError -> throw BadRequestProblem(errorDetail = applicationResult.message)
      is ValidatableActionResult.FieldValidationError -> throw BadRequestProblem(invalidParams = applicationResult.validationMessages)
      is ValidatableActionResult.Success -> applicationResult.entity
      else -> shouldNotBeReached()
    }

    return ResponseEntity
      .created(URI.create("/applications/${application.id}"))
      .body(applicationsTransformer.transformJpaToApi(application, offender, inmate))
  }

  override fun applicationsApplicationIdPut(applicationId: UUID, body: UpdateApplication): ResponseEntity<Application> {
    val deliusPrincipal = httpAuthService.getDeliusPrincipalOrThrow()
    val username = deliusPrincipal.name

    val serializedData = objectMapper.writeValueAsString(body.data)
    val serializedDocument = objectMapper.writeValueAsString(body.document)

    val applicationResult = applicationService.updateApplication(applicationId, serializedData, serializedDocument, body.submittedAt, username)

    val validationResult = when (applicationResult) {
      is AuthorisableActionResult.NotFound -> throw NotFoundProblem(applicationId, "Application")
      is AuthorisableActionResult.Unauthorised -> throw ForbiddenProblem()
      is AuthorisableActionResult.Success -> applicationResult.entity
      else -> shouldNotBeReached()
    }

    val updatedApplication = when (validationResult) {
      is ValidatableActionResult.GeneralValidationError -> throw BadRequestProblem(errorDetail = validationResult.message)
      is ValidatableActionResult.FieldValidationError -> throw BadRequestProblem(invalidParams = validationResult.validationMessages)
      is ValidatableActionResult.Success -> validationResult.entity
      else -> shouldNotBeReached()
    }

    return ResponseEntity.ok(getPersonDetailAndTransform(updatedApplication))
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
}
