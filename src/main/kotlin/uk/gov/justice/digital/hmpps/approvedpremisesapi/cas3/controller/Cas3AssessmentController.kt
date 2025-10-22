package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.controller

import jakarta.transaction.Transactional
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.AssessmentSortField
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.AssessmentStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SortDirection
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.Cas3AssessmentSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.service.Cas3AssessmentService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.transformer.Cas3AssessmentTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainAssessmentSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonInfoResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.LaoStrategy
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.OffenderDetailService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas3LaoStrategy
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.PageCriteria
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.ensureEntityFromCasResultIsSuccess
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.sortCas3AssessmentsByName
import java.util.UUID

@Cas3Controller
class Cas3AssessmentController(
  private val cas3AssessmentService: Cas3AssessmentService,
  private val userService: UserService,
  private val offenderDetailService: OffenderDetailService,
  private val cas3AssessmentTransformer: Cas3AssessmentTransformer,
) {
  @GetMapping("/assessments")
  @Suppress("LongParameterList")
  fun getAssessments(
    @RequestParam(defaultValue = "asc") sortDirection: SortDirection,
    @RequestParam(defaultValue = "arrivalDate") sortBy: AssessmentSortField,
    @RequestParam statuses: List<AssessmentStatus>?,
    @RequestParam crnOrName: String?,
    @RequestParam page: Int?,
    @RequestParam perPage: Int?,
  ): ResponseEntity<List<Cas3AssessmentSummary>> {
    val user = userService.getUserForRequest()
    val domainSummaryStatuses = statuses?.map { cas3AssessmentTransformer.transformApiStatusToDomainSummaryState(it) } ?: emptyList()

    val (summaries, metadata) = cas3AssessmentService.getAssessmentSummariesForUser(
      user,
      crnOrName,
      domainSummaryStatuses,
      PageCriteria(sortBy, sortDirection, page, perPage),
    )
    val transformSummaries = when (sortBy) {
      AssessmentSortField.personName -> transformDomainToApi(summaries, user.cas3LaoStrategy()).sortCas3AssessmentsByName(
        sortDirection,
      )
      else -> transformDomainToApi(summaries, user.cas3LaoStrategy())
    }

    return ResponseEntity.ok()
      .headers(metadata?.toHeaders())
      .body(transformSummaries)
  }

  @DeleteMapping("/assessments/{assessmentId}/allocations")
  @Transactional
  fun deallocateAssessment(@PathVariable assessmentId: UUID): ResponseEntity<Unit> {
    val user = userService.getUserForRequest()

    ensureEntityFromCasResultIsSuccess(cas3AssessmentService.deallocateAssessment(user, assessmentId))

    return ResponseEntity(Unit, HttpStatus.NO_CONTENT)
  }

  @PostMapping("/assessments/{assessmentId}/reallocateToMe")
  @Transactional
  fun reallocateAssessmentToMe(@PathVariable assessmentId: UUID): ResponseEntity<Unit> {
    val user = userService.getUserForRequest()

    ensureEntityFromCasResultIsSuccess(
      cas3AssessmentService.reallocateAssessmentToMe(user, assessmentId),
    )

    return ResponseEntity(HttpStatus.CREATED)
  }

  private fun transformDomainToApi(
    summaries: List<DomainAssessmentSummary>,
    laoStrategy: LaoStrategy,
  ): List<Cas3AssessmentSummary> {
    val crns = summaries.map { it.crn }
    val personInfoResults = offenderDetailService.getPersonInfoResults(crns.toSet(), laoStrategy)

    return summaries.map {
      val crn = it.crn
      cas3AssessmentTransformer.transformDomainToApiSummary(
        it,
        personInfoResults.firstOrNull { it.crn == crn } ?: PersonInfoResult.Unknown(crn),
      )
    }
  }
}
