package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1AssessmentSortField
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PlacementDates
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PlacementRequirements
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
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.listeners.AssessmentClarificationNoteListener
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.listeners.AssessmentListener
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PaginationMetadata
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonSummaryInfoResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.deliuscontext.CaseSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.CasResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.LaoStrategy
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.OffenderService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserAccessService
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
  private val assessmentListener: AssessmentListener,
  private val assessmentClarificationNoteListener: AssessmentClarificationNoteListener,
  private val approvedPremisesAssessmentRepository: ApprovedPremisesAssessmentRepository,
  private val lockableAssessmentRepository: LockableAssessmentRepository,
  private val clock: Clock,
) {

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

    preUpdateAssessment(assessment)
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
    preUpdateAssessment(assessmentToUpdate)
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
    val createPlacementRequest = placementDates != null

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

    preUpdateAssessment(assessment)
    val savedAssessment = approvedPremisesAssessmentRepository.save(assessment)

    /*
    Note - these placement requirements are required for all subsequent placement applications linked
    to the application, so they're created here even if a placement request isn't required

    Ideally a placement requirements would be created for each individual placement application instead
     */
    val placementRequirementsResult = cas1PlacementRequirementsService.createPlacementRequirements(assessment, placementRequirements)

    if (createPlacementRequest) {
      placementRequestService.createPlacementRequest(
        PlacementRequestSource.ASSESSMENT_OF_APPLICATION,
        placementRequirementsResult,
        placementDates,
        notes,
        false,
        null,
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

    if (createPlacementRequest) {
      cas1PlacementRequestEmailService.placementRequestSubmitted(application)
    }

    return CasResult.Success(savedAssessment)
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

    preUpdateAssessment(assessment)
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

  private fun prePersistClarificationNote(note: AssessmentClarificationNoteEntity) {
    assessmentClarificationNoteListener.prePersist(note)
  }

  private fun preUpdateClarificationNote(note: AssessmentClarificationNoteEntity) {
    assessmentClarificationNoteListener.preUpdate(note)
  }

  private fun preUpdateAssessment(assessment: ApprovedPremisesAssessmentEntity) {
    assessmentListener.preUpdate(assessment)
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
