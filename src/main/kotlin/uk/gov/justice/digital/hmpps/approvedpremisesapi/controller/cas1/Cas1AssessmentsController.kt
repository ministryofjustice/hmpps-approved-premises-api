package uk.gov.justice.digital.hmpps.approvedpremisesapi.controller.cas1

import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.cas1.AssessmentsCas1Delegate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1AssessmentSortField
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1AssessmentStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1AssessmentSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SortDirection
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainAssessmentSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonInfoResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.AssessmentService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.LaoStrategy
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.OffenderService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1LaoStrategy
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.AssessmentTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.PageCriteria

@Service
class Cas1AssessmentsController(
  private val assessmentService: AssessmentService,
  private val userService: UserService,
  private val offenderService: OffenderService,
  private val assessmentTransformer: AssessmentTransformer,
) : AssessmentsCas1Delegate {

  override fun getAssessmentsForUser(
    sortDirection: SortDirection?,
    sortBy: Cas1AssessmentSortField?,
    statuses: List<Cas1AssessmentStatus>?,
    crnOrName: String?,
    page: Int?,
    perPage: Int?,
  ): ResponseEntity<List<Cas1AssessmentSummary>> {
    val user = userService.getUserForRequest()
    val resolvedSortDirection = sortDirection ?: SortDirection.asc
    val resolvedSortBy = sortBy ?: Cas1AssessmentSortField.assessmentArrivalDate

    val domainSummaryStatuses = statuses?.map { assessmentTransformer.transformCas1AssessmentStatusToDomainSummaryState(it) } ?: emptyList()
    val (summaries, metadata) = assessmentService.findApprovedPremisesAssessmentSummariesNotReallocatedForUser(
      user,
      domainSummaryStatuses,
      PageCriteria(resolvedSortBy, resolvedSortDirection, page, perPage),
    )

    val transformedSummaries = transformDomainToApi(summaries, user.cas1LaoStrategy())

    return ResponseEntity.ok()
      .headers(metadata?.toHeaders())
      .body(transformedSummaries)
  }

  private fun transformDomainToApi(
    summaries: List<DomainAssessmentSummary>,
    laoStrategy: LaoStrategy,
  ): List<Cas1AssessmentSummary> {
    val crns = summaries.map { it.crn }
    val personInfoResults = offenderService.getPersonInfoResults(crns.toSet(), laoStrategy)

    return summaries.map {
      val crn = it.crn
      assessmentTransformer.transformDomainToCas1AssessmentSummary(
        it,
        personInfoResults.firstOrNull { it.crn == crn } ?: PersonInfoResult.Unknown(crn),
      )
    }
  }
}
