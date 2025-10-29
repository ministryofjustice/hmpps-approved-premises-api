package uk.gov.justice.digital.hmpps.approvedpremisesapi.service

import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1ApplicationTimelinessCategory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PlacementDates
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PlacementRequirements
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.deliuscontext.CaseSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesAssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentDecision
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentReferralHistoryNoteRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentReferralHistorySystemNoteEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentReferralHistoryUserNoteEntity
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
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.AuthorisableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.CasResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1ApplicationStatusService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1AssessmentDomainEventService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1AssessmentEmailService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1AssessmentService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1PlacementRequestEmailService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1PlacementRequestService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1PlacementRequirementsService
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
  private val placementRequestService: Cas1PlacementRequestService,
  private val cas1PlacementRequirementsService: Cas1PlacementRequirementsService,
  private val objectMapper: ObjectMapper,
  private val cas1TaskDeadlineService: Cas1TaskDeadlineService,
  private val cas1AssessmentEmailService: Cas1AssessmentEmailService,
  private val cas1AssessmentDomainEventService: Cas1AssessmentDomainEventService,
  private val cas1PlacementRequestEmailService: Cas1PlacementRequestEmailService,
  private val cas1ApplicationStatusService: Cas1ApplicationStatusService,
  private val clock: Clock,
  private val lockableAssessmentRepository: LockableAssessmentRepository,
  private val cas1AssessmentService: Cas1AssessmentService,
) {
  private val log = LoggerFactory.getLogger(this::class.java)

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

  /**
   * This function is now only used by CAS1 for reporting integration tests
   * (see Cas1RequestForPlacementReportTest.acceptLatestAssessmentLegacyBehaviour).
   *
   * The UI is now using `Cas1AssessmentService` via /cas1/ endpoints.
   *
   * Once APS-2577 is complete the related report test can be removed as it's no
   * longer required (via APS-2596), meaning CAS1 specific code can also
   * be removed from this function
   *
   * Note that CAS3 may still be using this function
   */
  @Deprecated("will be removed in the near future, use cas specific version instead")
  @SuppressWarnings("ThrowsCount")
  fun acceptAssessment(
    acceptingUser: UserEntity,
    assessmentId: UUID,
    document: String?,
    placementRequirements: PlacementRequirements?,
    placementDates: PlacementDates?,
    apType: ApType?,
    notes: String?,
    agreeWithShortNoticeReason: Boolean? = null,
    agreeWithShortNoticeReasonComments: String? = null,
    reasonForLateApplication: String? = null,
  ): CasResult<AssessmentEntity> {
    val acceptedAt = OffsetDateTime.now(clock)
    val createPlacementRequest = placementDates != null

    val assessment = when (val validation = validateAssessment(acceptingUser, assessmentId)) {
      is CasResult.Success -> validation.value
      else -> return validation
    }

    if (assessment is ApprovedPremisesAssessmentEntity) {
      val validationErrors = ValidationErrors()
      if (placementRequirements == null) {
        validationErrors["$.requirements"] = "empty"
        return CasResult.FieldValidationError(validationErrors)
      }
      when (val dataValidation = validateCas1AssessmentData(assessment)) {
        is CasResult.Success -> {}
        is CasResult.Error -> return dataValidation
      }

      assessment.agreeWithShortNoticeReason = agreeWithShortNoticeReason
      assessment.agreeWithShortNoticeReasonComments = agreeWithShortNoticeReasonComments
      assessment.reasonForLateApplication = reasonForLateApplication
    }

    assessment.document = document
    assessment.submittedAt = acceptedAt
    assessment.decision = AssessmentDecision.ACCEPTED

    if (assessment is TemporaryAccommodationAssessmentEntity) {
      assessment.completedAt = null
    }

    preUpdateAssessment(assessment)
    val savedAssessment = assessmentRepository.save(assessment)

    if (savedAssessment is TemporaryAccommodationAssessmentEntity) {
      savedAssessment.addSystemNote(userService.getUserForRequest(), ReferralHistorySystemNoteType.READY_TO_PLACE)
    }

    if (assessment is ApprovedPremisesAssessmentEntity) {
      val placementRequirementsResult = cas1PlacementRequirementsService.createPlacementRequirements(assessment, placementRequirements!!)

      if (createPlacementRequest) {
        placementRequestService.createPlacementRequest(
          placementRequirementsResult,
          placementDates,
          notes,
          false,
          null,
        )
      }
    }

    val application = savedAssessment.application

    val offenderDetails =
      when (val offenderDetailsResult = offenderService.getOffenderByCrn(application.crn, acceptingUser.deliusUsername, true)) {
        is AuthorisableActionResult.Success -> offenderDetailsResult.entity
        is AuthorisableActionResult.Unauthorised -> throw RuntimeException("Unable to get Offender Details when creating Application Assessed Domain Event: Unauthorised")
        is AuthorisableActionResult.NotFound -> throw RuntimeException("Unable to get Offender Details when creating Application Assessed Domain Event: Not Found")
      }

    if (application is ApprovedPremisesApplicationEntity) {
      cas1AssessmentDomainEventService.assessmentAccepted(
        application = application,
        assessment = assessment,
        offenderDetails = offenderDetails,
        placementDates = placementDates,
        apType = apType,
        acceptingUser = acceptingUser,
      )
      cas1AssessmentEmailService.assessmentAccepted(application)

      if (createPlacementRequest) {
        cas1PlacementRequestEmailService.placementRequestSubmitted(application)
      }
    }

    return CasResult.Success(savedAssessment)
  }

  fun closeAssessment(
    user: UserEntity,
    assessmentId: UUID,
  ): CasResult<AssessmentEntity> {
    val assessment = when (val assessmentResult = getAssessmentAndValidate(user, assessmentId)) {
      is CasResult.Success -> assessmentResult.value
      else -> return assessmentResult
    }

    if (assessment !is TemporaryAccommodationAssessmentEntity) {
      throw RuntimeException("Only CAS3 is currently supported")
    }

    if (assessment.completedAt != null) {
      log.info("User: ${user.id} attempted to close assessment: $assessmentId. This assessment has already been closed.")
      return CasResult.Success(assessment)
    }

    assessment.completedAt = OffsetDateTime.now()

    val savedAssessment = assessmentRepository.save(assessment)
    savedAssessment.addSystemNote(userService.getUserForRequest(), ReferralHistorySystemNoteType.COMPLETED)

    return CasResult.Success(savedAssessment)
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

  fun addAssessmentReferralHistoryUserNote(
    user: UserEntity,
    assessmentId: UUID,
    text: String,
  ): CasResult<AssessmentReferralHistoryUserNoteEntity> {
    val assessment = when (val assessmentResult = getAssessmentAndValidate(user, assessmentId)) {
      is CasResult.Success -> assessmentResult.value
      is CasResult.Error -> return assessmentResult.reviseType()
    }

    val referralHistoryNoteEntity = assessmentReferralHistoryNoteRepository.save(
      AssessmentReferralHistoryUserNoteEntity(
        id = UUID.randomUUID(),
        assessment = assessment,
        createdAt = OffsetDateTime.now(),
        message = text,
        createdByUser = user,
      ),
    )

    return CasResult.Success(referralHistoryNoteEntity)
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

  private fun validateAssessment(
    user: UserEntity,
    assessmentId: UUID,
  ): CasResult<AssessmentEntity> {
    val assessment = when (val assessmentResult = getAssessmentAndValidate(user, assessmentId)) {
      is CasResult.Success -> assessmentResult.value
      else -> return assessmentResult
    }

    if (assessment is ApprovedPremisesAssessmentEntity) {
      val allocatedToUser = assessment.allocatedToUser
        ?: return CasResult.GeneralValidationError("An assessment must be allocated to a user to be updated")

      if (allocatedToUser.id != user.id) {
        return CasResult.Unauthorised("The assessment can only be updated by the allocated user")
      }

      if (assessment.submittedAt != null) {
        return CasResult.GeneralValidationError("A decision has already been taken on this assessment")
      }
    }

    if (assessment.reallocatedAt != null) {
      return CasResult.GeneralValidationError("The application has been reallocated, this assessment is read only")
    }

    return CasResult.Success(assessment)
  }

  private fun validateCas1AssessmentData(
    assessment: ApprovedPremisesAssessmentEntity,
  ): CasResult<AssessmentEntity> {
    val validationErrors = ValidationErrors()
    if (assessment.data == null) {
      validationErrors["$.data"] = "empty"
    }
    return if (validationErrors.any()) {
      CasResult.FieldValidationError(validationErrors)
    } else {
      CasResult.Success(assessment)
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
