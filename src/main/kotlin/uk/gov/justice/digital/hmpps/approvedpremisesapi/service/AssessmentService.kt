package uk.gov.justice.digital.hmpps.approvedpremisesapi.service

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1ApplicationTimelinessCategory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.deliuscontext.CaseSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesAssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentReferralHistoryNoteRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentReferralHistorySystemNoteEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.LockableAssessmentRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ReferralHistorySystemNoteType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationAssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserPermission
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonSummaryInfoResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.ValidationErrors
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.CasResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1ApplicationStatusService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1AssessmentDomainEventService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1AssessmentEmailService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1AssessmentService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1TaskDeadlineService
import java.time.Clock
import java.time.OffsetDateTime
import java.util.UUID

@Service
@Suppress("ReturnCount", "CyclomaticComplexMethod")
class AssessmentService(
  private val userService: UserService,
  private val userAccessService: UserAccessService,
  private val assessmentRepository: AssessmentRepository,
  private val assessmentReferralHistoryNoteRepository: AssessmentReferralHistoryNoteRepository,
  private val offenderService: OffenderService,
  private val objectMapper: ObjectMapper,
  private val cas1TaskDeadlineService: Cas1TaskDeadlineService,
  private val cas1AssessmentEmailService: Cas1AssessmentEmailService,
  private val cas1AssessmentDomainEventService: Cas1AssessmentDomainEventService,
  private val cas1ApplicationStatusService: Cas1ApplicationStatusService,
  private val clock: Clock,
  private val lockableAssessmentRepository: LockableAssessmentRepository,
  private val cas1AssessmentService: Cas1AssessmentService,
) {
  fun getAssessmentAndValidate(
    user: UserEntity,
    assessmentId: UUID,
    forTimeline: Boolean = false,
  ): CasResult<AssessmentEntity> {
    val assessment = assessmentRepository.findByIdOrNull(assessmentId)
      ?: return CasResult.NotFound("AssessmentEntity", assessmentId.toString())

    val isAuthorised = userAccessService.userCanViewAssessment(user, assessment) || (forTimeline && userAccessService.userCanViewApplication(user, assessment.application))

    if (!isAuthorised) {
      return CasResult.Unauthorised("Not authorised to view the assessment")
    }

    val offenderDetails = getOffenderDetails(assessment.application.crn, user.cas1LaoStrategy())

    if (offenderDetails == null) {
      return CasResult.Unauthorised()
    }

    return CasResult.Success(assessment)
  }

  fun createTemporaryAccommodationAssessment(
    application: TemporaryAccommodationApplicationEntity,
    summaryData: Any,
  ): TemporaryAccommodationAssessmentEntity {
    val dateTimeNow = OffsetDateTime.now()

    val assessment = assessmentRepository.save(
      TemporaryAccommodationAssessmentEntity(
        id = UUID.randomUUID(),
        application = application,
        data = null,
        document = null,
        allocatedToUser = null,
        allocatedAt = dateTimeNow,
        reallocatedAt = null,
        createdAt = dateTimeNow,
        submittedAt = null,
        decision = null,
        rejectionRationale = null,
        clarificationNotes = mutableListOf(),
        referralHistoryNotes = mutableListOf(),
        completedAt = null,
        summaryData = objectMapper.writeValueAsString(summaryData),
        isWithdrawn = false,
        referralRejectionReason = null,
        referralRejectionReasonDetail = null,
        dueAt = null,
        releaseDate = null,
        accommodationRequiredFromDate = null,
      ),
    )

    assessment.addSystemNote(userService.getUserForRequest(), ReferralHistorySystemNoteType.SUBMITTED)

    return assessment
  }

  fun reallocateAssessment(
    allocatingUser: UserEntity,
    assigneeUser: UserEntity,
    id: UUID,
  ): CasResult<AssessmentEntity> {
    lockableAssessmentRepository.acquirePessimisticLock(id)

    val currentAssessment = assessmentRepository.findByIdOrNull(id)
      ?: return CasResult.NotFound("assessment", id.toString())

    if (currentAssessment.reallocatedAt != null) {
      return CasResult.ConflictError(
        currentAssessment.id,
        "This assessment has already been reallocated",
      )
    }

    return when (currentAssessment) {
      is ApprovedPremisesAssessmentEntity -> reallocateApprovedPremisesAssessment(
        allocatingUser = allocatingUser,
        assigneeUser = assigneeUser,
        currentAssessment = currentAssessment,
      )
      is TemporaryAccommodationAssessmentEntity -> reallocateTemporaryAccommodationAssessment(
        assigneeUser,
        currentAssessment,
      )

      else -> throw RuntimeException("Reallocating an assessment of type '${currentAssessment::class.qualifiedName}' has not been implemented.")
    }
  }

  private fun reallocateApprovedPremisesAssessment(
    allocatingUser: UserEntity,
    assigneeUser: UserEntity,
    currentAssessment: ApprovedPremisesAssessmentEntity,
  ): CasResult<AssessmentEntity> {
    if (currentAssessment.submittedAt != null) {
      return CasResult.GeneralValidationError("A decision has already been taken on this assessment")
    }

    val application = currentAssessment.application
    val requiredQualifications = cas1AssessmentService.getRequiredQualificationsToAssess(application as ApprovedPremisesApplicationEntity)

    if (!canUserAssessPlacement(assigneeUser, currentAssessment)) {
      return CasResult.FieldValidationError(
        ValidationErrors().apply {
          this["$.userId"] = "lacking assess application or assess appealed application permission"
        },
      )
    }

    if (!assigneeUser.hasAllQualifications(requiredQualifications)) {
      return CasResult.FieldValidationError(
        ValidationErrors().apply {
          this["$.userId"] = "lackingQualifications"
        },
      )
    }

    val dateTimeNow = OffsetDateTime.now(clock)
    currentAssessment.reallocatedAt = dateTimeNow

    preUpdateAssessment(currentAssessment)
    assessmentRepository.save(currentAssessment)

    val newAssessment =
      ApprovedPremisesAssessmentEntity(
        id = UUID.randomUUID(),
        application = application,
        data = null,
        document = null,
        allocatedToUser = assigneeUser,
        allocatedAt = dateTimeNow,
        reallocatedAt = null,
        createdAt = dateTimeNow,
        submittedAt = null,
        decision = null,
        rejectionRationale = null,
        clarificationNotes = mutableListOf(),
        referralHistoryNotes = mutableListOf(),
        isWithdrawn = false,
        createdFromAppeal = currentAssessment.createdFromAppeal,
        dueAt = null,
      )

    newAssessment.dueAt = cas1TaskDeadlineService.getDeadline(newAssessment)

    prePersistAssessment(newAssessment)
    assessmentRepository.save(newAssessment)

    cas1AssessmentEmailService.assessmentAllocated(assigneeUser, newAssessment.id, application, newAssessment.dueAt, application.noticeType == Cas1ApplicationTimelinessCategory.emergency)
    val allocatedToUser = currentAssessment.allocatedToUser
    if (allocatedToUser != null) {
      cas1AssessmentEmailService.assessmentDeallocated(allocatedToUser, newAssessment.id, application)
    }
    cas1AssessmentDomainEventService.assessmentAllocated(newAssessment, assigneeUser, allocatingUser)

    return CasResult.Success(newAssessment)
  }

  private fun canUserAssessPlacement(user: UserEntity, assessment: ApprovedPremisesAssessmentEntity): Boolean {
    val assigneeUsersPermissions = (user.roles.map { it.role.permissions }).flatten().distinct()

    return (
      assigneeUsersPermissions.contains(UserPermission.CAS1_ASSESS_APPLICATION) ||
        (assessment.createdFromAppeal && assigneeUsersPermissions.contains(UserPermission.CAS1_ASSESS_APPEALED_APPLICATION))
      )
  }

  @Deprecated("Superseded by Cas3AssessmentService.reallocateAssessment()")
  private fun reallocateTemporaryAccommodationAssessment(
    assigneeUser: UserEntity,
    currentAssessment: TemporaryAccommodationAssessmentEntity,
  ): CasResult<AssessmentEntity> {
    if (!assigneeUser.hasRole(UserRole.CAS3_ASSESSOR)) {
      return CasResult.FieldValidationError(
        ValidationErrors().apply {
          this["$.userId"] = "lackingAssessorRole"
        },
      )
    }

    currentAssessment.allocatedToUser = assigneeUser
    currentAssessment.allocatedAt = OffsetDateTime.now()
    currentAssessment.decision = null

    val savedAssessment = assessmentRepository.save(currentAssessment)
    savedAssessment.addSystemNote(userService.getUserForRequest(), ReferralHistorySystemNoteType.IN_REVIEW)

    return CasResult.Success(savedAssessment)
  }

  fun deallocateAssessment(id: UUID): CasResult<AssessmentEntity> {
    val currentAssessment = assessmentRepository.findByIdOrNull(id)
      ?: return CasResult.NotFound("assessment", id.toString())

    if (currentAssessment !is TemporaryAccommodationAssessmentEntity) {
      throw RuntimeException("Only CAS3 Assessments are currently supported")
    }

    currentAssessment.allocatedToUser = null
    currentAssessment.allocatedAt = null
    currentAssessment.decision = null
    currentAssessment.submittedAt = null

    val savedAssessment = assessmentRepository.save(currentAssessment)
    savedAssessment.addSystemNote(userService.getUserForRequest(), ReferralHistorySystemNoteType.UNALLOCATED)

    return CasResult.Success(savedAssessment)
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

  private fun prePersistAssessment(assessment: AssessmentEntity) {
    if (assessment is ApprovedPremisesAssessmentEntity) {
      cas1ApplicationStatusService.assessmentCreated(assessment)
    }
  }

  private fun preUpdateAssessment(assessment: AssessmentEntity) {
    if (assessment is ApprovedPremisesAssessmentEntity) {
      cas1ApplicationStatusService.assessmentUpdated(assessment)
    }
  }

  private fun getOffenderDetails(offenderCrn: String, laoStrategy: LaoStrategy): CaseSummary? {
    val offenderDetails = offenderService.getPersonSummaryInfoResult(
      offenderCrn,
      laoStrategy,
    ).let { offenderDetailsResult ->
      when (offenderDetailsResult) {
        is PersonSummaryInfoResult.Success.Full -> offenderDetailsResult.summary
        else -> null
      }
    }

    return offenderDetails
  }
}
