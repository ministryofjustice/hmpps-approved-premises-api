package uk.gov.justice.digital.hmpps.approvedpremisesapi.factory

import io.github.bluegroundltd.kfactory.Factory
import io.github.bluegroundltd.kfactory.Yielded
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.factory.TemporaryAccommodationApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentClarificationNoteEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentDecision
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentReferralHistoryNoteEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ReferralRejectionReasonEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationAssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomDateTimeAfter
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomDateTimeBefore
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

class TemporaryAccommodationAssessmentEntityFactory : Factory<TemporaryAccommodationAssessmentEntity> {

  private var id: Yielded<UUID> = { UUID.randomUUID() }
  private var data: Yielded<String?> = { "{}" }
  private var document: Yielded<String?> = { "{}" }
  private var application: Yielded<TemporaryAccommodationApplicationEntity>? = null
  private var createdAt: Yielded<OffsetDateTime> = { OffsetDateTime.now().randomDateTimeBefore(30) }
  private var allocatedAt: Yielded<OffsetDateTime?> = { OffsetDateTime.now().randomDateTimeBefore(30) }
  private var reallocatedAt: Yielded<OffsetDateTime?> = { null }
  private var submittedAt: Yielded<OffsetDateTime?> = { null }
  private var decision: Yielded<AssessmentDecision?> = { AssessmentDecision.ACCEPTED }
  private var allocatedToUser: UserEntity? = null
  private var rejectionRationale: Yielded<String?> = { null }
  private var clarificationNotes: Yielded<MutableList<AssessmentClarificationNoteEntity>> = { mutableListOf() }
  private var referralHistoryNotes: Yielded<MutableList<AssessmentReferralHistoryNoteEntity>> = { mutableListOf() }
  private var isWithdrawn: Yielded<Boolean> = { false }
  private var createdFromAppeal: Yielded<Boolean> = { false }
  private var dueAt: Yielded<OffsetDateTime?> = { OffsetDateTime.now().randomDateTimeAfter(10) }
  private var agreeWithShortNoticeReason: Yielded<Boolean?> = { null }
  private var agreeWithShortNoticeReasonComments: Yielded<String?> = { null }
  private var reasonForLateApplication: Yielded<String?> = { null }
  private var completedAt: Yielded<OffsetDateTime?> = { null }
  private var summaryData: Yielded<String> = { "{}" }
  private var referralRejectionReason: Yielded<ReferralRejectionReasonEntity?> = { null }
  private var referralRejectionReasonDetail: Yielded<String?> = { null }
  private var releaseDate: Yielded<LocalDate?> = { null }
  private var accommodationRequiredFromDate: Yielded<LocalDate?> = { null }

  fun withDefaults() = apply {
    this.application = { TemporaryAccommodationApplicationEntityFactory().withDefaults().produce() }
  }
  fun withId(id: UUID) = apply {
    this.id = { id }
  }

  fun withData(data: String?) = apply {
    this.data = { data }
  }

  fun withDocument(document: String?) = apply {
    this.document = { document }
  }

  fun withApplication(application: TemporaryAccommodationApplicationEntity) = apply {
    this.application = { application }
  }

  fun withYieldedApplication(application: Yielded<TemporaryAccommodationApplicationEntity>) = apply {
    this.application = application
  }

  fun withCreatedAt(createdAt: OffsetDateTime) = apply {
    this.createdAt = { createdAt }
  }

  fun withAllocatedAt(allocatedAt: OffsetDateTime?) = apply {
    this.allocatedAt = { allocatedAt }
  }

  fun withReallocatedAt(reallocatedAt: OffsetDateTime?) = apply {
    this.reallocatedAt = { reallocatedAt }
  }

  fun withSubmittedAt(submittedAt: OffsetDateTime?) = apply {
    this.submittedAt = { submittedAt }
  }

  fun withDecision(decision: AssessmentDecision?) = apply {
    this.decision = { decision }
  }

  fun withAllocatedToUser(allocatedToUser: UserEntity?) = apply {
    this.allocatedToUser = allocatedToUser
  }

  fun withClarificationNotes(clarificationNotes: MutableList<AssessmentClarificationNoteEntity>) = apply {
    this.clarificationNotes = { clarificationNotes }
  }

  fun withReferralHistoryNotes(referralHistoryNotes: MutableList<AssessmentReferralHistoryNoteEntity>) = apply {
    this.referralHistoryNotes = { referralHistoryNotes }
  }

  fun withRejectionRationale(rejectionRationale: String?) = apply {
    this.rejectionRationale = { rejectionRationale }
  }

  fun withIsWithdrawn(isWithdrawn: Boolean) = apply {
    this.isWithdrawn = { isWithdrawn }
  }

  fun withCreatedFromAppeal(createdFromAppeal: Boolean) = apply {
    this.createdFromAppeal = { createdFromAppeal }
  }

  fun withDueAt(dueAt: OffsetDateTime?) = apply {
    this.dueAt = { dueAt }
  }
  fun withAgreeWithShortNoticeReason(agreeWithShortNoticeReason: Boolean?) = apply {
    this.agreeWithShortNoticeReason = { agreeWithShortNoticeReason }
  }

  fun withAgreeWithShortNoticeReasonComments(agreeWithShortNoticeReasonComments: String?) = apply {
    this.agreeWithShortNoticeReasonComments = { agreeWithShortNoticeReasonComments }
  }

  fun withReasonForLateApplication(reasonForLateApplication: String?) = apply {
    this.reasonForLateApplication = { reasonForLateApplication }
  }

  fun withCompletedAt(completedAt: OffsetDateTime?) = apply {
    this.completedAt = { completedAt }
  }

  fun withSummaryData(summaryData: String) = apply {
    this.summaryData = { summaryData }
  }

  fun withReferralRejectionReason(referralRejectionReason: ReferralRejectionReasonEntity?) = apply {
    this.referralRejectionReason = { referralRejectionReason }
  }

  fun withReferralRejectionReasonDetail(referralRejectionReasonDetail: String?) = apply {
    this.referralRejectionReasonDetail = { referralRejectionReasonDetail }
  }

  fun withReleaseDate(releaseDate: LocalDate?) = apply {
    this.releaseDate = { releaseDate }
  }

  fun withAccommodationRequiredFromDate(accommodationRequiredFromDate: LocalDate?) = apply {
    this.accommodationRequiredFromDate = { accommodationRequiredFromDate }
  }

  override fun produce(): TemporaryAccommodationAssessmentEntity = TemporaryAccommodationAssessmentEntity(
    id = this.id(),
    data = this.data(),
    document = this.document(),
    createdAt = this.createdAt(),
    submittedAt = this.submittedAt(),
    decision = this.decision(),
    application = this.application?.invoke() ?: throw RuntimeException("Must provide an application"),
    allocatedToUser = this.allocatedToUser,
    allocatedAt = this.allocatedAt(),
    reallocatedAt = this.reallocatedAt(),
    rejectionRationale = this.rejectionRationale(),
    clarificationNotes = this.clarificationNotes(),
    referralHistoryNotes = this.referralHistoryNotes(),
    isWithdrawn = this.isWithdrawn(),
    dueAt = this.dueAt(),
    completedAt = this.completedAt(),
    summaryData = this.summaryData(),
    referralRejectionReason = this.referralRejectionReason(),
    referralRejectionReasonDetail = this.referralRejectionReasonDetail(),
    releaseDate = this.releaseDate(),
    accommodationRequiredFromDate = this.accommodationRequiredFromDate(),
  )
}
