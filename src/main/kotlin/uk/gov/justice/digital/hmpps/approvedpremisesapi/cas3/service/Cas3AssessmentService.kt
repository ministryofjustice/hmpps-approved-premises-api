package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.service

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.AssessmentSortField
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.UpdateAssessment
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.events.CAS3AssessmentUpdatedField
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.deliuscontext.CaseSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentDecision
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentReferralHistoryNoteRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentReferralHistorySystemNoteEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainAssessmentSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainAssessmentSummaryStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.LockableAssessmentRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ReferralHistorySystemNoteType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationAssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationAssessmentRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.findAssessmentById
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PaginationMetadata
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonSummaryInfoResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.CasResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.LaoStrategy
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.OffenderService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserAccessService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1LaoStrategy
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.PageCriteria
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.getMetadata
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.getPageableOrAllPages
import java.time.Clock
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
  private val offenderService: OffenderService,
  private val assessmentReferralHistoryNoteRepository: AssessmentReferralHistoryNoteRepository,
  private val lockableAssessmentRepository: LockableAssessmentRepository,
  private val clock: Clock,
) {
  fun getAssessmentSummariesForUser(
    user: UserEntity,
    crnOrName: String?,
    statuses: List<DomainAssessmentSummaryStatus>,
    pageCriteria: PageCriteria<AssessmentSortField>,
  ): Pair<List<DomainAssessmentSummary>, PaginationMetadata?> {
    val sortFieldString = when (pageCriteria.sortBy) {
      AssessmentSortField.assessmentStatus -> "status"
      AssessmentSortField.assessmentArrivalDate -> "arrivalDate"
      AssessmentSortField.assessmentCreatedAt -> "createdAt"
      AssessmentSortField.personCrn -> "crn"
      AssessmentSortField.applicationProbationDeliveryUnitName -> "probationDeliveryUnitName"
      else -> "arrivalDate"
    }
    val response = assessmentRepository.findTemporaryAccommodationAssessmentSummariesForRegionAndCrnAndStatus(
      user.probationRegion.id,
      crnOrName,
      statuses.map { it.name },
      getPageableOrAllPages(pageCriteria.withSortBy(sortFieldString)),
    )

    return Pair(response.content, getMetadata(response, pageCriteria))
  }

  fun getAssessmentAndValidate(
    user: UserEntity,
    assessmentId: UUID,
    forTimeline: Boolean = false,
  ): CasResult<TemporaryAccommodationAssessmentEntity> {
    val assessment = temporaryAccommodationAssessmentRepository.findByIdOrNull(assessmentId)
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

  @SuppressWarnings("ThrowsCount")
  fun acceptAssessment(
    acceptingUser: UserEntity,
    assessmentId: UUID,
    document: String?,
  ): CasResult<TemporaryAccommodationAssessmentEntity> {
    val acceptedAt = OffsetDateTime.now(clock)

    val assessment = when (val validation = validateAssessment(acceptingUser, assessmentId)) {
      is CasResult.Success -> validation.value
      else -> return validation
    }

    assessment.document = document
    assessment.submittedAt = acceptedAt
    assessment.decision = AssessmentDecision.ACCEPTED
    assessment.completedAt = null

    val savedAssessment = assessmentRepository.save(assessment)
    savedAssessment.addSystemNote(userService.getUserForRequest(), ReferralHistorySystemNoteType.READY_TO_PLACE)

    return CasResult.Success(savedAssessment)
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

  private fun validateAssessment(
    user: UserEntity,
    assessmentId: UUID,
  ): CasResult<TemporaryAccommodationAssessmentEntity> {
    val assessment = when (val assessmentResult = getAssessmentAndValidate(user, assessmentId)) {
      is CasResult.Success -> assessmentResult.value
      else -> return assessmentResult
    }

    if (assessment.reallocatedAt != null) {
      return CasResult.GeneralValidationError("The application has been reallocated, this assessment is read only")
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
