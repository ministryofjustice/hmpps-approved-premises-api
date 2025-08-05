package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.controller

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.AssessmentSortField
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.AssessmentStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.AssessmentSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SortDirection
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.TemporaryAccommodationAssessmentSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainAssessmentSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonInfoResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.BadRequestProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.AssessmentService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.LaoStrategy
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.OffenderDetailService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas3LaoStrategy
import uk.gov.justice.digital.hmpps.approvedpremisesapi.swagger.PaginationHeaders
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.AssessmentTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.PageCriteria
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.sortByName

@RestController
@RequestMapping(headers = ["X-Service-Name=temporary-accommodation"])
class Cas3AssessmentController(
  private val assessmentService: AssessmentService,
  private val userService: UserService,
  private val assessmentTransformer: AssessmentTransformer,
  private val offenderDetailService: OffenderDetailService,
) {

  @Suppress("LongParameterList")
  @PaginationHeaders
  @GetMapping("/assessments")
  fun getTemporaryAccommodationAssessments(
    @RequestParam(defaultValue = "asc") sortDirection: SortDirection,
    @RequestParam(defaultValue = "arrivalDate") sortBy: AssessmentSortField,
    @RequestParam statuses: List<AssessmentStatus>?,
    @RequestParam crnOrName: String?,
    @RequestParam page: Int?,
    @RequestParam perPage: Int?,
  ): ResponseEntity<List<TemporaryAccommodationAssessmentSummary>> {
    val user = userService.getUserForRequest()
    val domainSummaryStatuses = statuses?.map { assessmentTransformer.transformApiStatusToDomainSummaryState(it) } ?: emptyList()

    val (domainSummaries, metadata) = assessmentService.getAssessmentSummariesForUserCAS3(
      user,
      crnOrName,
      ServiceName.temporaryAccommodation,
      domainSummaryStatuses,
      PageCriteria(sortBy, sortDirection, page, perPage),
    )

    val summaries = when (sortBy) {
      AssessmentSortField.assessmentDueAt -> throw BadRequestProblem(
        errorDetail = "Sorting by due date is not supported for CAS3",
      )

      AssessmentSortField.personName -> transformDomainToApi(domainSummaries, user.cas3LaoStrategy())
        .sortByName(sortDirection)

      else -> transformDomainToApi(domainSummaries, user.cas3LaoStrategy())
    }

    return ResponseEntity.ok()
      .headers(metadata?.toHeaders())
      .body(summaries.map { it as TemporaryAccommodationAssessmentSummary })
  }

  private fun transformDomainToApi(
    summaries: List<DomainAssessmentSummary>,
    laoStrategy: LaoStrategy,
  ): List<AssessmentSummary> {
    val crns = summaries.map { it.crn }
    val personInfoResults = offenderDetailService.getPersonInfoResults(crns.toSet(), laoStrategy)

    return summaries.map {
      val crn = it.crn
      assessmentTransformer.transformDomainToApiSummary(
        it,
        personInfoResults.firstOrNull { it.crn == crn } ?: PersonInfoResult.Unknown(crn),
      )
    }
  }
}
