package uk.gov.justice.digital.hmpps.approvedpremisesapi.controller.cas2

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.transaction.Transactional
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.cas2.ApplicationsCas2Delegate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas2Application
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SortDirection
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.UpdateCas2Application
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2ApplicationSummaryEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.ForbiddenProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.HttpAuthService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.NomisUserService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas2.ApplicationService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas2.OffenderService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas2.ApplicationsTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.PageCriteria
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.extractEntityFromCasResult
import java.net.URI
import java.util.UUID
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas2ApplicationSummary as ModelCas2ApplicationSummary

@Service(
  "uk.gov.justice.digital.hmpps.approvedpremisesapi.controller.cas2" +
    ".ApplicationsController",
)
class ApplicationsController(
  private val httpAuthService: HttpAuthService,
  private val applicationService: ApplicationService,
  private val applicationsTransformer: ApplicationsTransformer,
  private val objectMapper: ObjectMapper,
  private val offenderService: OffenderService,
  private val userService: NomisUserService,
) : ApplicationsCas2Delegate {

  override fun getCas2Applications(
    isSubmitted: Boolean?,
    page: Int?,
    prisonCode: String?,
  ): ResponseEntity<List<ModelCas2ApplicationSummary>> {
    val user = userService.getUserForRequest()

    prisonCode?.let { if (prisonCode != user.activeCaseloadId) throw ForbiddenProblem() }

    val pageCriteria = PageCriteria("createdAt", SortDirection.desc, page)

    val (applications, metadata) = applicationService.getApplications(prisonCode, isSubmitted, user, pageCriteria)

    return ResponseEntity.ok().headers(
      metadata?.toHeaders(),
    ).body(getPersonNamesAndTransformToSummaries(applications))
  }

  override fun getCas2Application(applicationId: UUID): ResponseEntity<Cas2Application> {
    val user = userService.getUserForRequest()
    val application = extractEntityFromCasResult(applicationService.getApplicationForUser(applicationId, user))
    return ResponseEntity.ok(getPersonDetailAndTransform(application))
  }

  @Transactional
  override fun createCas2Application(body: NewApplication): ResponseEntity<Cas2Application> {
    val nomisPrincipal = httpAuthService.getNomisPrincipalOrThrow()
    val user = userService.getUserForRequest()

    val personInfo = offenderService.getFullInfoForPersonOrThrow(body.crn)

    val applicationResult = applicationService.createApplication(
      body.crn,
      user,
      nomisPrincipal.token.tokenValue,
    )

    val application = extractEntityFromCasResult(applicationResult)

    return ResponseEntity
      .created(URI.create("/cas2/applications/${application.id}"))
      .body(applicationsTransformer.transformJpaToApi(application, personInfo))
  }

  @Transactional
  override fun updateCas2Application(
    applicationId: UUID,
    body: UpdateCas2Application,
  ): ResponseEntity<Cas2Application> {
    val user = userService.getUserForRequest()

    val serializedData = objectMapper.writeValueAsString(body.data)

    val applicationResult = applicationService.updateApplication(
      applicationId =
      applicationId,
      data = serializedData,
      user,
    )

    return ResponseEntity.ok(getPersonDetailAndTransform(extractEntityFromCasResult(applicationResult)))
  }

  @Transactional
  override fun abandonCas2Application(applicationId: UUID): ResponseEntity<Unit> {
    val user = userService.getUserForRequest()
    extractEntityFromCasResult(applicationService.abandonApplication(applicationId, user))
    return ResponseEntity.ok(Unit)
  }

  private fun getPersonNamesAndTransformToSummaries(applicationSummaries: List<Cas2ApplicationSummaryEntity>): List<uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas2ApplicationSummary> {
    val crns = applicationSummaries.map { it.crn }

    val personNamesMap = offenderService.getMapOfPersonNamesAndCrns(crns)

    return applicationSummaries.map { application ->
      applicationsTransformer.transformJpaSummaryToSummary(application, personNamesMap[application.crn]!!)
    }
  }

  private fun getPersonDetailAndTransform(
    application: Cas2ApplicationEntity,
  ): Cas2Application {
    val personInfo = offenderService.getFullInfoForPersonOrThrow(application.crn)

    return applicationsTransformer.transformJpaToApi(application, personInfo)
  }
}
