package uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ReferralHistoryNote
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentReferralHistorySystemNoteEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ReferralHistorySystemNoteType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationAssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.AssessmentDecision as ApiAssessmentDecision
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentDecision as JpaAssessmentDecision

@Component
class AssessmentTransformer(
  private val assessmentReferralHistoryNoteTransformer: AssessmentReferralHistoryNoteTransformer,
  private val userService: UserService,
) {
  fun getSortedReferralHistoryNotes(
    assessment: TemporaryAccommodationAssessmentEntity,
    cas3Events: List<DomainEventEntity>,
    includeUserNotes: Boolean = true,
  ): List<ReferralHistoryNote> {
    val lastReferralRejectedHistoryNote =
      assessment.referralHistoryNotes.filter { it is AssessmentReferralHistorySystemNoteEntity && it.type == ReferralHistorySystemNoteType.REJECTED }
        .maxByOrNull { it.createdAt }

    val notes = assessment.referralHistoryNotes.map {
      if (it.id == lastReferralRejectedHistoryNote?.id) {
        assessmentReferralHistoryNoteTransformer.transformJpaToApi(it, assessment, includeUserNotes)
      } else {
        assessmentReferralHistoryNoteTransformer.transformJpaToApi(it)
      }
    }
      .filter { includeUserNotes || it.type != "user" }
      .toMutableList()

    notes.addAll(
      cas3Events.map {
        val user = userService.findByIdOrNull(it.triggeredByUserId!!)!!
        assessmentReferralHistoryNoteTransformer.transformToReferralHistoryDomainEventNote(it, user)
      },
    )
    return notes.sortedByDescending { it.createdAt }
  }

  fun transformJpaDecisionToApi(decision: JpaAssessmentDecision?) = when (decision) {
    JpaAssessmentDecision.ACCEPTED -> ApiAssessmentDecision.accepted
    JpaAssessmentDecision.REJECTED -> ApiAssessmentDecision.rejected
    null -> null
  }
}
