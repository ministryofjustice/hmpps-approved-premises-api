package uk.gov.justice.digital.hmpps.approvedpremisesapi.controller.cas1

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1ApplicationTimeline
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1PersonDetails
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Person
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.OfflineApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserPermission
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonInfoResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonRisks
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.NotFoundProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.OffenderDetailService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.OffenderRisksService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.SentryService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1ApplicationService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1OffenderService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1UserAccessService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1LaoStrategy
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas1.BoxedApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas1.Cas1ApplicationTimelineModel
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas1.Cas1PersonTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas1.Cas1PersonalTimelineTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.ensureEntityFromCasResultIsSuccess
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.extractEntityFromCasResult

@Cas1Controller
@Tag(name = "CAS1 People")
class Cas1PeopleController(
  private val userService: UserService,
  private val offenderDetailService: OffenderDetailService,
  private val cas1ApplicationService: Cas1ApplicationService,
  private val cas1PersonalTimelineTransformer: Cas1PersonalTimelineTransformer,
  private val cas1TimelineService: Cas1TimelineService,
  private val sentryService: SentryService,
  private val offenderRiskService: OffenderRisksService,
  private val userAccessService: Cas1UserAccessService,
  private val cas1PersonTransformer: Cas1PersonTransformer,
  private val cas1OffenderService: Cas1OffenderService,
) {

  companion object {
    const val TIMELINE_APPLICATION_LIMIT = 50
  }

  @Operation(summary = "Returns a timeline of all applications for a Person.")
  @GetMapping("/people/{crn}/timeline")
  fun getTimelineForCrn(@PathVariable crn: String): ResponseEntity<Cas1PersonalTimeline> {
    val user = userService.getUserForRequest()

    val timeline = when (val personInfoResult = offenderDetailService.getPersonInfoResult(crn, user.cas1LaoStrategy())) {
      is PersonInfoResult.NotFound -> throw NotFoundProblem(crn, "Offender")
      is PersonInfoResult.Unknown -> throw personInfoResult.throwable ?: RuntimeException("Could not retrieve person info for CRN: $crn")
      is PersonInfoResult.Success.Full -> buildPersonInfoWithTimeline(personInfoResult, crn)
      is PersonInfoResult.Success.Restricted -> buildPersonInfoWithoutTimeline(personInfoResult)
    }

    return ResponseEntity.ok(timeline)
  }

  @Operation(summary = "Returns a risk profile for a Person.")
  @GetMapping("/people/{crn}/risk-profile")
  fun getPersonRiskProfile(@PathVariable crn: String): ResponseEntity<PersonRisks> {
    userAccessService.ensureCurrentUserHasPermission(UserPermission.CAS1_AP_RESIDENT_PROFILE)

    val personRisks = offenderRiskService.getPersonRisks(crn)
    return ResponseEntity.ok(personRisks)
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
    }.sortedByDescending { it.application.createdAt() }

    return cas1PersonalTimelineTransformer.transformApplicationTimelineModels(personInfo, applicationTimelineModels)
  }

  private fun getRegularSubmittedApplications(crn: String) = cas1ApplicationService
    .getSubmittedApplicationsForCrn(crn, limit = TIMELINE_APPLICATION_LIMIT)
    .map { BoxedApplication.of(it) }

  private fun getOfflineApplications(crn: String) = cas1ApplicationService
    .getOfflineApplicationsForCrn(crn, limit = TIMELINE_APPLICATION_LIMIT)
    .map { BoxedApplication.of(it) }

  @Operation(summary = "Returns the person details for a CRN.")
  @GetMapping("/people/{crn}/personal-details")
  fun getPersonDetailsForCrn(@PathVariable crn: String): ResponseEntity<Cas1PersonDetails> {
    val user = userService.getUserForRequest()

    val personSummaryInfoResult = cas1OffenderService.getCas1PersonSummaryInfoResult(crn, user.cas1LaoStrategy())
    ensureEntityFromCasResultIsSuccess(personSummaryInfoResult)

    val tier = cas1OffenderService.getRiskTier(crn)
    ensureEntityFromCasResultIsSuccess(tier)

    val cas1PersonDetails = cas1PersonTransformer.transformPersonToCas1PersonDetails(extractEntityFromCasResult(personSummaryInfoResult), extractEntityFromCasResult(tier))

    return ResponseEntity.ok(cas1PersonDetails)
  }
}

data class Cas1PersonalTimeline(
  val person: Person,
  val applications: List<Cas1ApplicationTimeline>,
)
