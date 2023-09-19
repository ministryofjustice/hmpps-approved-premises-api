package uk.gov.justice.digital.hmpps.approvedpremisesapi.controller

import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.Cas2ApiDelegate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Application
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApplicationSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.BadRequestProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.ConflictProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.ValidatableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.ApplicationService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.HttpAuthService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.OffenderService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.ApplicationsTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.getFullInfoForPersonOrThrow
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.getInfoForPersonOrThrowInternalServerError
import java.net.URI
import javax.transaction.Transactional

@Service
class Cas2ApplicationsController(
  private val httpAuthService: HttpAuthService,
  private val applicationService: ApplicationService,
  private val applicationsTransformer: ApplicationsTransformer,
  private val offenderService: OffenderService,
  private val userService: UserService,
) : Cas2ApiDelegate {
  private val log = LoggerFactory.getLogger(this::class.java)

  override fun cas2ApplicationsGet(): ResponseEntity<List<ApplicationSummary>> {
    val user = userService.getUserForRequest()

    val applications = applicationService.getAllApplicationsForUsername(
      user
        .deliusUsername,
      ServiceName.cas2,
    )

    return ResponseEntity.ok(applications.map { getPersonDetailAndTransformToSummary(it, user) })
  }

  @Transactional
  override fun cas2ApplicationsPost(body: NewApplication):
    ResponseEntity<Application> {
    val deliusPrincipal = httpAuthService.getDeliusPrincipalOrThrow()
    val user = userService.getUserForRequest()

    val personInfo = offenderService.getFullInfoForPersonOrThrow(body.crn, user)

    val applicationResult = applicationService.createCas2Application(body.crn, user, deliusPrincipal.token.tokenValue)

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

  private fun getPersonDetailAndTransformToSummary(application: uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationSummary, user: UserEntity): ApplicationSummary {
    val personInfo = offenderService.getInfoForPersonOrThrowInternalServerError(application.getCrn(), user)

    return applicationsTransformer.transformDomainToApiSummary(application, personInfo)
  }
}
