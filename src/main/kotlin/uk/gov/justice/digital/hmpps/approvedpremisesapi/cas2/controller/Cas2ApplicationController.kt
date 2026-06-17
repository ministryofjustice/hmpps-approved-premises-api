package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import jakarta.transaction.Transactional
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import tools.jackson.databind.json.JsonMapper
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.PaginationHeaders
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApplicationOrigin
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model.NewCas2Application
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SortDirection
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model.UpdateCas2Application
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model.Cas2Application
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.service.Cas2ApplicationService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.service.Cas2OffenderService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.service.Cas2UserService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.service.Cas2v2OffenderSearchResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.transformer.Cas2ApplicationsTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.jpa.entity.Cas2ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.jpa.entity.Cas2ApplicationSummaryEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.service.Cas2HdcOffenderService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.problem.BadRequestProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.problem.ConflictProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.problem.ForbiddenProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.problem.NotFoundProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.problem.ParamDetails
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.results.ValidatableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.PageCriteria
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.ensureEntityFromCasResultIsSuccess
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.extractEntityFromCasResult
import java.net.URI
import java.util.UUID
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model.Cas2ApplicationSummary as ModelCas2v2ApplicationSummary

@Cas2Controller
class Cas2ApplicationController(
  private val cas2ApplicationService: Cas2ApplicationService,
  private val cas2ApplicationsTransformer: Cas2ApplicationsTransformer,
  private val jsonMapper: JsonMapper,
  private val cas2OffenderService: Cas2OffenderService,
  private val cas2HdcOffenderService: Cas2HdcOffenderService,
  private val userService: Cas2UserService,
) {

  @Operation(summary = "List all applications according to miscellaneous parameters")
  @GetMapping("/applications")
  @PaginationHeaders
  fun applicationsGet(
    @RequestParam isSubmitted: Boolean?,
    @RequestParam page: Int?,
    @Parameter(description = "Use of prisonCode is limited for users with referer roles (ROLE_CAS2_COURT_BAIL_REFERRER or ROLE_CAS2_PRISON_BAIL_REFERRER). See limitByUser documentation")
    @RequestParam prisonCode: String?,
    @RequestParam applicationOrigin: ApplicationOrigin?,
    @Parameter(
      description = """Defaults to true. If true returns applications created by the calling user. If the user does not have a refer role (ROLE_CAS2_COURT_BAIL_REFERRER or ROLE_CAS2_PRISON_BAIL_REFERRER), 
      |and prisonCode is specified, it must match the user's active case load id""",
    )
    @RequestParam limitByUser: Boolean?,
    @RequestParam crnOrNomsNumber: String?,
  ): ResponseEntity<List<ModelCas2v2ApplicationSummary>> {
    val user = userService.getUserForRequest()

    val effectiveLimitByUser = limitByUser ?: true
    if (effectiveLimitByUser && userService.requiresCaseLoadIdCheck()) {
      prisonCode?.let { if (prisonCode != user.activeNomisCaseloadId) throw ForbiddenProblem() }
    }

    val pageCriteria = PageCriteria("createdAt", SortDirection.desc, page)

    val (applications, metadata) = cas2ApplicationService.getCas2Applications(
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

  @Operation(
    description = "Get an application by ID. Access is allowed if one of the following rules is met: " +
      "1. The calling user created the application, " +
      "2. The calling user has ROLE_CAS2_PRISON_BAIL_REFERRER and the application is a submitted prison bail application, " +
      "3. The calling user is a NOMIS user and the application corresponds to an offender in a prison matching the calling user active case load id",
  )
  @GetMapping("/applications/{applicationId}")
  fun applicationsApplicationIdGet(
    @PathVariable applicationId: UUID,
  ): ResponseEntity<Cas2Application> {
    val user = userService.getUserForRequest()

    val applicationResult = cas2ApplicationService
      .getCas2ApplicationForUser(
        applicationId,
        user,
      )

    val application = extractEntityFromCasResult(applicationResult)
    return ResponseEntity.ok(getPersonDetailAndTransform(application))
  }

  @Operation(description = "Create a new Application. This will persist a non-submitted application and the response will include the assigned application ID")
  @Transactional
  @Suppress("ThrowsCount")
  @PostMapping("/applications")
  fun applicationsPost(
    @RequestBody body: NewCas2Application,
  ): ResponseEntity<Cas2Application> {
    val user = userService.getUserForRequest()

    val personInfo = when (val cas2v2OffenderSearchResult = cas2OffenderService.getPersonByNomisIdOrCrn(body.crn)) {
      is Cas2v2OffenderSearchResult.NotFound -> throw NotFoundProblem(body.crn, "Offender")
      is Cas2v2OffenderSearchResult.Forbidden -> throw ForbiddenProblem()
      is Cas2v2OffenderSearchResult.Unknown -> throw cas2v2OffenderSearchResult.throwable ?: BadRequestProblem(errorDetail = "Could not retrieve person info for Prison Number: ${body.crn}")
      is Cas2v2OffenderSearchResult.Success.Full -> cas2v2OffenderSearchResult.person
    }

    val applicationResult = cas2ApplicationService.createCas2Application(
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
      .created(URI.create("/cas2/applications/${application.id}"))
      .body(cas2ApplicationsTransformer.transformJpaAndFullPersonToApi(application, personInfo))
  }

  @Operation(description = "Update a non submitted application. An application can only be updated if it's created by the calling user, unsubmitted and not abandoned")
  @Suppress("TooGenericExceptionThrown")
  @Transactional
  @PutMapping("/applications/{applicationId}")
  fun applicationsApplicationIdPut(
    @PathVariable applicationId: UUID,
    @RequestBody body: UpdateCas2Application,
  ): ResponseEntity<Cas2Application> {
    val user = userService.getUserForRequest()

    val serializedData = jsonMapper.writeValueAsString(body.data)

    val applicationResult = cas2ApplicationService.updateCas2Application(
      applicationId = applicationId,
      data = serializedData,
      user,
      body.bailHearingDate,
      body.cohort,
    )

    val entity = extractEntityFromCasResult(applicationResult)
    return ResponseEntity.ok(getPersonDetailAndTransform(entity))
  }

  @Operation(description = "Abandon an application. Application must be unsubmitted and created by the calling user")
  @Transactional
  @PutMapping("/applications/{applicationId}/abandon")
  fun applicationsApplicationIdAbandonPut(
    @PathVariable applicationId: UUID,
  ): ResponseEntity<Unit> {
    val user = userService.getUserForRequest()

    val applicationResult = cas2ApplicationService.abandonCas2Application(applicationId, user)
    ensureEntityFromCasResultIsSuccess(applicationResult)
    return ResponseEntity.ok(Unit)
  }

  private fun getPersonNamesAndTransformToSummaries(
    applicationSummaries: List<Cas2ApplicationSummaryEntity>,
  ): List<ModelCas2v2ApplicationSummary> {
    val crns = applicationSummaries.map { it.crn }

    val personNamesMap = cas2HdcOffenderService.getMapOfPersonNamesAndCrns(crns)

    return applicationSummaries.map { application ->
      cas2ApplicationsTransformer.transformJpaSummaryToSummary(application, personNamesMap[application.crn]!!)
    }
  }

  @SuppressWarnings("ThrowsCount")
  private fun getPersonDetailAndTransform(
    application: Cas2ApplicationEntity,
  ): Cas2Application {
    val personInfo = when (val cas2v2OffenderSearchResult = cas2OffenderService.getPersonByNomisIdOrCrn(application.crn)) {
      is Cas2v2OffenderSearchResult.NotFound -> throw NotFoundProblem(application.crn, "Offender")
      is Cas2v2OffenderSearchResult.Forbidden -> throw ForbiddenProblem()
      is Cas2v2OffenderSearchResult.Unknown -> throw cas2v2OffenderSearchResult.throwable ?: BadRequestProblem(errorDetail = "Could not retrieve person info for Prison Number: ${application.crn}")
      is Cas2v2OffenderSearchResult.Success.Full -> cas2v2OffenderSearchResult.person
    }

    return cas2ApplicationsTransformer.transformJpaAndFullPersonToApi(application, personInfo)
  }
}
