package uk.gov.justice.digital.hmpps.approvedpremisesapi.controller.cas2

import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.cas2.SubmissionsCas2Delegate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas2SubmittedApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas2SubmittedApplicationSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2ApplicationSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.ForbiddenProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.NotFoundProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.AuthorisableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.ExternalUserService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.HttpAuthService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas2.ApplicationService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas2.OffenderService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas2.ApplicationsTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.cas2.getFullInfoForPersonOrThrow
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.cas2.getInfoForPersonOrThrowInternalServerError
import java.util.UUID

@Service("Cas2SubmissionsController")
class SubmissionsController(
  private val httpAuthService: HttpAuthService,
  private val applicationService: ApplicationService,
  private val applicationsTransformer: ApplicationsTransformer,
  private val offenderService: OffenderService,
  private val externalUserService: ExternalUserService,
) : SubmissionsCas2Delegate {

  override fun submissionsGet(): ResponseEntity<List<Cas2SubmittedApplicationSummary>> {
    httpAuthService.getCas2AuthenticatedPrincipalOrThrow()
    ensureExternalUserPersisted()
    val applications = applicationService.getAllSubmittedApplicationsForAssessor()
    return ResponseEntity.ok(applications.map { getPersonDetailAndTransformToSummary(it) })
  }

  override fun submissionsApplicationIdGet(applicationId: UUID): ResponseEntity<Cas2SubmittedApplication> {
    httpAuthService.getCas2AuthenticatedPrincipalOrThrow()

    val application = when (
      val applicationResult = applicationService.getSubmittedApplicationForAssessor(applicationId)
    ) {
      is AuthorisableActionResult.NotFound -> null
      is AuthorisableActionResult.Unauthorised -> throw ForbiddenProblem()
      is AuthorisableActionResult.Success -> applicationResult.entity
    }

    if (application != null) {
      return ResponseEntity.ok(getPersonDetailAndTransform(application))
    }
    throw NotFoundProblem(applicationId, "Application")
  }

  private fun ensureExternalUserPersisted() {
    externalUserService.getUserForRequest()
  }

  private fun getPersonDetailAndTransformToSummary(
    application: Cas2ApplicationSummary,
  ):
    Cas2SubmittedApplicationSummary {
    val personInfo = offenderService.getInfoForPersonOrThrowInternalServerError(application.getCrn())

    return applicationsTransformer.transformJpaSummaryToCas2SubmittedSummary(
      application,
      personInfo,
    )
  }

  private fun getPersonDetailAndTransform(
    application: Cas2ApplicationEntity,
  ): Cas2SubmittedApplication {
    val personInfo = offenderService.getFullInfoForPersonOrThrow(application.crn)

    return applicationsTransformer.transformJpaToSubmittedApplication(
      application,
      personInfo,
    )
  }
}
