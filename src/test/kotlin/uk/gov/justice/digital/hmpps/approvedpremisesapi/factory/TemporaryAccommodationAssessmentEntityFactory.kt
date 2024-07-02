package uk.gov.justice.digital.hmpps.approvedpremisesapi.factory

import io.github.bluegroundltd.kfactory.Factory
import io.github.bluegroundltd.kfactory.Yielded
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesAssessmentJsonSchemaEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentClarificationNoteEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentDecision
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentReferralHistoryNoteEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.JsonSchemaEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ReferralRejectionReasonEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationAssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomDateTimeBefore
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

class TemporaryAccommodationAssessmentEntityFactory : Factory<TemporaryAccommodationAssessmentEntity> {
  private var id: Yielded<UUID> = { UUID.randomUUID() }
  private var data: Yielded<String?> = { "{}" }
  private var document: Yielded<String?> = { "{}" }
  private var application: Yielded<ApplicationEntity>? = null
  private var assessmentSchema: Yielded<JsonSchemaEntity> = {
    ApprovedPremisesAssessmentJsonSchemaEntity(
      id = UUID.randomUUID(),
      addedAt = OffsetDateTime.now(),
      schema = "{}",
    )
  }
  private var createdAt: Yielded<OffsetDateTime> = { OffsetDateTime.now().randomDateTimeBefore(30) }
  private var allocatedAt: Yielded<OffsetDateTime> = { OffsetDateTime.now().randomDateTimeBefore(30) }
  private var reallocatedAt: Yielded<OffsetDateTime?> = { null }
  private var submittedAt: Yielded<OffsetDateTime?> = { null }
  private var decision: Yielded<AssessmentDecision?> = { AssessmentDecision.ACCEPTED }
  private var allocatedToUser: Yielded<UserEntity?> = { null }
  private var rejectionRationale: Yielded<String?> = { null }
  private var clarificationNotes: Yielded<MutableList<AssessmentClarificationNoteEntity>> = { mutableListOf() }
  private var referralHistoryNotes: Yielded<MutableList<AssessmentReferralHistoryNoteEntity>> = { mutableListOf() }
  private var completedAt: Yielded<OffsetDateTime?> = { null }
  private var summaryData: Yielded<String> = { "{}" }
  private var dueAt: Yielded<OffsetDateTime?> = { null }
  private var isWithdrawn: Yielded<Boolean> = { false }
  private var referralRejectionReason: Yielded<ReferralRejectionReasonEntity?> = { null }
  private var referralRejectionReasonDetail: Yielded<String?> = { null }
  private var releaseDate: Yielded<LocalDate?> = { LocalDate.now().plusDays(20) }
  private var accommodationRequiredFromDate: Yielded<LocalDate?> = { LocalDate.now().plusDays(20) }

  fun withId(id: UUID) = apply {
    this.id = { id }
  }

  fun withData(data: String?) = apply {
    this.data = { data }
  }

  fun withDocument(document: String) = apply {
    this.document = { document }
  }

  fun withApplication(application: ApplicationEntity) = apply {
    this.application = { application }
  }

  fun withAssessmentSchema(assessmentSchema: JsonSchemaEntity) = apply {
    this.assessmentSchema = { assessmentSchema }
  }

  fun withCreatedAt(createdAt: OffsetDateTime) = apply {
    this.createdAt = { createdAt }
  }

  fun withAllocatedAt(allocatedAt: OffsetDateTime) = apply {
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

  fun withAllocatedToUser(allocatedToUser: UserEntity) = apply {
    this.allocatedToUser = { allocatedToUser }
  }

  fun withoutAllocatedToUser() = apply {
    this.allocatedToUser = { null }
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

  fun withCompletedAt(completedAt: OffsetDateTime) = apply {
    this.completedAt = { completedAt }
  }

  fun withSummaryData(summaryData: String) = apply {
    this.summaryData = { summaryData }
  }

  fun withReferralRejectionReason(referralRejectionReason: ReferralRejectionReasonEntity) = apply {
    this.referralRejectionReason = { referralRejectionReason }
  }

  fun withIsWithdrawn(isWithdrawn: Boolean) = apply {
    this.isWithdrawn = { isWithdrawn }
  }
  fun withReferralRejectionReasonDetail(referralRejectionReasonDetail: String?) = apply {
    this.referralRejectionReasonDetail = { referralRejectionReasonDetail }
  }

  fun withDueAt(dueAt: OffsetDateTime) = apply {
    this.dueAt = { dueAt }
  }
  fun withReleaseDate(releaseDate: LocalDate) = apply {
    this.releaseDate = { releaseDate }
  }
  fun withAccommodationRequiredFromDate(accommodationRequiredFromDate: LocalDate) = apply {
    this.accommodationRequiredFromDate = { accommodationRequiredFromDate }
  }

  override fun produce(): TemporaryAccommodationAssessmentEntity = TemporaryAccommodationAssessmentEntity(
    id = this.id(),
    data = this.data(),
    document = this.document(),
    schemaVersion = this.assessmentSchema(),
    createdAt = this.createdAt(),
    submittedAt = this.submittedAt(),
    decision = this.decision(),
    schemaUpToDate = false,
    application = this.application?.invoke() ?: throw RuntimeException("Must provide an application"),
    allocatedToUser = this.allocatedToUser(),
    allocatedAt = this.allocatedAt(),
    reallocatedAt = this.reallocatedAt(),
    rejectionRationale = this.rejectionRationale(),
    clarificationNotes = this.clarificationNotes(),
    referralHistoryNotes = this.referralHistoryNotes(),
    completedAt = this.completedAt(),
    summaryData = this.summaryData(),
    isWithdrawn = this.isWithdrawn(),
    referralRejectionReason = this.referralRejectionReason(),
    referralRejectionReasonDetail = this.referralRejectionReasonDetail(),
    dueAt = this.dueAt(),
    releaseDate = this.releaseDate(),
    accommodationRequiredFromDate = this.accommodationRequiredFromDate(),
  )
}
