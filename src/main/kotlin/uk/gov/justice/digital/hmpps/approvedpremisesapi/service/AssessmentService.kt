package uk.gov.justice.digital.hmpps.approvedpremisesapi.service

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.allocations.UserAllocator
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.ApplicationAssessed
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.ApplicationAssessedAssessedBy
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.ApplicationAssessedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.Cru
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.PersonReference
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.ProbationArea
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.StaffMember
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.AssessmentSortField
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PlacementDates
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PlacementRequirements
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ClientResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.CommunityApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.NotifyConfig
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesAssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesAssessmentJsonSchemaEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentClarificationNoteEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentClarificationNoteRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentDecision
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentReferralHistoryNoteRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentReferralHistorySystemNoteEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentReferralHistoryUserNoteEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainAssessmentSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainAssessmentSummaryStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ReferralHistorySystemNoteType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationAssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationAssessmentJsonSchemaEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserQualification
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.DomainEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PaginationMetadata
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.ValidationErrors
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.OffenderDetailSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.StaffUserDetails
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.AuthorisableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.ValidatableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.PageCriteria
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.UrlTemplate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.getMetadata
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.getPageableOrAllPages
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.toLocalDateTime
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

@Service
@Suppress("ReturnCount", "CyclomaticComplexMethod")
class AssessmentService(
  private val userService: UserService,
  private val userAccessService: UserAccessService,
  private val assessmentRepository: AssessmentRepository,
  private val assessmentClarificationNoteRepository: AssessmentClarificationNoteRepository,
  private val assessmentReferralHistoryNoteRepository: AssessmentReferralHistoryNoteRepository,
  private val jsonSchemaService: JsonSchemaService,
  private val domainEventService: DomainEventService,
  private val offenderService: OffenderService,
  private val communityApiClient: CommunityApiClient,
  private val cruService: CruService,
  private val placementRequestService: PlacementRequestService,
  private val emailNotificationService: EmailNotificationService,
  private val notifyConfig: NotifyConfig,
  private val placementRequirementsService: PlacementRequirementsService,
  private val userAllocator: UserAllocator,
  private val objectMapper: ObjectMapper,
  @Value("\${url-templates.frontend.application}") private val applicationUrlTemplate: UrlTemplate,
  @Value("\${url-templates.frontend.assessment}") private val assessmentUrlTemplate: UrlTemplate,
  @Value("\${notify.send-placement-request-notifications}")
  private val sendPlacementRequestNotifications: Boolean,
  @Value("\${notify.send-new-withdrawal-notifications}")
  private val sendNewWithdrawalNotifications: Boolean,
) {
  fun getVisibleAssessmentSummariesForUserCAS1(
    user: UserEntity,
    statuses: List<DomainAssessmentSummaryStatus>,
    pageCriteria: PageCriteria<AssessmentSortField>,
  ): Pair<List<DomainAssessmentSummary>, PaginationMetadata?> {
    val pageable = buildPageable(pageCriteria)

    val response = assessmentRepository.findAllApprovedPremisesAssessmentSummariesNotReallocated(
      user.id.toString(),
      statuses.map { it.name },
      pageable,
    )

    return Pair(response.content, getMetadata(response, pageCriteria))
  }

  private fun buildPageable(pageCriteria: PageCriteria<AssessmentSortField>): Pageable? {
    val sortFieldString = when (pageCriteria.sortBy) {
      AssessmentSortField.assessmentStatus -> "status"
      AssessmentSortField.assessmentArrivalDate -> "arrivalDate"
      AssessmentSortField.assessmentCreatedAt -> "createdAt"
      AssessmentSortField.personCrn -> "crn"
      AssessmentSortField.personName -> "personName"
    }

    return getPageableOrAllPages(pageCriteria.withSortBy(sortFieldString))
  }

  fun getAssessmentSummariesForUserCAS3(
    user: UserEntity,
    crn: String?,
    serviceName: ServiceName,
    statuses: List<DomainAssessmentSummaryStatus>,
    pageCriteria: PageCriteria<AssessmentSortField>,
  ): Pair<List<DomainAssessmentSummary>, PaginationMetadata?> {
    when (serviceName) {
      ServiceName.temporaryAccommodation -> {
        val sortFieldString = when (pageCriteria.sortBy) {
          AssessmentSortField.assessmentStatus -> "status"
          AssessmentSortField.assessmentArrivalDate -> "arrivalDate"
          AssessmentSortField.assessmentCreatedAt -> "createdAt"
          AssessmentSortField.personCrn -> "crn"
          else -> "arrivalDate"
        }
        val response = assessmentRepository.findTemporaryAccommodationAssessmentSummariesForRegionAndCrnAndStatus(
          user.probationRegion.id,
          crn,
          statuses.map { it.name },
          getPageableOrAllPages(pageCriteria.withSortBy(sortFieldString)),
        )

        return Pair(response.content, getMetadata(response, pageCriteria))
      }
      else -> throw RuntimeException("Only CAS3 assessments are currently supported")
    }
  }

  fun getAssessmentForUser(user: UserEntity, assessmentId: UUID): AuthorisableActionResult<AssessmentEntity> {
    val assessment = assessmentRepository.findByIdOrNull(assessmentId)
      ?: return AuthorisableActionResult.NotFound()

    val latestSchema = when (assessment) {
      is ApprovedPremisesAssessmentEntity -> jsonSchemaService.getNewestSchema(
        ApprovedPremisesAssessmentJsonSchemaEntity::class.java,
      )

      is TemporaryAccommodationAssessmentEntity -> jsonSchemaService.getNewestSchema(
        TemporaryAccommodationAssessmentJsonSchemaEntity::class.java,
      )

      else -> throw RuntimeException("Assessment type '${assessment::class.qualifiedName}' is not currently supported")
    }

    if (!userAccessService.userCanViewAssessment(user, assessment)) {
      return AuthorisableActionResult.Unauthorised()
    }

    assessment.schemaUpToDate = assessment.schemaVersion.id == latestSchema.id

    val offenderResult = offenderService.getOffenderByCrn(
      assessment.application.crn,
      user.deliusUsername,
      user.hasQualification(UserQualification.LAO),
    )

    if (offenderResult !is AuthorisableActionResult.Success) {
      return AuthorisableActionResult.Unauthorised()
    }

    return AuthorisableActionResult.Success(assessment)
  }

  fun getAssessmentForUserAndApplication(
    user: UserEntity,
    applicationID: UUID,
  ): AuthorisableActionResult<AssessmentEntity> {
    val latestSchema = jsonSchemaService.getNewestSchema(ApprovedPremisesAssessmentJsonSchemaEntity::class.java)

    val assessment = assessmentRepository.findByApplication_IdAndReallocatedAtNull(applicationID)
      ?: return AuthorisableActionResult.NotFound()

    if (!user.hasRole(UserRole.CAS1_WORKFLOW_MANAGER) && assessment.allocatedToUser != user) {
      return AuthorisableActionResult.Unauthorised()
    }

    assessment.schemaUpToDate = assessment.schemaVersion.id == latestSchema.id

    return AuthorisableActionResult.Success(assessment)
  }

  fun createApprovedPremisesAssessment(application: ApprovedPremisesApplicationEntity, createdFromAppeal: Boolean = false): ApprovedPremisesAssessmentEntity {
    val dateTimeNow = OffsetDateTime.now()

    var assessment = ApprovedPremisesAssessmentEntity(
      id = UUID.randomUUID(),
      application = application,
      data = null,
      document = null,
      schemaVersion = jsonSchemaService.getNewestSchema(ApprovedPremisesAssessmentJsonSchemaEntity::class.java),
      allocatedToUser = null,
      allocatedAt = dateTimeNow,
      reallocatedAt = null,
      createdAt = dateTimeNow,
      submittedAt = null,
      decision = null,
      schemaUpToDate = true,
      rejectionRationale = null,
      clarificationNotes = mutableListOf(),
      referralHistoryNotes = mutableListOf(),
      isWithdrawn = false,
      createdFromAppeal = createdFromAppeal,
    )

    val allocatedUser = userAllocator.getUserForAssessmentAllocation(assessment)
    assessment.allocatedToUser = allocatedUser

    assessment = assessmentRepository.save(assessment)

    if (allocatedUser != null) {
      if (allocatedUser.email != null) {
        emailNotificationService.sendEmail(
          recipientEmailAddress = allocatedUser.email!!,
          templateId = notifyConfig.templates.assessmentAllocated,
          personalisation = mapOf(
            "name" to allocatedUser.name,
            "assessmentUrl" to assessmentUrlTemplate.resolve("id", assessment.id.toString()),
            "crn" to application.crn,
          ),
        )
      }
    }

    return assessment
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
        schemaVersion = jsonSchemaService.getNewestSchema(TemporaryAccommodationAssessmentJsonSchemaEntity::class.java),
        allocatedToUser = null,
        allocatedAt = dateTimeNow,
        reallocatedAt = null,
        createdAt = dateTimeNow,
        submittedAt = null,
        decision = null,
        schemaUpToDate = true,
        rejectionRationale = null,
        clarificationNotes = mutableListOf(),
        referralHistoryNotes = mutableListOf(),
        completedAt = null,
        summaryData = objectMapper.writeValueAsString(summaryData),
        isWithdrawn = false,
      ),
    )

    assessment.addSystemNote(userService.getUserForRequest(), ReferralHistorySystemNoteType.SUBMITTED)

    return assessment
  }

  fun updateAssessment(
    user: UserEntity,
    assessmentId: UUID,
    data: String?,
  ): AuthorisableActionResult<ValidatableActionResult<AssessmentEntity>> {
    val assessmentResult = getAssessmentForUser(user, assessmentId)

    val assessment = when (assessmentResult) {
      is AuthorisableActionResult.Success -> assessmentResult.entity
      is AuthorisableActionResult.Unauthorised -> return AuthorisableActionResult.Unauthorised()
      is AuthorisableActionResult.NotFound -> return AuthorisableActionResult.NotFound()
    }

    if (assessment.isWithdrawn) {
      return AuthorisableActionResult.Success(
        ValidatableActionResult.GeneralValidationError("The application has been withdrawn."),
      )
    }

    if (!assessment.schemaUpToDate) {
      return AuthorisableActionResult.Success(
        ValidatableActionResult.GeneralValidationError("The schema version is outdated"),
      )
    }

    if (assessment.submittedAt != null) {
      return AuthorisableActionResult.Success(
        ValidatableActionResult.GeneralValidationError("A decision has already been taken on this assessment"),
      )
    }

    if (assessment.reallocatedAt != null) {
      return AuthorisableActionResult.Success(
        ValidatableActionResult.GeneralValidationError("The application has been reallocated, this assessment is read only"),
      )
    }

    assessment.data = data

    val savedAssessment = assessmentRepository.save(assessment)

    return AuthorisableActionResult.Success(
      ValidatableActionResult.Success(savedAssessment),
    )
  }

  fun acceptAssessment(
    user: UserEntity,
    assessmentId: UUID,
    document: String?,
    placementRequirements: PlacementRequirements?,
    placementDates: PlacementDates?,
    notes: String?,
  ): AuthorisableActionResult<ValidatableActionResult<AssessmentEntity>> {
    val acceptedAt = OffsetDateTime.now()
    val createPlacementRequest = placementDates != null

    val assessmentResult = getAssessmentForUser(user, assessmentId)
    val assessment = when (assessmentResult) {
      is AuthorisableActionResult.Success -> assessmentResult.entity
      is AuthorisableActionResult.Unauthorised -> return AuthorisableActionResult.Unauthorised()
      is AuthorisableActionResult.NotFound -> return AuthorisableActionResult.NotFound()
    }

    if (!assessment.schemaUpToDate) {
      return AuthorisableActionResult.Success(
        ValidatableActionResult.GeneralValidationError("The schema version is outdated"),
      )
    }

    if (assessment is ApprovedPremisesAssessmentEntity && assessment.submittedAt != null) {
      return AuthorisableActionResult.Success(
        ValidatableActionResult.GeneralValidationError("A decision has already been taken on this assessment"),
      )
    }

    if (assessment.reallocatedAt != null) {
      return AuthorisableActionResult.Success(
        ValidatableActionResult.GeneralValidationError("The application has been reallocated, this assessment is read only"),
      )
    }

    val validationErrors = ValidationErrors()
    val assessmentData = assessment.data

    if (assessment is ApprovedPremisesAssessmentEntity) {
      if (assessmentData == null) {
        validationErrors["$.data"] = "empty"
      } else if (!jsonSchemaService.validate(assessment.schemaVersion, assessmentData)) {
        validationErrors["$.data"] = "invalid"
      }

      if (placementRequirements == null) {
        validationErrors["$.requirements"] = "empty"
      }
    }

    if (validationErrors.any()) {
      return AuthorisableActionResult.Success(
        ValidatableActionResult.FieldValidationError(validationErrors),
      )
    }

    assessment.document = document
    assessment.submittedAt = acceptedAt
    assessment.decision = AssessmentDecision.ACCEPTED

    if (assessment is TemporaryAccommodationAssessmentEntity) {
      assessment.completedAt = null
    }

    val savedAssessment = assessmentRepository.save(assessment)

    if (savedAssessment is TemporaryAccommodationAssessmentEntity) {
      savedAssessment.addSystemNote(userService.getUserForRequest(), ReferralHistorySystemNoteType.READY_TO_PLACE)
    }

    if (assessment is ApprovedPremisesAssessmentEntity) {
      val placementRequirementsValidationResult =
        placementRequirementsService.createPlacementRequirements(assessment, placementRequirements!!)

      if (placementRequirementsValidationResult !is ValidatableActionResult.Success) {
        return AuthorisableActionResult.Success(
          placementRequirementsValidationResult.translateError(),
        )
      }

      if (createPlacementRequest) {
        placementRequestService.createPlacementRequest(
          placementRequirementsValidationResult.entity,
          placementDates!!,
          notes,
          false,
          null,
        )
      }
    }

    val application = savedAssessment.application

    val offenderDetails =
      when (val offenderDetailsResult = offenderService.getOffenderByCrn(application.crn, user.deliusUsername, true)) {
        is AuthorisableActionResult.Success -> offenderDetailsResult.entity
        is AuthorisableActionResult.Unauthorised -> throw RuntimeException("Unable to get Offender Details when creating Application Assessed Domain Event: Unauthorised")
        is AuthorisableActionResult.NotFound -> throw RuntimeException("Unable to get Offender Details when creating Application Assessed Domain Event: Not Found")
      }

    val staffDetailsResult = communityApiClient.getStaffUserDetails(user.deliusUsername)
    val staffDetails = when (staffDetailsResult) {
      is ClientResult.Success -> staffDetailsResult.body
      is ClientResult.Failure -> staffDetailsResult.throwException()
    }

    if (application is ApprovedPremisesApplicationEntity) {
      saveCas1ApplicationAssessedDomainEvent(application, assessment, offenderDetails, staffDetails, placementDates)

      application.createdByUser.email?.let { email ->
        emailNotificationService.sendEmail(
          recipientEmailAddress = email,
          templateId = notifyConfig.templates.assessmentAccepted,
          personalisation = mapOf(
            "name" to application.createdByUser.name,
            "applicationUrl" to applicationUrlTemplate.resolve("id", application.id.toString()),
            "crn" to application.crn,
          ),
        )

        if (createPlacementRequest && sendPlacementRequestNotifications) {
          emailNotificationService.sendEmail(
            recipientEmailAddress = email,
            templateId = notifyConfig.templates.placementRequestSubmitted,
            personalisation = mapOf(
              "crn" to application.crn,
            ),
          )
        }
      }
    }

    return AuthorisableActionResult.Success(
      ValidatableActionResult.Success(savedAssessment),
    )
  }

  private fun saveCas1ApplicationAssessedDomainEvent(
    application: ApprovedPremisesApplicationEntity,
    assessment: AssessmentEntity,
    offenderDetails: OffenderDetailSummary,
    staffDetails: StaffUserDetails,
    placementDates: PlacementDates?,
  ) {
    val domainEventId = UUID.randomUUID()
    val acceptedAt = assessment.submittedAt!!

    domainEventService.saveApplicationAssessedDomainEvent(
      DomainEvent(
        id = domainEventId,
        applicationId = application.id,
        assessmentId = assessment.id,
        crn = application.crn,
        occurredAt = acceptedAt.toInstant(),
        data = ApplicationAssessedEnvelope(
          id = domainEventId,
          timestamp = acceptedAt.toInstant(),
          eventType = "approved-premises.application.assessed",
          eventDetails = ApplicationAssessed(
            applicationId = application.id,
            applicationUrl = applicationUrlTemplate
              .resolve("id", application.id.toString()),
            personReference = PersonReference(
              crn = offenderDetails.otherIds.crn,
              noms = offenderDetails.otherIds.nomsNumber ?: "Unknown NOMS Number",
            ),
            deliusEventNumber = application.eventNumber,
            assessedAt = acceptedAt.toInstant(),
            assessedBy = ApplicationAssessedAssessedBy(
              staffMember = StaffMember(
                staffCode = staffDetails.staffCode,
                staffIdentifier = staffDetails.staffIdentifier,
                forenames = staffDetails.staff.forenames,
                surname = staffDetails.staff.surname,
                username = staffDetails.username,
              ),
              probationArea = ProbationArea(
                code = staffDetails.probationArea.code,
                name = staffDetails.probationArea.description,
              ),
              cru = Cru(
                name = cruService.cruNameFromProbationAreaCode(staffDetails.probationArea.code),
              ),
            ),
            decision = assessment.decision.toString(),
            decisionRationale = assessment.rejectionRationale,
            arrivalDate = placementDates?.expectedArrival?.toLocalDateTime()?.toInstant(),
          ),
        ),
      ),
    )
  }

  fun rejectAssessment(
    user: UserEntity,
    assessmentId: UUID,
    document: String?,
    rejectionRationale: String,
  ): AuthorisableActionResult<ValidatableActionResult<AssessmentEntity>> {
    val domainEventId = UUID.randomUUID()
    val rejectedAt = OffsetDateTime.now()

    val assessmentResult = getAssessmentForUser(user, assessmentId)
    val assessment = when (assessmentResult) {
      is AuthorisableActionResult.Success -> assessmentResult.entity
      is AuthorisableActionResult.Unauthorised -> return AuthorisableActionResult.Unauthorised()
      is AuthorisableActionResult.NotFound -> return AuthorisableActionResult.NotFound()
    }

    if (!assessment.schemaUpToDate) {
      return AuthorisableActionResult.Success(
        ValidatableActionResult.GeneralValidationError("The schema version is outdated"),
      )
    }

    if (assessment is ApprovedPremisesAssessmentEntity && assessment.submittedAt != null) {
      return AuthorisableActionResult.Success(
        ValidatableActionResult.GeneralValidationError("A decision has already been taken on this assessment"),
      )
    }

    if (assessment.reallocatedAt != null) {
      return AuthorisableActionResult.Success(
        ValidatableActionResult.GeneralValidationError("The application has been reallocated, this assessment is read only"),
      )
    }

    val validationErrors = ValidationErrors()
    val assessmentData = assessment.data

    if (assessment is ApprovedPremisesAssessmentEntity) {
      if (assessmentData == null) {
        validationErrors["$.data"] = "empty"
      } else if (!jsonSchemaService.validate(assessment.schemaVersion, assessmentData)) {
        validationErrors["$.data"] = "invalid"
      }
    }

    if (validationErrors.any()) {
      return AuthorisableActionResult.Success(
        ValidatableActionResult.FieldValidationError(validationErrors),
      )
    }

    assessment.document = document
    assessment.submittedAt = rejectedAt
    assessment.decision = AssessmentDecision.REJECTED
    assessment.rejectionRationale = rejectionRationale

    if (assessment is TemporaryAccommodationAssessmentEntity) {
      assessment.completedAt = null
    }

    val savedAssessment = assessmentRepository.save(assessment)

    if (savedAssessment is TemporaryAccommodationAssessmentEntity) {
      savedAssessment.addSystemNote(userService.getUserForRequest(), ReferralHistorySystemNoteType.REJECTED)
    }

    val application = savedAssessment.application

    val offenderDetails =
      when (val offenderDetailsResult = offenderService.getOffenderByCrn(application.crn, user.deliusUsername, true)) {
        is AuthorisableActionResult.Success -> offenderDetailsResult.entity
        else -> null
      }

    val staffDetailsResult = communityApiClient.getStaffUserDetails(user.deliusUsername)
    val staffDetails = when (staffDetailsResult) {
      is ClientResult.Success -> staffDetailsResult.body
      is ClientResult.Failure -> staffDetailsResult.throwException()
    }

    if (application is ApprovedPremisesApplicationEntity) {
      domainEventService.saveApplicationAssessedDomainEvent(
        DomainEvent(
          id = domainEventId,
          applicationId = application.id,
          assessmentId = assessment.id,
          crn = application.crn,
          occurredAt = rejectedAt.toInstant(),
          data = ApplicationAssessedEnvelope(
            id = domainEventId,
            timestamp = rejectedAt.toInstant(),
            eventType = "approved-premises.application.assessed",
            eventDetails = ApplicationAssessed(
              applicationId = application.id,
              applicationUrl = applicationUrlTemplate
                .resolve("id", application.id.toString()),
              personReference = PersonReference(
                crn = assessment.application.crn,
                noms = offenderDetails?.otherIds?.nomsNumber ?: "Unknown NOMS Number",
              ),
              deliusEventNumber = application.eventNumber,
              assessedAt = rejectedAt.toInstant(),
              assessedBy = ApplicationAssessedAssessedBy(
                staffMember = StaffMember(
                  staffCode = staffDetails.staffCode,
                  staffIdentifier = staffDetails.staffIdentifier,
                  forenames = staffDetails.staff.forenames,
                  surname = staffDetails.staff.surname,
                  username = staffDetails.username,
                ),
                probationArea = ProbationArea(
                  code = staffDetails.probationArea.code,
                  name = staffDetails.probationArea.description,
                ),
                cru = Cru(
                  name = cruService.cruNameFromProbationAreaCode(staffDetails.probationArea.code),
                ),
              ),
              decision = assessment.decision.toString(),
              decisionRationale = assessment.rejectionRationale,
              arrivalDate = null,
            ),
          ),
        ),
      )
      if (application.createdByUser.email != null) {
        emailNotificationService.sendEmail(
          recipientEmailAddress = application.createdByUser.email!!,
          templateId = notifyConfig.templates.assessmentRejected,
          personalisation = mapOf(
            "name" to application.createdByUser.name,
            "applicationUrl" to applicationUrlTemplate.resolve("id", application.id.toString()),
            "crn" to application.crn,
          ),
        )
      }
    }

    return AuthorisableActionResult.Success(
      ValidatableActionResult.Success(savedAssessment),
    )
  }

  fun closeAssessment(
    user: UserEntity,
    assessmentId: UUID,
  ): AuthorisableActionResult<ValidatableActionResult<AssessmentEntity>> {
    val assessment = when (val assessmentResult = getAssessmentForUser(user, assessmentId)) {
      is AuthorisableActionResult.Success -> assessmentResult.entity
      is AuthorisableActionResult.Unauthorised -> return AuthorisableActionResult.Unauthorised()
      is AuthorisableActionResult.NotFound -> return AuthorisableActionResult.NotFound()
    }

    if (assessment !is TemporaryAccommodationAssessmentEntity) {
      throw RuntimeException("Only CAS3 is currently supported")
    }

    if (!assessment.schemaUpToDate) {
      return AuthorisableActionResult.Success(
        ValidatableActionResult.GeneralValidationError("The schema version is outdated"),
      )
    }

    if (assessment.completedAt != null) {
      return AuthorisableActionResult.Success(
        ValidatableActionResult.GeneralValidationError("This assessment has already been closed"),
      )
    }

    assessment.completedAt = OffsetDateTime.now()

    val savedAssessment = assessmentRepository.save(assessment)
    savedAssessment.addSystemNote(userService.getUserForRequest(), ReferralHistorySystemNoteType.COMPLETED)

    return AuthorisableActionResult.Success(
      ValidatableActionResult.Success(savedAssessment),
    )
  }

  fun reallocateAssessment(
    assigneeUser: UserEntity,
    id: UUID,
  ): AuthorisableActionResult<ValidatableActionResult<AssessmentEntity>> {
    val currentAssessment = assessmentRepository.findByIdOrNull(id)
      ?: return AuthorisableActionResult.NotFound()

    return when (currentAssessment) {
      is ApprovedPremisesAssessmentEntity -> reallocateApprovedPremisesAssessment(assigneeUser, currentAssessment)
      is TemporaryAccommodationAssessmentEntity -> reallocateTemporaryAccommodationAssessment(
        assigneeUser,
        currentAssessment,
      )

      else -> throw RuntimeException("Reallocating an assessment of type '${currentAssessment::class.qualifiedName}' has not been implemented.")
    }
  }

  private fun reallocateApprovedPremisesAssessment(
    assigneeUser: UserEntity,
    currentAssessment: ApprovedPremisesAssessmentEntity,
  ): AuthorisableActionResult<ValidatableActionResult<AssessmentEntity>> {
    if (currentAssessment.submittedAt != null) {
      return AuthorisableActionResult.Success(
        ValidatableActionResult.GeneralValidationError("A decision has already been taken on this assessment"),
      )
    }

    val application = currentAssessment.application
    val requiredQualifications = application.getRequiredQualifications()

    if (!assigneeUser.hasRole(UserRole.CAS1_ASSESSOR)) {
      return AuthorisableActionResult.Success(
        ValidatableActionResult.FieldValidationError(
          ValidationErrors().apply {
            this["$.userId"] = "lackingAssessorRole"
          },
        ),
      )
    }

    if (!assigneeUser.hasAllQualifications(requiredQualifications)) {
      return AuthorisableActionResult.Success(
        ValidatableActionResult.FieldValidationError(
          ValidationErrors().apply {
            this["$.userId"] = "lackingQualifications"
          },
        ),
      )
    }

    currentAssessment.reallocatedAt = OffsetDateTime.now()
    assessmentRepository.save(currentAssessment)

    // Make the timestamp precision less precise, so we don't have any issues with microsecond resolution in tests
    val dateTimeNow = OffsetDateTime.now().withNano(0)

    val newAssessment = assessmentRepository.save(
      ApprovedPremisesAssessmentEntity(
        id = UUID.randomUUID(),
        application = application,
        data = null,
        document = null,
        schemaVersion = jsonSchemaService.getNewestSchema(ApprovedPremisesAssessmentJsonSchemaEntity::class.java),
        allocatedToUser = assigneeUser,
        allocatedAt = dateTimeNow,
        reallocatedAt = null,
        createdAt = dateTimeNow,
        submittedAt = null,
        decision = null,
        schemaUpToDate = true,
        rejectionRationale = null,
        clarificationNotes = mutableListOf(),
        referralHistoryNotes = mutableListOf(),
        isWithdrawn = false,
        createdFromAppeal = currentAssessment.createdFromAppeal,
      ),
    )

    if (application is ApprovedPremisesApplicationEntity) {
      if (assigneeUser.email != null) {
        emailNotificationService.sendEmail(
          recipientEmailAddress = assigneeUser.email!!,
          templateId = notifyConfig.templates.assessmentAllocated,
          personalisation = mapOf(
            "name" to assigneeUser.name,
            "assessmentUrl" to assessmentUrlTemplate.resolve("id", newAssessment.id.toString()),
            "crn" to application.crn,
          ),
        )
      }
      val allocatedToUser = currentAssessment.allocatedToUser
      if (allocatedToUser != null) {
        if (allocatedToUser.email != null) {
          emailNotificationService.sendEmail(
            recipientEmailAddress = allocatedToUser.email!!,
            templateId = notifyConfig.templates.assessmentDeallocated,
            personalisation = mapOf(
              "name" to currentAssessment.allocatedToUser!!.name,
              "assessmentUrl" to assessmentUrlTemplate.resolve("id", newAssessment.id.toString()),
              "crn" to application.crn,
            ),
          )
        }
      }
    }

    return AuthorisableActionResult.Success(
      ValidatableActionResult.Success(
        newAssessment,
      ),
    )
  }

  private fun reallocateTemporaryAccommodationAssessment(
    assigneeUser: UserEntity,
    currentAssessment: TemporaryAccommodationAssessmentEntity,
  ): AuthorisableActionResult<ValidatableActionResult<AssessmentEntity>> {
    if (!assigneeUser.hasRole(UserRole.CAS3_ASSESSOR)) {
      return AuthorisableActionResult.Success(
        ValidatableActionResult.FieldValidationError(
          ValidationErrors().apply {
            this["$.userId"] = "lackingAssessorRole"
          },
        ),
      )
    }

    currentAssessment.allocatedToUser = assigneeUser
    currentAssessment.allocatedAt = OffsetDateTime.now()
    currentAssessment.decision = null

    val savedAssessment = assessmentRepository.save(currentAssessment)
    savedAssessment.addSystemNote(userService.getUserForRequest(), ReferralHistorySystemNoteType.IN_REVIEW)

    return AuthorisableActionResult.Success(
      ValidatableActionResult.Success(
        savedAssessment,
      ),
    )
  }

  fun deallocateAssessment(id: UUID): AuthorisableActionResult<ValidatableActionResult<AssessmentEntity>> {
    val currentAssessment = assessmentRepository.findByIdOrNull(id)
      ?: return AuthorisableActionResult.NotFound()

    if (currentAssessment !is TemporaryAccommodationAssessmentEntity) {
      throw RuntimeException("Only CAS3 Assessments are currently supported")
    }

    currentAssessment.allocatedToUser = null
    currentAssessment.allocatedAt = null
    currentAssessment.decision = null
    currentAssessment.submittedAt = null

    val savedAssessment = assessmentRepository.save(currentAssessment)
    savedAssessment.addSystemNote(userService.getUserForRequest(), ReferralHistorySystemNoteType.UNALLOCATED)

    return AuthorisableActionResult.Success(
      ValidatableActionResult.Success(
        savedAssessment,
      ),
    )
  }

  fun addAssessmentClarificationNote(
    user: UserEntity,
    assessmentId: UUID,
    text: String,
  ): AuthorisableActionResult<AssessmentClarificationNoteEntity> {
    val assessmentResult = getAssessmentForUser(user, assessmentId)
    val assessment = when (assessmentResult) {
      is AuthorisableActionResult.Success -> assessmentResult.entity
      is AuthorisableActionResult.Unauthorised -> return AuthorisableActionResult.Unauthorised()
      is AuthorisableActionResult.NotFound -> return AuthorisableActionResult.NotFound()
    }

    val clarificationNoteEntity = assessmentClarificationNoteRepository.save(
      AssessmentClarificationNoteEntity(
        id = UUID.randomUUID(),
        assessment = assessment,
        createdByUser = user,
        createdAt = OffsetDateTime.now(),
        query = text,
        response = null,
        responseReceivedOn = null,
      ),
    )

    return AuthorisableActionResult.Success(clarificationNoteEntity)
  }

  fun updateAssessmentClarificationNote(
    user: UserEntity,
    assessmentId: UUID,
    id: UUID,
    response: String,
    responseReceivedOn: LocalDate,
  ): AuthorisableActionResult<ValidatableActionResult<AssessmentClarificationNoteEntity>> {
    val assessment = when (val assessmentResult = getAssessmentForUser(user, assessmentId)) {
      is AuthorisableActionResult.Success -> assessmentResult.entity
      is AuthorisableActionResult.Unauthorised -> return AuthorisableActionResult.Unauthorised()
      is AuthorisableActionResult.NotFound -> return AuthorisableActionResult.NotFound()
    }

    val clarificationNoteEntity = assessmentClarificationNoteRepository.findByAssessmentIdAndId(assessment.id, id)

    if (clarificationNoteEntity === null) {
      return AuthorisableActionResult.NotFound()
    }

    if (clarificationNoteEntity.createdByUser.id !== user.id) {
      return AuthorisableActionResult.Unauthorised()
    }

    if (clarificationNoteEntity.response !== null) {
      return AuthorisableActionResult.Success(
        ValidatableActionResult.GeneralValidationError("A response has already been added to this note"),
      )
    }

    clarificationNoteEntity.response = response
    clarificationNoteEntity.responseReceivedOn = responseReceivedOn

    val savedNote = assessmentClarificationNoteRepository.save(clarificationNoteEntity)
    // We need to save the assessment here to update the Application's status
    assessmentRepository.save(clarificationNoteEntity.assessment)

    return AuthorisableActionResult.Success(
      ValidatableActionResult.Success(savedNote),
    )
  }

  fun addAssessmentReferralHistoryUserNote(
    user: UserEntity,
    assessmentId: UUID,
    text: String,
  ): AuthorisableActionResult<AssessmentReferralHistoryUserNoteEntity> {
    val assessment = when (val assessmentResult = getAssessmentForUser(user, assessmentId)) {
      is AuthorisableActionResult.Success -> assessmentResult.entity
      is AuthorisableActionResult.Unauthorised -> return AuthorisableActionResult.Unauthorised()
      is AuthorisableActionResult.NotFound -> return AuthorisableActionResult.NotFound()
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

    return AuthorisableActionResult.Success(referralHistoryNoteEntity)
  }

  fun updateCas1AssessmentWithdrawn(assessmentId: UUID) {
    val assessment = assessmentRepository.findByIdOrNull(assessmentId)
    if (assessment is ApprovedPremisesAssessmentEntity) {
      val isPendingAssessment = assessment.isPendingAssessment()

      assessment.isWithdrawn = true
      assessmentRepository.save(assessment)

      val allocatedUserEmail = assessment.allocatedToUser?.email
      if (sendNewWithdrawalNotifications &&
        isPendingAssessment &&
        allocatedUserEmail != null
      ) {
        emailNotificationService.sendEmail(
          recipientEmailAddress = allocatedUserEmail,
          templateId = notifyConfig.templates.assessmentWithdrawn,
          personalisation = mapOf(
            "applicationUrl" to applicationUrlTemplate.resolve("id", assessment.application.id.toString()),
            "crn" to assessment.application.crn,
          ),
        )
      }
    }
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
}
