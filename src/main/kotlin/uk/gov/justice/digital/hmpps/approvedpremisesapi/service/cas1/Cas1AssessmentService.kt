package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1ApplicationTimelinessCategory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1AssessmentSortField
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PlacementDates
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PlacementRequirements
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.deliuscontext.CaseSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesAssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesAssessmentRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentClarificationNoteEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentClarificationNoteRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentDecision
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainAssessmentSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainAssessmentSummaryStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.LockableAssessmentRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationPlaceholderRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementRequirementsEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserQualification
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.listeners.AssessmentClarificationNoteListener
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.ApprovedPremisesType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PaginationMetadata
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonSummaryInfoResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.CasResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.LaoStrategy
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.OffenderService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserAccessService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.allocations.UserAllocator
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1LaoStrategy
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.PageCriteria
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.asOffenderDetailSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.getMetadata
import java.time.Clock
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

@Service
class Cas1AssessmentService(
  private val userAccessService: UserAccessService,
  private val assessmentRepository: AssessmentRepository,
  private val assessmentClarificationNoteRepository: AssessmentClarificationNoteRepository,
  private val offenderService: OffenderService,
  private val placementRequestService: Cas1PlacementRequestService,
  private val cas1PlacementRequirementsService: Cas1PlacementRequirementsService,
  private val cas1AssessmentEmailService: Cas1AssessmentEmailService,
  private val cas1AssessmentDomainEventService: Cas1AssessmentDomainEventService,
  private val cas1PlacementRequestEmailService: Cas1PlacementRequestEmailService,
  private val applicationStatusService: Cas1ApplicationStatusService,
  private val assessmentClarificationNoteListener: AssessmentClarificationNoteListener,
  private val approvedPremisesAssessmentRepository: ApprovedPremisesAssessmentRepository,
  private val lockableAssessmentRepository: LockableAssessmentRepository,
  private val taskDeadlineService: Cas1TaskDeadlineService,
  private val userAllocator: UserAllocator,
  private val placementApplicationPlaceholderRepository: PlacementApplicationPlaceholderRepository,
  private val cas1PlacementApplicationService: Cas1PlacementApplicationService,
  private val clock: Clock,
) {

  fun createAssessment(application: ApprovedPremisesApplicationEntity, createdFromAppeal: Boolean = false): ApprovedPremisesAssessmentEntity {
    val dateTimeNow = OffsetDateTime.now(clock)

    var assessment = ApprovedPremisesAssessmentEntity(
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
      isWithdrawn = false,
      createdFromAppeal = createdFromAppeal,
      dueAt = null,
    )

    assessment.dueAt = taskDeadlineService.getDeadline(assessment)

    val allocatedUser = userAllocator.getUserForAssessmentAllocation(assessment)
    assessment.allocatedToUser = allocatedUser

    applicationStatusService.assessmentCreated(assessment)
    assessment = assessmentRepository.save(assessment)

    if (allocatedUser != null) {
      if (createdFromAppeal) {
        cas1AssessmentEmailService.appealedAssessmentAllocated(allocatedUser, assessment.id, application)
      } else {
        cas1AssessmentEmailService.assessmentAllocated(allocatedUser, assessment.id, application, assessment.dueAt, application.noticeType == Cas1ApplicationTimelinessCategory.emergency)
      }
      cas1AssessmentDomainEventService.assessmentAllocated(assessment, allocatedUser, allocatingUser = null)
    }

    return assessment
  }

  @SuppressWarnings("ReturnCount")
  fun updateAssessment(
    updatingUser: UserEntity,
    assessmentId: UUID,
    data: String?,
  ): CasResult<ApprovedPremisesAssessmentEntity> {
    lockableAssessmentRepository.acquirePessimisticLock(assessmentId)

    val assessment = when (val assessmentResult = getAssessmentAndValidate(updatingUser, assessmentId)) {
      is CasResult.Success -> assessmentResult.value
      else -> return assessmentResult
    }

    val allocatedToUser = assessment.allocatedToUser
      ?: return CasResult.GeneralValidationError("An assessment must be allocated to a user to be updated")

    if (allocatedToUser.id != updatingUser.id) {
      return CasResult.Unauthorised("The assessment can only be updated by the allocated user")
    }

    if (assessment.isWithdrawn) {
      return CasResult.GeneralValidationError("The application has been withdrawn.")
    }

    if (assessment.submittedAt != null) {
      return CasResult.GeneralValidationError("A decision has already been taken on this assessment")
    }

    if (assessment.reallocatedAt != null) {
      return CasResult.GeneralValidationError("The assessment has been reallocated, this assessment is read only")
    }

    assessment.data = data

    applicationStatusService.assessmentUpdated(assessment)
    val savedAssessment = approvedPremisesAssessmentRepository.save(assessment)

    return CasResult.Success(savedAssessment)
  }

  @SuppressWarnings("TooGenericExceptionThrown")
  fun getAssessmentAndValidate(
    user: UserEntity,
    assessmentId: UUID,
  ): CasResult<ApprovedPremisesAssessmentEntity> {
    val assessment = approvedPremisesAssessmentRepository.findByIdOrNull(assessmentId)
      ?: return CasResult.NotFound("AssessmentEntity", assessmentId.toString())

    if (!userAccessService.userCanViewAssessment(user, assessment)) {
      return CasResult.Unauthorised("Not authorised to view the assessment")
    }

    val offenderDetails = getOffenderDetails(assessment.application.crn, user.cas1LaoStrategy())

    if (offenderDetails == null) {
      return CasResult.Unauthorised()
    }

    return CasResult.Success(assessment)
  }

  fun findApprovedPremisesAssessmentSummariesNotReallocatedForUser(
    user: UserEntity,
    statuses: List<DomainAssessmentSummaryStatus>,
    pageCriteria: PageCriteria<Cas1AssessmentSortField>,
  ): Pair<List<DomainAssessmentSummary>, PaginationMetadata?> {
    val pageable = pageCriteria.toPageableOrAllPages(
      sortBy = when (pageCriteria.sortBy) {
        Cas1AssessmentSortField.assessmentStatus -> "status"
        Cas1AssessmentSortField.assessmentArrivalDate -> "arrivalDate"
        Cas1AssessmentSortField.assessmentCreatedAt -> "createdAt"
        Cas1AssessmentSortField.assessmentDueAt -> "dueAt"
        Cas1AssessmentSortField.personCrn -> "crn"
        Cas1AssessmentSortField.personName -> "personName"
        Cas1AssessmentSortField.applicationProbationDeliveryUnitName -> error("not supported for CAS1")
      },
    )

    val response = assessmentRepository.findAllApprovedPremisesAssessmentSummariesNotReallocated(
      user.id.toString(),
      statuses.map { it.name },
      pageable,
    )

    return Pair(response.content, getMetadata(response, pageCriteria))
  }

  @SuppressWarnings("ReturnCount")
  fun addAssessmentClarificationNote(
    user: UserEntity,
    assessmentId: UUID,
    text: String,
  ): CasResult<AssessmentClarificationNoteEntity> {
    val assessment = when (val assessmentResult = getAssessmentAndValidate(user, assessmentId)) {
      is CasResult.Success -> assessmentResult.value
      is CasResult.Error -> return assessmentResult.reviseType()
    }

    val clarificationNoteToSave = AssessmentClarificationNoteEntity(
      id = UUID.randomUUID(),
      assessment = assessment,
      createdByUser = user,
      createdAt = OffsetDateTime.now(clock),
      query = text,
      response = null,
      responseReceivedOn = null,
    )
    prePersistClarificationNote(clarificationNoteToSave)
    val clarificationNoteEntity = assessmentClarificationNoteRepository.save(clarificationNoteToSave)

    cas1AssessmentDomainEventService.furtherInformationRequested(assessment, clarificationNoteEntity)

    return CasResult.Success(clarificationNoteEntity)
  }

  @SuppressWarnings("ReturnCount")
  fun updateAssessmentClarificationNote(
    user: UserEntity,
    assessmentId: UUID,
    id: UUID,
    response: String,
    responseReceivedOn: LocalDate,
  ): CasResult<AssessmentClarificationNoteEntity> {
    val assessment = when (val assessmentResult = getAssessmentAndValidate(user, assessmentId)) {
      is CasResult.Success -> assessmentResult.value
      is CasResult.Error -> return assessmentResult.reviseType()
    }

    val clarificationNoteEntity = assessmentClarificationNoteRepository.findByAssessmentIdAndId(assessment.id, id)

    if (clarificationNoteEntity === null) {
      return CasResult.NotFound(entityType = "ClarificationNote", id = id.toString())
    }

    if (clarificationNoteEntity.createdByUser.id !== user.id) {
      return CasResult.Unauthorised()
    }

    if (clarificationNoteEntity.response !== null) {
      return CasResult.GeneralValidationError("A response has already been added to this note")
    }

    clarificationNoteEntity.response = response
    clarificationNoteEntity.responseReceivedOn = responseReceivedOn

    preUpdateClarificationNote(clarificationNoteEntity)
    val savedNote = assessmentClarificationNoteRepository.save(clarificationNoteEntity)

    val assessmentToUpdate = clarificationNoteEntity.assessment as ApprovedPremisesAssessmentEntity
    applicationStatusService.assessmentUpdated(assessment)
    approvedPremisesAssessmentRepository.save(assessmentToUpdate)

    return CasResult.Success(savedNote)
  }

  @Transactional
  @SuppressWarnings("TooGenericExceptionThrown", "ReturnCount")
  fun acceptAssessment(
    acceptingUser: UserEntity,
    assessmentId: UUID,
    document: String?,
    placementRequirements: PlacementRequirements,
    placementDates: PlacementDates?,
    apType: ApType?,
    notes: String?,
    agreeWithShortNoticeReason: Boolean? = null,
    agreeWithShortNoticeReasonComments: String? = null,
    reasonForLateApplication: String? = null,
  ): CasResult<ApprovedPremisesAssessmentEntity> {
    lockableAssessmentRepository.acquirePessimisticLock(assessmentId)

    val acceptedAt = OffsetDateTime.now(clock)
    val includesRequestForPlacement = placementDates != null

    val assessment = when (val validation = validateAssessmentForDecision(acceptingUser, assessmentId)) {
      is CasResult.Success -> validation.value
      else -> return validation
    }

    assessment.agreeWithShortNoticeReason = agreeWithShortNoticeReason
    assessment.agreeWithShortNoticeReasonComments = agreeWithShortNoticeReasonComments
    assessment.reasonForLateApplication = reasonForLateApplication

    assessment.document = document
    assessment.submittedAt = acceptedAt
    assessment.decision = AssessmentDecision.ACCEPTED

    applicationStatusService.assessmentUpdated(assessment)
    val savedAssessment = approvedPremisesAssessmentRepository.save(assessment)

    /*
    Note - these placement requirements are required for all subsequent placement applications linked
    to the application, so they're created here even if a placement request isn't required

    Ideally a placement requirements would be created for each individual placement application instead
     */
    val placementRequirementsResult = cas1PlacementRequirementsService.createPlacementRequirements(assessment, placementRequirements)

    if (includesRequestForPlacement) {
      createRequestForPlacement(
        assessment,
        placementRequirementsResult,
        placementDates,
        notes,
      )
    }

    val application = savedAssessment.application as ApprovedPremisesApplicationEntity

    val caseSummary = getOffenderDetails(application.crn, LaoStrategy.NeverRestricted)
      ?: throw RuntimeException("Offender details not found for CRN: ${application.crn} when creating Application Assessed Domain Event")

    cas1AssessmentDomainEventService.assessmentAccepted(
      application = application,
      assessment = assessment,
      offenderDetails = caseSummary.asOffenderDetailSummary(),
      placementDates = placementDates,
      apType = apType,
      acceptingUser = acceptingUser,
    )
    cas1AssessmentEmailService.assessmentAccepted(application)

    if (includesRequestForPlacement) {
      // it may be worth moving this logic into cas1PlacementApplicationService.createAutomaticPlacementApplication
      // so all emails related to requests for placements are managed in cas1PlacementApplicationService
      // before doing this carefully review which emails are sent for this path and the
      // cas1PlacementApplicationService.recordDecision (accepted) path
      cas1PlacementRequestEmailService.placementRequestSubmitted(application)
    }

    return CasResult.Success(savedAssessment)
  }

  private fun createRequestForPlacement(
    assessment: ApprovedPremisesAssessmentEntity,
    placementRequirements: PlacementRequirementsEntity,
    placementDates: PlacementDates,
    notes: String?,
  ) {
    val application = assessment.application

    val placementApplicationPlaceholder = placementApplicationPlaceholderRepository.findByApplication(application)
      ?: error("Can't find placement application placeholder entry for application ${application.id}")
    placementApplicationPlaceholder.archived = true
    placementApplicationPlaceholderRepository.save(placementApplicationPlaceholder)

    val placementApplicationAutomatic = cas1PlacementApplicationService.createAutomaticPlacementApplication(
      id = placementApplicationPlaceholder.id,
      assessment = assessment,
      authorisedExpectedArrival = placementDates.expectedArrival,
      authorisedDurationDays = placementDates.duration,
    )

    // This logic should probably be moved into
    // cas1PlacementApplicationService.createAutomaticPlacementApplication,
    // (called above)
    //
    // That ensures that the Cas1PlacementApplicationService manages all
    // creations of placement requests from placement applications
    placementRequestService.createPlacementRequest(
      placementRequirements,
      placementDates,
      notes,
      false,
      placementApplicationAutomatic,
    )
  }

  @Transactional
  @SuppressWarnings("TooGenericExceptionThrown")
  fun rejectAssessment(
    rejectingUser: UserEntity,
    assessmentId: UUID,
    document: String?,
    rejectionRationale: String,
    agreeWithShortNoticeReason: Boolean? = null,
    agreeWithShortNoticeReasonComments: String? = null,
    reasonForLateApplication: String? = null,
  ): CasResult<ApprovedPremisesAssessmentEntity> {
    lockableAssessmentRepository.acquirePessimisticLock(assessmentId)

    val assessment = when (val validation = validateAssessmentForDecision(rejectingUser, assessmentId)) {
      is CasResult.Success -> validation.value
      else -> return validation
    }

    assessment.agreeWithShortNoticeReason = agreeWithShortNoticeReason
    assessment.agreeWithShortNoticeReasonComments = agreeWithShortNoticeReasonComments
    assessment.reasonForLateApplication = reasonForLateApplication

    assessment.document = document
    assessment.submittedAt = OffsetDateTime.now(clock)
    assessment.decision = AssessmentDecision.REJECTED
    assessment.rejectionRationale = rejectionRationale

    applicationStatusService.assessmentUpdated(assessment)
    val savedAssessment = approvedPremisesAssessmentRepository.save(assessment)

    val application = savedAssessment.application as ApprovedPremisesApplicationEntity

    val caseSummary = getOffenderDetails(application.crn, LaoStrategy.NeverRestricted)
      ?: throw RuntimeException("Offender details not found for CRN: ${application.crn} when creating Application Assessed Domain Event")

    cas1AssessmentDomainEventService.assessmentRejected(
      application = application,
      assessment = assessment,
      offenderDetails = caseSummary.asOffenderDetailSummary(),
      rejectingUser = rejectingUser,
    )

    cas1AssessmentEmailService.assessmentRejected(application)

    return CasResult.Success(savedAssessment)
  }

  fun updateAssessmentWithdrawn(assessmentId: UUID, withdrawingUser: UserEntity) {
    val assessment = assessmentRepository.findByIdOrNull(assessmentId)
    if (assessment is ApprovedPremisesAssessmentEntity) {
      val isPendingAssessment = assessment.isPendingAssessment()

      assessment.isWithdrawn = true

      applicationStatusService.assessmentUpdated(assessment)
      assessmentRepository.save(assessment)

      cas1AssessmentEmailService.assessmentWithdrawn(
        assessment = assessment,
        isAssessmentPending = isPendingAssessment,
        withdrawingUser = withdrawingUser,
        application = assessment.application as ApprovedPremisesApplicationEntity,
      )
    }
  }

  fun getRequiredQualificationsToAssess(application: ApprovedPremisesApplicationEntity): List<UserQualification> {
    val requiredQualifications = mutableListOf<UserQualification>()

    when (application.apType) {
      ApprovedPremisesType.PIPE -> requiredQualifications += UserQualification.PIPE
      ApprovedPremisesType.ESAP -> requiredQualifications += UserQualification.ESAP
      ApprovedPremisesType.RFAP -> requiredQualifications += UserQualification.RECOVERY_FOCUSED
      ApprovedPremisesType.MHAP_ST_JOSEPHS -> requiredQualifications += UserQualification.MENTAL_HEALTH_SPECIALIST
      ApprovedPremisesType.MHAP_ELLIOTT_HOUSE -> requiredQualifications += UserQualification.MENTAL_HEALTH_SPECIALIST
      else -> {}
    }

    if (application.noticeType == Cas1ApplicationTimelinessCategory.emergency || application.noticeType == Cas1ApplicationTimelinessCategory.shortNotice) {
      requiredQualifications += UserQualification.EMERGENCY
    }

    return requiredQualifications
  }

  private fun prePersistClarificationNote(note: AssessmentClarificationNoteEntity) {
    assessmentClarificationNoteListener.prePersist(note)
  }

  private fun preUpdateClarificationNote(note: AssessmentClarificationNoteEntity) {
    assessmentClarificationNoteListener.preUpdate(note)
  }

  @SuppressWarnings("ReturnCount")
  private fun validateAssessmentForDecision(
    user: UserEntity,
    assessmentId: UUID,
  ): CasResult<ApprovedPremisesAssessmentEntity> {
    val assessment = when (val assessmentResult = getAssessmentAndValidate(user, assessmentId)) {
      is CasResult.Success -> assessmentResult.value
      else -> return assessmentResult
    }

    val allocatedToUser = assessment.allocatedToUser
      ?: return CasResult.GeneralValidationError("An assessment must be allocated to a user to be updated")

    if (allocatedToUser.id != user.id) {
      return CasResult.Unauthorised("The assessment can only be updated by the allocated user")
    }

    if (assessment.submittedAt != null) {
      return CasResult.GeneralValidationError("A decision has already been taken on this assessment")
    }

    if (assessment.reallocatedAt != null) {
      return CasResult.GeneralValidationError("The application has been reallocated, this assessment is read only")
    }

    if (assessment.data == null) {
      return CasResult.FieldValidationError(mapOf("$.data" to "empty"))
    }

    return CasResult.Success(assessment)
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
