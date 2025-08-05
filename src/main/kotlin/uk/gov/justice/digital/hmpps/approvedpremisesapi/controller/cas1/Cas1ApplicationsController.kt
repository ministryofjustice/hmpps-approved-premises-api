package uk.gov.justice.digital.hmpps.approvedpremisesapi.controller.cas1

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestParam
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApplicationSortField
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApprovedPremisesApplicationStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1ApplicationSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1TimelineEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ReleaseTypeOption
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SortDirection
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonInfoResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.LaoStrategy
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.OffenderDetailService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1ApplicationService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1LaoStrategy
import uk.gov.justice.digital.hmpps.approvedpremisesapi.swagger.PaginationHeaders
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.ApplicationsTransformer
import java.util.UUID
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationSummary as DomainApprovedPremisesApplicationSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.ApprovedPremisesApplicationStatus.Companion as DomainApprovedPremisesApplicationStatus

@Cas1Controller
@Tag(name = "CAS1 Applications")
class Cas1ApplicationsController(
  private val cas1TimelineService: Cas1TimelineService,
  private val cas1ApplicationService: Cas1ApplicationService,
  private val userService: UserService,
  private val offenderDetailService: OffenderDetailService,
  private val applicationsTransformer: ApplicationsTransformer,
) {

  @Operation(summary = "Returns domain event summary")
  @GetMapping("/applications/{applicationId}/timeline")
  fun getApplicationTimeline(
    @PathVariable applicationId: UUID,
  ): ResponseEntity<List<Cas1TimelineEvent>> {
    val cas1timelineEvents = cas1TimelineService.getApplicationTimelineEvents(applicationId)
    return ResponseEntity.ok(cas1timelineEvents)
  }

  @PaginationHeaders
  @Operation(summary = "Lists all applications that any user has created")
  @GetMapping("/applications/all")
  fun getAllApplications(
    @RequestParam page: Int?,
    @RequestParam crnOrName: String?,
    @RequestParam sortDirection: SortDirection?,
    @RequestParam status: List<ApprovedPremisesApplicationStatus>?,
    @RequestParam sortBy: ApplicationSortField?,
    @RequestParam apAreaId: UUID?,
    @RequestParam releaseType: ReleaseTypeOption?,
  ): ResponseEntity<List<Cas1ApplicationSummary>> {
    val user = userService.getUserForRequest()
    val statusTransformed = status?.map { DomainApprovedPremisesApplicationStatus.valueOf(it) } ?: emptyList()

    val (applications, metadata) =
      cas1ApplicationService.getAllApprovedPremisesApplications(
        page,
        crnOrName,
        sortDirection,
        statusTransformed,
        sortBy,
        apAreaId,
        releaseType?.name,
      )

    return ResponseEntity.ok().headers(
      metadata?.toHeaders(),
    ).body(
      getPersonDetailAndTransformToSummary(
        applications = applications,
        laoStrategy = user.cas1LaoStrategy(),
      ),
    )
  }

  @Operation(summary = "Lists all applications that the user has created")
  @GetMapping("/applications/me")
  fun getMyApplications(): ResponseEntity<List<Cas1ApplicationSummary>> {
    val user = userService.getUserForRequest()

    val (applications, metadata) =
      cas1ApplicationService.getAllApprovedPremisesApplications(
        page = null,
        crnOrName = null,
        sortDirection = SortDirection.asc,
        status = emptyList(),
        sortBy = ApplicationSortField.createdAt,
        apAreaId = null,
        releaseType = null,
        createdByUserId = user.id,
      )

    return ResponseEntity.ok().headers(
      metadata?.toHeaders(),
    ).body(
      getPersonDetailAndTransformToSummary(
        applications = applications,
        laoStrategy = user.cas1LaoStrategy(),
      ),
    )
  }

  private fun getPersonDetailAndTransformToSummary(
    applications: List<DomainApprovedPremisesApplicationSummary>,
    laoStrategy: LaoStrategy,
  ): List<Cas1ApplicationSummary> {
    val crns = applications.map { it.getCrn() }
    val personInfoResults = offenderDetailService.getPersonInfoResults(crns.toSet(), laoStrategy)

    return applications.map {
      val crn = it.getCrn()
      applicationsTransformer.transformDomainToCas1ApplicationSummary(
        it,
        personInfoResults.firstOrNull { it.crn == crn } ?: PersonInfoResult.Unknown(crn),
      )
    }
  }
}
