package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2v2.controller

import jakarta.transaction.Transactional
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas2v2SubmittedApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas2v2SubmittedApplicationSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SortDirection
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SubmitCas2v2Application
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.Cas2ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.Cas2ApplicationSummaryEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.service.Cas2OffenderService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.service.Cas2UserService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2v2.service.Cas2v2ApplicationService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2v2.service.Cas2v2OffenderSearchResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2v2.service.Cas2v2OffenderService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2v2.transformer.Cas2v2ApplicationsTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2v2.transformer.Cas2v2SubmissionsTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.BadRequestProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.ForbiddenProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.NotFoundProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.PageCriteria
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.ensureEntityFromCasResultIsSuccess
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.extractEntityFromCasResult
import java.util.UUID

@Cas2v2Controller
class Cas2v2SubmissionsController(
  private val cas2v2ApplicationService: Cas2v2ApplicationService,
  private val cas2v2SubmissionsTransformer: Cas2v2SubmissionsTransformer,
  private val cas2v2ApplicationsTransformer: Cas2v2ApplicationsTransformer,
  private val cas2v2OffenderService: Cas2v2OffenderService,
  private val offenderService: Cas2OffenderService,
  private val userService: Cas2UserService,
) {

  @GetMapping("/submissions")
  fun submissionsGet(
    @RequestParam page: Int?,
  ): ResponseEntity<List<Cas2v2SubmittedApplicationSummary>> {
    userService.getCas2UserForRequest()

    val sortDirection = SortDirection.asc
    val sortBy = "submittedAt"

    val (applications, metadata) = cas2v2ApplicationService.getAllSubmittedCas2v2ApplicationsForAssessor(PageCriteria(sortBy, sortDirection, page))

    return ResponseEntity.ok().headers(
      metadata?.toHeaders(),
    ).body(getPersonNamesAndTransformToSummaries(applications))
  }

  @GetMapping("/submissions/{applicationId}")
  fun submissionsApplicationIdGet(
    @PathVariable applicationId: UUID,
  ): ResponseEntity<Cas2v2SubmittedApplication> {
    userService.getCas2UserForRequest()

    val applicationResult = cas2v2ApplicationService.getSubmittedCas2v2ApplicationForAssessor(applicationId)
    val application = extractEntityFromCasResult(applicationResult)

    return ResponseEntity.ok(getPersonDetailAndTransform(application))
  }

  @Transactional
  @PostMapping("/submissions")
  fun submissionsPost(
    @RequestBody submitCas2v2Application: SubmitCas2v2Application,
  ): ResponseEntity<Unit> {
    val user = userService.getCas2UserForRequest()
    val submitResult = cas2v2ApplicationService.submitCas2v2Application(submitCas2v2Application, user)
    ensureEntityFromCasResultIsSuccess(submitResult)

    return ResponseEntity(HttpStatus.OK)
  }

  private fun getPersonNamesAndTransformToSummaries(
    applicationSummaries: List<Cas2ApplicationSummaryEntity>,
  ): List<Cas2v2SubmittedApplicationSummary> {
    val crns = applicationSummaries.map { it.crn }

    val personNamesMap = offenderService.getMapOfPersonNamesAndCrns(crns)

    return applicationSummaries.map { application ->
      cas2v2SubmissionsTransformer.transformJpaSummaryToApiRepresentation(application, personNamesMap[application.crn]!!)
    }
  }

  @SuppressWarnings("ThrowsCount")
  private fun getPersonDetailAndTransform(
    application: Cas2ApplicationEntity,
  ): Cas2v2SubmittedApplication {
    val personInfo = when (val cas2v2OffenderSearchResult = cas2v2OffenderService.getPersonByNomisIdOrCrn(application.crn)) {
      is Cas2v2OffenderSearchResult.NotFound -> throw NotFoundProblem(application.crn, "Offender")
      is Cas2v2OffenderSearchResult.Forbidden -> throw ForbiddenProblem()
      is Cas2v2OffenderSearchResult.Unknown -> throw cas2v2OffenderSearchResult.throwable ?: BadRequestProblem(errorDetail = "Could not retrieve person info for Prison Number: ${application.crn}")
      is Cas2v2OffenderSearchResult.Success.Full -> cas2v2OffenderSearchResult.person
    }

    return cas2v2ApplicationsTransformer.transformJpaAndFullPersonToApiSubmitted(application, personInfo)
  }
}
