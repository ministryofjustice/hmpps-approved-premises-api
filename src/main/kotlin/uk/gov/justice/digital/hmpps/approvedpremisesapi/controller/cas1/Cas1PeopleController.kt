package uk.gov.justice.digital.hmpps.approvedpremisesapi.controller.cas1

import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.cas1.PeopleCas1Delegate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1PersonalTimeline
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.OfflineApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonInfoResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.NotFoundProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.OffenderDetailService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.SentryService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1ApplicationService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1LaoStrategy
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas1.BoxedApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas1.Cas1ApplicationTimelineModel
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas1.Cas1PersonalTimelineTransformer

@Service
class Cas1PeopleController(
  private val userService: UserService,
  private val offenderDetailService: OffenderDetailService,
  private val cas1ApplicationService: Cas1ApplicationService,
  private val cas1PersonalTimelineTransformer: Cas1PersonalTimelineTransformer,
  private val cas1TimelineService: Cas1TimelineService,
  private val sentryService: SentryService,
) : PeopleCas1Delegate {

  companion object {
    const val TIMELINE_APPLICATION_LIMIT = 50
  }

  override fun getPeopleApplicationsTimeline(crn: String): ResponseEntity<Cas1PersonalTimeline> {
    val user = userService.getUserForRequest()

    val timeline = when (val personInfoResult = offenderDetailService.getPersonInfoResult(crn, user.cas1LaoStrategy())) {
      is PersonInfoResult.NotFound -> throw NotFoundProblem(crn, "Offender")
      is PersonInfoResult.Unknown -> throw personInfoResult.throwable ?: RuntimeException("Could not retrieve person info for CRN: $crn")
      is PersonInfoResult.Success.Full -> buildPersonInfoWithTimeline(personInfoResult, crn)
      is PersonInfoResult.Success.Restricted -> buildPersonInfoWithoutTimeline(personInfoResult)
    }

    return ResponseEntity.ok(timeline)
  }

  private fun buildPersonInfoWithoutTimeline(personInfo: PersonInfoResult.Success.Restricted): Cas1PersonalTimeline = cas1PersonalTimelineTransformer.transformApplicationTimelineModels(personInfo, emptyList())

  @SuppressWarnings("MagicNumber")
  private fun buildPersonInfoWithTimeline(personInfo: PersonInfoResult.Success.Full, crn: String): Cas1PersonalTimeline {
    val regularSubmittedApplications = getRegularSubmittedApplications(crn)
    val offlineApplications = getOfflineApplications(crn)
    val combinedApplications = (regularSubmittedApplications + offlineApplications).take(TIMELINE_APPLICATION_LIMIT)

    if (combinedApplications.size == TIMELINE_APPLICATION_LIMIT) {
      sentryService.captureErrorMessage(
        "Person Timeline results truncated to 50 applications. CRN: $crn. Regular: ${regularSubmittedApplications.size}, Offline: ${offlineApplications.size}. Consider adding paging.",
      )
    }

    val applicationTimelineModels = combinedApplications.map { application ->
      val applicationId = application.map(
        ApprovedPremisesApplicationEntity::id,
        OfflineApplicationEntity::id,
      )
      val cas1timelineEvents = cas1TimelineService.getApplicationTimelineEvents(applicationId)
      Cas1ApplicationTimelineModel(application, cas1timelineEvents)
    }

    return cas1PersonalTimelineTransformer.transformApplicationTimelineModels(personInfo, applicationTimelineModels)
  }

  private fun getRegularSubmittedApplications(crn: String) = cas1ApplicationService
    .getSubmittedApplicationsForCrn(crn, limit = TIMELINE_APPLICATION_LIMIT)
    .map { BoxedApplication.of(it) }

  private fun getOfflineApplications(crn: String) = cas1ApplicationService
    .getOfflineApplicationsForCrn(crn, limit = TIMELINE_APPLICATION_LIMIT)
    .map { BoxedApplication.of(it) }
}
