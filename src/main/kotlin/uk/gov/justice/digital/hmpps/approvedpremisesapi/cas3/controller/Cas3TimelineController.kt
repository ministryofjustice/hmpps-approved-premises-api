package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.controller

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ReferralHistoryNote
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.service.Cas3DomainEventService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationAssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.AssessmentService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.AssessmentTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.extractEntityFromCasResult
import java.util.UUID

@Cas3Controller
class Cas3TimelineController(
  private val userService: UserService,
  private val assessmentService: AssessmentService,
  private val cas3DomainEventService: Cas3DomainEventService,
  private val assessmentTransformer: AssessmentTransformer,
) {

  @GetMapping("/timeline/{assessmentId}")
  fun getTimelineEntries(@PathVariable assessmentId: UUID): ResponseEntity<List<ReferralHistoryNote>> {
    val user = userService.getUserForRequest()
    val assessmentResult = assessmentService.getAssessmentAndValidate(user, assessmentId, forTimeline = true)
    val assessment = extractEntityFromCasResult(assessmentResult) as TemporaryAccommodationAssessmentEntity
    val domainEventNotes = cas3DomainEventService.getAssessmentUpdatedEvents(assessmentId = assessment.id)
    val timelineEntries = assessmentTransformer.getSortedReferralHistoryNotes(
      assessment,
      domainEventNotes,
      includeUserNotes = user.hasAnyRole(UserRole.CAS3_ASSESSOR),
    )

    return ResponseEntity.ok(timelineEntries)
  }
}
