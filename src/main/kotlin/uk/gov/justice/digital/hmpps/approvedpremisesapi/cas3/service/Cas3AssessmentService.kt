package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.service

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.UpdateAssessment
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.events.CAS3AssessmentUpdatedField
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentReferralHistoryNoteRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentReferralHistorySystemNoteEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.LockableAssessmentRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ReferralHistorySystemNoteType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationAssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationAssessmentRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.findAssessmentById
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.CasResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserAccessService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserService
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

@Service
class Cas3AssessmentService(
  private val assessmentRepository: AssessmentRepository,
  private val temporaryAccommodationAssessmentRepository: TemporaryAccommodationAssessmentRepository,
  private val userAccessService: UserAccessService,
  private val cas3DomainEventService: Cas3DomainEventService,
  private val cas3DomainEventBuilder: Cas3DomainEventBuilder,
  private val userService: UserService,
  private val assessmentReferralHistoryNoteRepository: AssessmentReferralHistoryNoteRepository,
  private val lockableAssessmentRepository: LockableAssessmentRepository,
) {
  @Suppress("ReturnCount")
  fun updateAssessment(
    user: UserEntity,
    assessmentId: UUID,
    updateAssessment: UpdateAssessment,
  ): CasResult<TemporaryAccommodationAssessmentEntity> {
    val assessment: TemporaryAccommodationAssessmentEntity = (
      assessmentRepository.findAssessmentById(assessmentId)
        ?: return CasResult.NotFound("TemporaryAccommodationAssessmentEntity", assessmentId.toString())
      )

    if (!userAccessService.userCanViewAssessment(user, assessment)) {
      return CasResult.Unauthorised()
    }

    if (updateAssessment.releaseDate != null && updateAssessment.accommodationRequiredFromDate != null) {
      return CasResult.GeneralValidationError("Cannot update both dates")
    }

    updateAssessment.releaseDate?.let { newReleaseDate ->
      val currentAccommodationReqDate = assessment.currentAccommodationRequiredFromDate()

      if (newReleaseDate.isAfter(currentAccommodationReqDate)) {
        return notAfterValidationResult(currentAccommodationReqDate)
      }

      val domainEvent =
        cas3DomainEventBuilder.buildAssessmentUpdatedDomainEvent(
          assessment = assessment,
          listOf(
            CAS3AssessmentUpdatedField(
              fieldName = "releaseDate",
              updatedFrom = assessment.currentReleaseDate().toString(),
              updatedTo = newReleaseDate.toString(),
            ),
          ),
        )
      cas3DomainEventService.saveAssessmentUpdatedEvent(domainEvent)
      assessment.releaseDate = newReleaseDate
    }

    updateAssessment.accommodationRequiredFromDate?.let { newAccommodationRequiredFromDate ->
      val currentReleaseDate = assessment.currentReleaseDate()

      if (newAccommodationRequiredFromDate.isBefore(currentReleaseDate)) {
        return notBeforeValidationResult(currentReleaseDate)
      }

      val domainEvent =
        cas3DomainEventBuilder.buildAssessmentUpdatedDomainEvent(
          assessment = assessment,
          listOf(
            CAS3AssessmentUpdatedField(
              fieldName = "accommodationRequiredFromDate",
              updatedFrom = assessment.currentAccommodationRequiredFromDate().toString(),
              updatedTo = newAccommodationRequiredFromDate.toString(),
            ),
          ),
        )
      cas3DomainEventService.saveAssessmentUpdatedEvent(domainEvent)
      assessment.accommodationRequiredFromDate = newAccommodationRequiredFromDate
    }

    return CasResult.Success(assessmentRepository.save(assessment))
  }

  fun deallocateAssessment(requestUser: UserEntity, assessmentId: UUID): CasResult<Unit> {
    if (!userAccessService.userCanDeallocateTask(requestUser)) {
      return CasResult.Unauthorised()
    }

    val currentAssessment = temporaryAccommodationAssessmentRepository.findByIdOrNull(assessmentId)
      ?: return CasResult.NotFound("assessment", assessmentId.toString())

    currentAssessment.allocatedToUser = null
    currentAssessment.allocatedAt = null
    currentAssessment.decision = null
    currentAssessment.submittedAt = null

    val savedAssessment = assessmentRepository.save(currentAssessment)
    savedAssessment.addSystemNote(userService.getUserForRequest(), ReferralHistorySystemNoteType.UNALLOCATED)

    return CasResult.Success(Unit)
  }

  private fun AssessmentEntity.addSystemNote(user: UserEntity, type: ReferralHistorySystemNoteType) {
    this.referralHistoryNotes += assessmentReferralHistoryNoteRepository.save(
      AssessmentReferralHistorySystemNoteEntity(
        id = UUID.randomUUID(),
        assessment = this,
        createdAt = OffsetDateTime.now(),
        message = "",
        createdByUser = user,
        type = type,
      ),
    )
  }

  fun reallocateAssessmentToMe(requestUser: UserEntity, assessmentId: UUID): CasResult<AssessmentEntity?> {
    if (!userAccessService.userCanReallocateTask(requestUser)) {
      return CasResult.Unauthorised()
    }

    lockableAssessmentRepository.acquirePessimisticLock(assessmentId)

    val currentAssessment = temporaryAccommodationAssessmentRepository.findByIdOrNull(assessmentId)
      ?: return CasResult.NotFound("assessment", assessmentId.toString())

    if (currentAssessment.reallocatedAt != null) {
      return CasResult.ConflictError(
        currentAssessment.id,
        "This assessment has already been reallocated",
      )
    }

    currentAssessment.allocatedToUser = requestUser
    currentAssessment.allocatedAt = OffsetDateTime.now()
    currentAssessment.decision = null

    val savedAssessment = assessmentRepository.save(currentAssessment)
    savedAssessment.addSystemNote(userService.getUserForRequest(), ReferralHistorySystemNoteType.IN_REVIEW)

    return CasResult.Success(savedAssessment)
  }

  private fun notBeforeValidationResult(existingDate: LocalDate) = CasResult.GeneralValidationError<TemporaryAccommodationAssessmentEntity>(
    "Accommodation required from date cannot be before release date: $existingDate",
  )

  private fun notAfterValidationResult(existingDate: LocalDate) = CasResult.GeneralValidationError<TemporaryAccommodationAssessmentEntity>(
    "Release date cannot be after accommodation required from date: $existingDate",
  )
}
