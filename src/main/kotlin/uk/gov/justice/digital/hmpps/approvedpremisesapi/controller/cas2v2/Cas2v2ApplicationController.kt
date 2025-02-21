package uk.gov.justice.digital.hmpps.approvedpremisesapi.controller.cas2v2

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.transaction.Transactional
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.cas2v2.ApplicationsCas2v2Delegate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Application
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewCas2v2Application
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SortDirection
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.UpdateApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.UpdateCas2v2Application
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas2v2.Cas2v2ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas2v2.Cas2v2ApplicationSummaryEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.BadRequestProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.ConflictProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.ForbiddenProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.ValidatableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas2.OffenderService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas2v2.Cas2v2ApplicationService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas2v2.Cas2v2UserService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas2v2.Cas2v2ApplicationsTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.PageCriteria
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.ensureEntityFromCasResultIsSuccess
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.extractEntityFromCasResult
import java.net.URI
import java.util.UUID
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas2v2ApplicationSummary as ModelCas2v2ApplicationSummary

@Service(
  "Cas2v2ApplicationController",
)
class Cas2v2ApplicationController(
  private val cas2v2ApplicationService: Cas2v2ApplicationService,
  private val cas2v2ApplicationsTransformer: Cas2v2ApplicationsTransformer,
  private val objectMapper: ObjectMapper,
  private val cas2OffenderService: OffenderService,
  private val userService: Cas2v2UserService,
) : ApplicationsCas2v2Delegate {

  override fun applicationsGet(
    isSubmitted: Boolean?,
    page: Int?,
    prisonCode: String?,
  ): ResponseEntity<List<ModelCas2v2ApplicationSummary>> {
    val user = userService.getUserForRequest()

    if (userService.requiresCaseLoadIdCheck()) {
      prisonCode?.let { if (prisonCode != user.activeNomisCaseloadId) throw ForbiddenProblem() }
    }

    val pageCriteria = PageCriteria("createdAt", SortDirection.desc, page)

    val (applications, metadata) = cas2v2ApplicationService.getCas2v2Applications(prisonCode, isSubmitted, user, pageCriteria)

    return ResponseEntity.ok().headers(
      metadata?.toHeaders(),
    ).body(getPersonNamesAndTransformToSummaries(applications))
  }

  override fun applicationsApplicationIdGet(applicationId: UUID): ResponseEntity<Application> {
    val user = userService.getUserForRequest()

    val applicationResult = cas2v2ApplicationService
      .getCas2v2ApplicationForUser(
        applicationId,
        user,
      )

    val application = extractEntityFromCasResult(applicationResult)
    return ResponseEntity.ok(getPersonDetailAndTransform(application))
  }

  @Transactional
  override fun applicationsPost(body: NewCas2v2Application): ResponseEntity<Application> {
    val user = userService.getUserForRequest()

    val personInfo = cas2OffenderService.getFullInfoForPersonOrThrow(body.crn)

    val applicationResult = cas2v2ApplicationService.createCas2v2Application(
      body.crn,
      user,
      body.applicationOrigin,
      body.bailHearingDate,
    )

    val application = when (applicationResult) {
      is ValidatableActionResult.GeneralValidationError -> throw BadRequestProblem(errorDetail = applicationResult.message)
      is ValidatableActionResult.FieldValidationError -> throw BadRequestProblem(invalidParams = applicationResult.validationMessages)
      is ValidatableActionResult.ConflictError -> throw ConflictProblem(id = applicationResult.conflictingEntityId, conflictReason = applicationResult.message)

      is ValidatableActionResult.Success -> applicationResult.entity
    }

    return ResponseEntity
      .created(URI.create("/cas2v2/applications/${application.id}"))
      .body(cas2v2ApplicationsTransformer.transformJpaToApi(application, personInfo))
  }

  @Suppress("TooGenericExceptionThrown")
  @Transactional
  override fun applicationsApplicationIdPut(
    applicationId: UUID,
    body: UpdateApplication,
  ): ResponseEntity<Application> {
    val user = userService.getUserForRequest()

    val serializedData = objectMapper.writeValueAsString(body.data)

    val applicationResult = when (body) {
      is UpdateCas2v2Application -> cas2v2ApplicationService.updateCas2v2Application(
        applicationId = applicationId,
        data = serializedData,
        user,
        body.bailHearingDate,
      )

      else -> throw RuntimeException("Unsupported UpdateApplication type: ${body::class.qualifiedName}")
    }

    val entity = extractEntityFromCasResult(applicationResult)
    return ResponseEntity.ok(getPersonDetailAndTransform(entity))
  }

  @Transactional
  override fun applicationsApplicationIdAbandonPut(applicationId: UUID): ResponseEntity<Unit> {
    val user = userService.getUserForRequest()

    val applicationResult = cas2v2ApplicationService.abandonCas2v2Application(applicationId, user)
    ensureEntityFromCasResultIsSuccess(applicationResult)
    return ResponseEntity.ok(Unit)
  }

  private fun getPersonNamesAndTransformToSummaries(
    applicationSummaries: List<Cas2v2ApplicationSummaryEntity>,
  ): List<ModelCas2v2ApplicationSummary> {
    val crns = applicationSummaries.map { it.crn }

    val personNamesMap = cas2OffenderService.getMapOfPersonNamesAndCrns(crns)

    return applicationSummaries.map { application ->
      cas2v2ApplicationsTransformer.transformJpaSummaryToSummary(application, personNamesMap[application.crn]!!)
    }
  }

  private fun getPersonDetailAndTransform(
    application: Cas2v2ApplicationEntity,
  ): Application {
    val personInfo = cas2OffenderService.getFullInfoForPersonOrThrow(application.crn)

    return cas2v2ApplicationsTransformer.transformJpaToApi(application, personInfo)
  }
}
