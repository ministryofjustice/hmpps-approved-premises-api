package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2v2.controller

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.transaction.Transactional
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Application
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApplicationOrigin
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewCas2v2Application
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SortDirection
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.UpdateApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.UpdateCas2v2Application
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.service.Cas2OffenderService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2v2.jpa.entity.Cas2v2ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2v2.jpa.entity.Cas2v2ApplicationSummaryEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2v2.service.Cas2v2ApplicationService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2v2.service.Cas2v2OffenderSearchResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2v2.service.Cas2v2OffenderService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2v2.service.Cas2v2UserService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2v2.transformer.Cas2v2ApplicationsTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.BadRequestProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.ConflictProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.ForbiddenProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.NotFoundProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.ParamDetails
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.ValidatableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.swagger.PaginationHeaders
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.PageCriteria
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.ensureEntityFromCasResultIsSuccess
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.extractEntityFromCasResult
import java.net.URI
import java.util.UUID
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas2v2ApplicationSummary as ModelCas2v2ApplicationSummary

@Cas2v2Controller
class Cas2v2ApplicationController(
  private val cas2v2ApplicationService: Cas2v2ApplicationService,
  private val cas2v2ApplicationsTransformer: Cas2v2ApplicationsTransformer,
  private val objectMapper: ObjectMapper,
  private val cas2v2OffenderService: Cas2v2OffenderService,
  private val cas2OffenderService: Cas2OffenderService,
  private val userService: Cas2v2UserService,
) {
  @GetMapping("/applications")
  @PaginationHeaders
  @SuppressWarnings("LongParameterList")
  fun applicationsGet(
    @RequestParam isSubmitted: Boolean?,
    @RequestParam page: Int?,
    @RequestParam prisonCode: String?,
    @RequestParam applicationOrigin: ApplicationOrigin?,
    @RequestParam limitByUser: Boolean?,
    @RequestParam crnOrNomsNumber: String?,
  ): ResponseEntity<List<ModelCas2v2ApplicationSummary>> {
    val user = userService.getUserForRequest()

    val effectiveLimitByUser = limitByUser ?: true
    if (effectiveLimitByUser && userService.requiresCaseLoadIdCheck()) {
      prisonCode?.let { if (prisonCode != user.activeNomisCaseloadId) throw ForbiddenProblem() }
    }

    val pageCriteria = PageCriteria("createdAt", SortDirection.desc, page)

    val (applications, metadata) = cas2v2ApplicationService.getCas2v2Applications(
      prisonCode,
      isSubmitted,
      applicationOrigin,
      effectiveLimitByUser,
      crnOrNomsNumber,
      user,
      pageCriteria,
    )

    return ResponseEntity.ok().headers(
      metadata?.toHeaders(),
    ).body(getPersonNamesAndTransformToSummaries(applications))
  }

  @GetMapping("/applications/{applicationId}")
  fun applicationsApplicationIdGet(
    @PathVariable applicationId: UUID,
  ): ResponseEntity<Application> {
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
  @Suppress("ThrowsCount")
  @PostMapping("/applications")
  fun applicationsPost(
    @RequestBody body: NewCas2v2Application,
  ): ResponseEntity<Application> {
    val user = userService.getUserForRequest()

    val personInfo = when (val cas2v2OffenderSearchResult = cas2v2OffenderService.getPersonByNomisIdOrCrn(body.crn)) {
      is Cas2v2OffenderSearchResult.NotFound -> throw NotFoundProblem(body.crn, "Offender")
      is Cas2v2OffenderSearchResult.Forbidden -> throw ForbiddenProblem()
      is Cas2v2OffenderSearchResult.Unknown -> throw cas2v2OffenderSearchResult.throwable ?: BadRequestProblem(errorDetail = "Could not retrieve person info for Prison Number: ${body.crn}")
      is Cas2v2OffenderSearchResult.Success.Full -> cas2v2OffenderSearchResult.person
    }

    val applicationResult = cas2v2ApplicationService.createCas2v2Application(
      body.crn,
      user,
      body.applicationOrigin,
      body.bailHearingDate,
    )

    val application = when (applicationResult) {
      is ValidatableActionResult.GeneralValidationError -> throw BadRequestProblem(errorDetail = applicationResult.message)
      is ValidatableActionResult.FieldValidationError -> throw BadRequestProblem(invalidParams = applicationResult.validationMessages.mapValues { ParamDetails(it.value) })
      is ValidatableActionResult.ConflictError -> throw ConflictProblem(id = applicationResult.conflictingEntityId, conflictReason = applicationResult.message)

      is ValidatableActionResult.Success -> applicationResult.entity
    }

    return ResponseEntity
      .created(URI.create("/cas2v2/applications/${application.id}"))
      .body(cas2v2ApplicationsTransformer.transformJpaAndFullPersonToApi(application, personInfo))
  }

  @Suppress("TooGenericExceptionThrown")
  @Transactional
  @PutMapping("/applications/{applicationId}")
  fun applicationsApplicationIdPut(
    @PathVariable applicationId: UUID,
    @RequestBody body: UpdateApplication,
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
  @PutMapping("/applications/{applicationId}/abandon")
  fun applicationsApplicationIdAbandonPut(
    @PathVariable applicationId: UUID,
  ): ResponseEntity<Unit> {
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

  @SuppressWarnings("ThrowsCount")
  private fun getPersonDetailAndTransform(
    application: Cas2v2ApplicationEntity,
  ): Application {
    val personInfo = when (val cas2v2OffenderSearchResult = cas2v2OffenderService.getPersonByNomisIdOrCrn(application.crn)) {
      is Cas2v2OffenderSearchResult.NotFound -> throw NotFoundProblem(application.crn, "Offender")
      is Cas2v2OffenderSearchResult.Forbidden -> throw ForbiddenProblem()
      is Cas2v2OffenderSearchResult.Unknown -> throw cas2v2OffenderSearchResult.throwable ?: BadRequestProblem(errorDetail = "Could not retrieve person info for Prison Number: ${application.crn}")
      is Cas2v2OffenderSearchResult.Success.Full -> cas2v2OffenderSearchResult.person
    }

    return cas2v2ApplicationsTransformer.transformJpaAndFullPersonToApi(application, personInfo)
  }
}
