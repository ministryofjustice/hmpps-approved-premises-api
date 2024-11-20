package uk.gov.justice.digital.hmpps.approvedpremisesapi.controller.cas3

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.cas3.TimelineCas3Delegate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ReferralHistoryNote
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationAssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.AssessmentService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas3.DomainEventService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.AssessmentTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.extractEntityFromCasResult

@RestController("CAS3TimelineController")
class TimelineController(
  private val userService: UserService,
  private val assessmentService: AssessmentService,
  private val domainEventService: DomainEventService,
  private val assessmentTransformer: AssessmentTransformer,
) : TimelineCas3Delegate {
  override fun getTimeline(assessmentId: java.util.UUID): ResponseEntity<List<ReferralHistoryNote>> {
    val user = userService.getUserForRequest()

    val assessmentResult = assessmentService.getAssessmentAndValidate(user, assessmentId)
    val assessment = extractEntityFromCasResult(assessmentResult) as TemporaryAccommodationAssessmentEntity
    val domainEventNotes = domainEventService.getAssessmentUpdatedEvents(assessmentId = assessment.id)
    val timelineEntries = assessmentTransformer.getSortedReferralHistoryNotes(assessment, domainEventNotes)

    return ResponseEntity.ok(timelineEntries)
  }
}
