package uk.gov.justice.digital.hmpps.approvedpremisesapi.controller.cas1

import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.cas1.ApplicationsCas1Delegate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApplicationSortField
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApprovedPremisesApplicationStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1ApplicationSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1TimelineEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ReleaseTypeOption
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SortDirection
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonInfoResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.ApplicationService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.LaoStrategy
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.LaoStrategy.CheckUserAccess
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.OffenderService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1LaoStrategy
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.ApplicationsTransformer
import java.util.UUID
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationSummary as DomainApprovedPremisesApplicationSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.ApprovedPremisesApplicationStatus.Companion as DomainApprovedPremisesApplicationStatus

@Service
class Cas1ApplicationsController(
  private val cas1TimelineService: Cas1TimelineService,
  private val applicationService: ApplicationService,
  private val userService: UserService,
  private val offenderService: OffenderService,
  private val applicationsTransformer: ApplicationsTransformer,
) : ApplicationsCas1Delegate {

  override fun getApplicationTimeLine(
    applicationId: UUID,
  ): ResponseEntity<List<Cas1TimelineEvent>> {
    val cas1timelineEvents = cas1TimelineService.getApplicationTimelineEvents(applicationId)
    return ResponseEntity.ok(cas1timelineEvents)
  }

  override fun getAllApplications(
    page: Int?,
    crnOrName: String?,
    sortDirection: SortDirection?,
    status: List<ApprovedPremisesApplicationStatus>?,
    sortBy: ApplicationSortField?,
    apAreaId: UUID?,
    releaseType: ReleaseTypeOption?,
  ): ResponseEntity<List<Cas1ApplicationSummary>> {
    val user = userService.getUserForRequest()
    val statusTransformed = status?.map { DomainApprovedPremisesApplicationStatus.valueOf(it) } ?: emptyList()

    val (applications, metadata) =
      applicationService.getAllApprovedPremisesApplications(
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

  override fun getApplicationsForUser(): ResponseEntity<List<Cas1ApplicationSummary>> {
    val user = userService.getUserForRequest()

    val applications = applicationService.getAllApprovedPremisesApplicationsForUser(user)

    return ResponseEntity.ok(
      getPersonDetailAndTransformToSummary(
        applications = applications,
        laoStrategy = CheckUserAccess(user.deliusUsername),
      ),
    )
  }

  private fun getPersonDetailAndTransformToSummary(
    applications: List<DomainApprovedPremisesApplicationSummary>,
    laoStrategy: LaoStrategy,
  ): List<Cas1ApplicationSummary> {
    val crns = applications.map { it.getCrn() }
    val personInfoResults = offenderService.getPersonInfoResults(crns.toSet(), laoStrategy)

    return applications.map {
      val crn = it.getCrn()
      applicationsTransformer.transformDomainToCas1ApplicationSummary(
        it,
        personInfoResults.firstOrNull { it.crn == crn } ?: PersonInfoResult.Unknown(crn),
      )
    }

    return emptyList()
  }
}
