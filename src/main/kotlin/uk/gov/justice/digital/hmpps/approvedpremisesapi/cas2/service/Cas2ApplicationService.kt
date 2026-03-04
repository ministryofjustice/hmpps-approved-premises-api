package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.service

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.transaction.Transactional
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.jpa.domain.Specification
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas2.model.Cas2ApplicationSubmittedEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas2.model.Cas2ApplicationSubmittedEventDetails
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas2.model.Cas2ApplicationSubmittedEventDetailsSubmittedBy
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas2.model.Cas2StaffMember
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas2.model.EventType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas2.model.PersonReference
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApplicationOrigin
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.AssignmentType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SubmitCas2v2Application
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.Cas2ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.Cas2ApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.Cas2ApplicationSummaryEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.Cas2ApplicationSummaryRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.Cas2LockableApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.Cas2UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model.Cas2ServiceOrigin
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model.SubmitCas2Application
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.service.Cas2OffenderSearchResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2v2.jpa.entity.Cas2v2ApplicationSummarySpecifications
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.Cas2NotifyTemplates
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.NotifyConfig
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.DomainEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PaginationMetadata
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonInfoResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.ValidationErrors
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.validated
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.AuthorisableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.CasResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.EmailNotificationService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.SentryService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UpstreamApiException
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.PageCriteria
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.getMetadata
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.getPageableOrAllPages
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

@Suppress("TooManyFunctions")
@SuppressWarnings("TooGenericExceptionThrown")
@Service
class Cas2ApplicationService(
  private val applicationRepository: Cas2ApplicationRepository,
  private val lockableApplicationRepository: Cas2LockableApplicationRepository,
  private val applicationSummaryRepository: Cas2ApplicationSummaryRepository,
  private val offenderService: Cas2OffenderService,
  private val userAccessService: Cas2UserAccessService,
  private val domainEventService: Cas2DomainEventService,
  private val emailNotificationService: EmailNotificationService,
  private val assessmentService: Cas2AssessmentService,
  private val notifyConfig: NotifyConfig,
  private val objectMapper: ObjectMapper,
  private val sentryService: SentryService,
  @Value("\${url-templates.frontend.cas2.application}") private val applicationUrlTemplate: String,
  @Value("\${url-templates.frontend.cas2.submitted-application-overview}") private val submittedApplicationUrlTemplate: String,
  @Value("\${url-templates.frontend.cas2v2.application}") private val cas2v2ApplicationUrlTemplate: String,
  @Value("\${url-templates.frontend.cas2v2.submitted-application-overview}") private val cas2v2SubmittedApplicationUrlTemplate: String,
) {

  fun getCas2v2Applications(
    prisonCode: String?,
    isSubmitted: Boolean?,
    applicationOrigin: ApplicationOrigin?,
    limitByUser: Boolean,
    crnOrNomsNumber: String?,
    user: Cas2UserEntity,
    pageCriteria: PageCriteria<String>,
  ): Pair<MutableList<Cas2ApplicationSummaryEntity>, PaginationMetadata?> {
    var spec: Specification<Cas2ApplicationSummaryEntity> =
      Specification { _, _, cb -> cb.conjunction() }

    if (limitByUser) {
      spec = spec.and(Cas2v2ApplicationSummarySpecifications.hasUserId(user.id.toString()))
    }

    if (crnOrNomsNumber != null) {
      spec = spec.and(Cas2v2ApplicationSummarySpecifications.hasCrnOrNomsNumber(crnOrNomsNumber))
    }

    if (prisonCode != null) {
      spec = spec.and(Cas2v2ApplicationSummarySpecifications.hasPrisonCode(prisonCode))
    }

    if (applicationOrigin != null) {
      spec = spec.and(Cas2v2ApplicationSummarySpecifications.hasApplicationOrigin(applicationOrigin))
    }

    if (isSubmitted != null) {
      spec = spec.and(Cas2v2ApplicationSummarySpecifications.isSubmitted(isSubmitted))
    }

    spec = spec.and(Cas2v2ApplicationSummarySpecifications.hasServiceOrigin(Cas2ServiceOrigin.BAIL.toString()))

    val response = applicationSummaryRepository.findAll(spec, getPageableOrAllPages(pageCriteria))

    val metadata = getMetadata(response, pageCriteria)
    return Pair(response.content, metadata)
  }

  fun getAllSubmittedCas2v2ApplicationsForAssessor(pageCriteria: PageCriteria<String>): Pair<List<Cas2ApplicationSummaryEntity>, PaginationMetadata?> {
    val pageable = getPageableOrAllPages(pageCriteria)

    val response = applicationSummaryRepository.findByServiceOriginAndSubmittedAtIsNotNull(Cas2ServiceOrigin.BAIL.toString(), pageable)

    val metadata = getMetadata(response, pageCriteria)

    return Pair(response.content, metadata)
  }

  fun getSubmittedCas2v2ApplicationForAssessor(applicationId: UUID): CasResult<Cas2ApplicationEntity> {
    val applicationEntity = applicationRepository.findByIdAndServiceOriginAndSubmittedAtIsNotNull(applicationId, Cas2ServiceOrigin.BAIL)
      ?: return CasResult.NotFound("Cas2ApplicationEntity", applicationId.toString())

    return CasResult.Success(applicationEntity)
  }

  fun getCas2v2ApplicationForUser(applicationId: UUID, user: Cas2UserEntity): CasResult<Cas2ApplicationEntity> {
    val applicationEntity = applicationRepository.findByIdAndServiceOrigin(applicationId, Cas2ServiceOrigin.BAIL)
      ?: return CasResult.NotFound("Cas2ApplicationEntity", applicationId.toString())

    if (applicationEntity.abandonedAt != null) {
      return CasResult.NotFound("Cas2ApplicationEntity", applicationId.toString())
    }

    val canAccess = userAccessService.userCanViewCas2v2Application(user, applicationEntity)

    return if (canAccess) {
      CasResult.Success(applicationEntity)
    } else {
      CasResult.Unauthorised()
    }
  }

  @SuppressWarnings("TooGenericExceptionThrown")
  fun createCas2v2Application(
    crn: String,
    user: Cas2UserEntity,
    applicationOrigin: ApplicationOrigin = ApplicationOrigin.homeDetentionCurfew,
    bailHearingDate: LocalDate? = null,
  ) = validated<Cas2ApplicationEntity> {
    val offenderDetailsResult = offenderService.getPersonByNomisIdOrCrn(crn)

    val offenderDetails = when (offenderDetailsResult) {
      is Cas2OffenderSearchResult.NotFound -> return "$.crn" hasSingleValidationError "doesNotExist"
      is Cas2OffenderSearchResult.Forbidden -> return "$.crn" hasSingleValidationError "userPermission"
      is Cas2OffenderSearchResult.Unknown -> return "$.crn" hasSingleValidationError "unknown"
      is Cas2OffenderSearchResult.Success.Full -> offenderDetailsResult.person
    }

    if (validationErrors.any()) {
      return fieldValidationError
    }

    val id = UUID.randomUUID()

    val entityToSave = Cas2ApplicationEntity(
      id = id,
      crn = crn,
      createdByUser = user,
      data = null,
      document = null,
      createdAt = OffsetDateTime.now(),
      submittedAt = null,
      nomsNumber = offenderDetails.nomsNumber,
      telephoneNumber = null,
      applicationOrigin = applicationOrigin,
      bailHearingDate = bailHearingDate,
      serviceOrigin = Cas2ServiceOrigin.BAIL,
    )

    val createdApplication = applicationRepository.save(entityToSave)

    return success(createdApplication)
  }

  @SuppressWarnings("ReturnCount")
  fun updateCas2v2Application(
    applicationId: UUID,
    data: String?,
    user: Cas2UserEntity,
    bailHearingDate: LocalDate?,
  ): CasResult<Cas2ApplicationEntity> {
    val application = applicationRepository.findByIdAndServiceOrigin(applicationId, Cas2ServiceOrigin.BAIL)
      ?: return CasResult.NotFound("Cas2ApplicationEntity", applicationId.toString())

    if (!application.isCreatedBy(user)) {
      return CasResult.Unauthorised()
    }

    if (application.submittedAt != null) {
      return CasResult.GeneralValidationError("This application has already been submitted")
    }

    if (application.abandonedAt != null) {
      return CasResult.GeneralValidationError("This application has been abandoned")
    }

    application.bailHearingDate = bailHearingDate

    application.apply {
      this.data = removeXssCharacters(data)
    }

    val savedApplication = applicationRepository.save(application)

    return CasResult.Success(savedApplication)
  }

  @SuppressWarnings("ReturnCount")
  fun abandonCas2v2Application(applicationId: UUID, user: Cas2UserEntity): CasResult<Cas2ApplicationEntity> {
    val application = applicationRepository.findByIdAndServiceOrigin(applicationId, Cas2ServiceOrigin.BAIL)
      ?: return CasResult.NotFound("Cas2ApplicationEntity", applicationId.toString())

    if (!application.isCreatedBy(user)) {
      return CasResult.Unauthorised()
    }

    if (application.submittedAt != null) {
      return CasResult.ConflictError(applicationId, "This application has already been submitted")
    }

    if (application.abandonedAt != null) {
      return CasResult.Success(application)
    }

    application.apply {
      this.abandonedAt = OffsetDateTime.now()
      this.data = null
    }

    val savedApplication = applicationRepository.save(application)

    return CasResult.Success(savedApplication)
  }

  @SuppressWarnings("ReturnCount", "TooGenericExceptionThrown")
  @Transactional
  fun submitCas2v2Application(
    submitCas2v2Application: SubmitCas2v2Application,
    user: Cas2UserEntity,
  ): CasResult<Cas2ApplicationEntity> {
    val applicationId = submitCas2v2Application.applicationId

    lockableApplicationRepository.acquirePessimisticLock(applicationId)

    var application = applicationRepository.findByIdAndServiceOrigin(applicationId, Cas2ServiceOrigin.BAIL)
      ?: return CasResult.NotFound("Cas2ApplicationEntity", applicationId.toString())

    val serializedTranslatedDocument = objectMapper.writeValueAsString(submitCas2v2Application.translatedDocument)

    if (!application.isCreatedBy(user)) {
      return CasResult.Unauthorised()
    }

    if (application.abandonedAt != null) {
      return CasResult.GeneralValidationError("This application has already been abandoned")
    }

    if (application.submittedAt != null) {
      return CasResult.GeneralValidationError("This application has already been submitted")
    }

    val validationErrors = ValidationErrors()
    val applicationData = application.data

    if (applicationData == null) {
      validationErrors["$.data"] = "empty"
    }

    if (validationErrors.any()) {
      return CasResult.FieldValidationError(validationErrors)
    }

    var prisonCode: String? = null
    if (application.nomsNumber != null) {
      try {
        prisonCode = retrievePrisonCode(application)
      } catch (e: UpstreamApiException) {
        sentryService.captureException(e)
      }
    }

    try {
      application.apply {
        submittedAt = OffsetDateTime.now()
        document = serializedTranslatedDocument
        referringPrisonCode = prisonCode
        preferredAreas = submitCas2v2Application.preferredAreas
        hdcEligibilityDate = submitCas2v2Application.hdcEligibilityDate
        conditionalReleaseDate = submitCas2v2Application.conditionalReleaseDate
        telephoneNumber = submitCas2v2Application.telephoneNumber
        bailHearingDate = submitCas2v2Application.bailHearingDate
        applicationOrigin = submitCas2v2Application.applicationOrigin
      }
    } catch (error: UpstreamApiException) {
      return CasResult.GeneralValidationError(error.message.toString())
    }

    application = applicationRepository.save(application)

    createCas2ApplicationSubmittedEvent(application)

    createAssessment(application)

    sendEmailApplicationSubmitted(user, application)

    return CasResult.Success(application)
  }

  fun getApplicationSummaries(
    user: Cas2UserEntity,
    pageCriteria: PageCriteria<String>,
    assignmentType: AssignmentType,
  ): Pair<MutableList<Cas2ApplicationSummaryEntity>, PaginationMetadata?> {
    val response = when (assignmentType) {
      AssignmentType.UNALLOCATED -> applicationSummaryRepository.findUnallocatedApplicationsInSamePrisonAsUser(
        user.activeNomisCaseloadId!!,
        getPageableOrAllPages(pageCriteria),
        Cas2ServiceOrigin.HDC.toString(),
      )

      AssignmentType.IN_PROGRESS -> applicationSummaryRepository.findInProgressApplications(
        user.id.toString(),
        getPageableOrAllPages(pageCriteria),
        Cas2ServiceOrigin.HDC.toString(),
      )

      AssignmentType.PRISON -> {
        applicationSummaryRepository.findAllocatedApplicationsInSamePrisonAsUser(
          user.activeNomisCaseloadId!!,
          getPageableOrAllPages(pageCriteria),
          Cas2ServiceOrigin.HDC.toString(),
        )
      }

      AssignmentType.ALLOCATED -> {
        applicationSummaryRepository.findApplicationsAssignedToUser(
          user.id,
          getPageableOrAllPages(pageCriteria),
          Cas2ServiceOrigin.HDC.toString(),
        )
      }

      AssignmentType.DEALLOCATED -> {
        val deallocatedApplicationIds =
          applicationRepository.findPreviouslyAssignedApplicationsInDifferentPrisonToUser(user.id, user.activeNomisCaseloadId!!, Cas2ServiceOrigin.HDC.toString())
        applicationSummaryRepository.findAllByServiceOriginAndIdIn(
          Cas2ServiceOrigin.HDC.toString(),
          deallocatedApplicationIds,
          getPageableOrAllPages(pageCriteria),
        )
      }
    }

    val metadata = getMetadata(response, pageCriteria)
    return Pair(response.content, metadata)
  }

  fun getSubmittedApplicationsByCrn(crn: String): List<Cas2ApplicationEntity> = applicationRepository.findAllByCrnAndSubmittedAtIsNotNullAndAssessmentIdIsNotNull(crn)

  fun findMostRecentApplication(nomsNumber: String): Cas2ApplicationEntity? = applicationRepository.findFirstByNomsNumberAndServiceOriginAndSubmittedAtIsNotNullOrderBySubmittedAtDesc(nomsNumber, Cas2ServiceOrigin.HDC)

  fun findApplicationToAssign(nomsNumber: String): Cas2ApplicationEntity? = findMostRecentApplication(nomsNumber)?.takeIf { !it.isMostRecentStatusUpdateANonAssignableStatus() }

  fun getAllSubmittedApplicationsForAssessor(pageCriteria: PageCriteria<String>): Pair<List<Cas2ApplicationSummaryEntity>, PaginationMetadata?> {
    val pageable = getPageableOrAllPages(pageCriteria)

    val response = applicationSummaryRepository.findByServiceOriginAndSubmittedAtIsNotNull(Cas2ServiceOrigin.HDC.toString(), pageable)

    val metadata = getMetadata(response, pageCriteria)

    return Pair(response.content, metadata)
  }

  fun getSubmittedApplicationForAssessor(applicationId: UUID): CasResult<Cas2ApplicationEntity> {
    val applicationEntity = applicationRepository.findByIdAndServiceOriginAndSubmittedAtIsNotNull(applicationId, Cas2ServiceOrigin.HDC)
      ?: return CasResult.NotFound("Application", applicationId.toString())

    return CasResult.Success(applicationEntity)
  }

  fun getApplicationForUser(applicationId: UUID, user: Cas2UserEntity): CasResult<Cas2ApplicationEntity> {
    val applicationEntity = applicationRepository.findByIdAndServiceOrigin(applicationId, Cas2ServiceOrigin.HDC)

    if (applicationEntity == null || applicationEntity.abandonedAt != null) {
      return CasResult.NotFound("Application", applicationId.toString())
    }

    val canAccess = userAccessService.userCanViewApplication(user, applicationEntity)

    return if (canAccess) {
      CasResult.Success(
        applicationEntity,
      )
    } else {
      CasResult.Unauthorised()
    }
  }

  fun createApplication(
    personInfoResult: PersonInfoResult.Success.Full,
    user: Cas2UserEntity,
  ): CasResult<Cas2ApplicationEntity> {
    val createdApplication = applicationRepository.save(
      Cas2ApplicationEntity(
        id = UUID.randomUUID(),
        crn = personInfoResult.crn,
        createdByUser = user,
        data = null,
        document = null,
        createdAt = OffsetDateTime.now(),
        submittedAt = null,
        nomsNumber = personInfoResult.offenderDetailSummary.otherIds.nomsNumber!!,
        telephoneNumber = null,
        applicationOrigin = ApplicationOrigin.homeDetentionCurfew,
        serviceOrigin = Cas2ServiceOrigin.HDC,
      ),
    )

    return CasResult.Success(createdApplication)
  }

  @SuppressWarnings("ReturnCount")
  fun updateApplication(applicationId: UUID, data: String?, user: Cas2UserEntity): CasResult<Cas2ApplicationEntity> {
    val application = applicationRepository.findByIdAndServiceOrigin(applicationId, Cas2ServiceOrigin.HDC)
      ?: return CasResult.NotFound("Application", applicationId.toString())

    if (!application.isCreatedBy(user)) {
      return CasResult.Unauthorised()
    }

    if (application.submittedAt != null) {
      return CasResult.GeneralValidationError("This application has already been submitted")
    }

    if (application.abandonedAt != null) {
      return CasResult.GeneralValidationError("This application has been abandoned")
    }

    application.apply {
      this.data = removeXssCharacters(data)
    }

    val savedApplication = applicationRepository.save(application)

    return CasResult.Success(savedApplication)
  }

  @SuppressWarnings("ReturnCount")
  fun abandonApplication(applicationId: UUID, user: Cas2UserEntity): CasResult<Cas2ApplicationEntity> {
    val application = applicationRepository.findByIdAndServiceOrigin(applicationId, Cas2ServiceOrigin.HDC)
      ?: return CasResult.NotFound("Application", applicationId.toString())

    if (!application.isCreatedBy(user)) {
      return CasResult.Unauthorised()
    }

    if (application.submittedAt != null) {
      return CasResult.ConflictError(applicationId, "This application has already been submitted")
    }

    if (application.abandonedAt != null) {
      return CasResult.Success(application)
    }

    application.apply {
      this.abandonedAt = OffsetDateTime.now()
      this.data = null
    }

    val savedApplication = applicationRepository.save(application)

    return CasResult.Success(savedApplication)
  }

  @SuppressWarnings("ReturnCount")
  @Transactional
  fun submitApplication(
    submitApplication: SubmitCas2Application,
    user: Cas2UserEntity,
  ): CasResult<Cas2ApplicationEntity> {
    val applicationId = submitApplication.applicationId

    lockableApplicationRepository.acquirePessimisticLock(applicationId)

    var application = applicationRepository.findByIdAndServiceOrigin(applicationId, Cas2ServiceOrigin.HDC)
      ?: return CasResult.NotFound("Application", applicationId.toString())

    val serializedTranslatedDocument = objectMapper.writeValueAsString(submitApplication.translatedDocument)

    if (!application.isCreatedBy(user)) {
      return CasResult.Unauthorised()
    }

    if (application.abandonedAt != null) {
      return CasResult.GeneralValidationError("This application has already been abandoned")
    }

    if (application.submittedAt != null) {
      return CasResult.GeneralValidationError("This application has already been submitted")
    }

    val validationErrors = ValidationErrors()
    val applicationData = application.data

    if (applicationData == null) {
      validationErrors["$.data"] = "empty"
    }

    if (validationErrors.any()) {
      return CasResult.FieldValidationError(validationErrors)
    }

    try {
      application.apply {
        val prisonCode = retrievePrisonCode(application)
        submittedAt = OffsetDateTime.now()
        document = serializedTranslatedDocument
        referringPrisonCode = prisonCode
        preferredAreas = submitApplication.preferredAreas
        hdcEligibilityDate = submitApplication.hdcEligibilityDate
        conditionalReleaseDate = submitApplication.conditionalReleaseDate
        telephoneNumber = submitApplication.telephoneNumber
        this.createApplicationAssignment(prisonCode = prisonCode, allocatedPomUser = user)
      }
    } catch (error: UpstreamApiException) {
      return CasResult.GeneralValidationError(error.message.toString())
    }

    application = applicationRepository.save(application)

    createCas2ApplicationSubmittedEvent(application)

    createAssessment(application)

    sendEmailApplicationSubmitted(user, application)

    return CasResult.Success(application)
  }

  fun createCas2ApplicationSubmittedEvent(application: Cas2ApplicationEntity) {
    val domainEventId = UUID.randomUUID()
    val eventOccurredAt = application.submittedAt ?: OffsetDateTime.now()

    val (applicationUrl, staffMember) = when (application.serviceOrigin) {
      Cas2ServiceOrigin.HDC -> applicationUrlTemplate to Cas2StaffMember(
        staffIdentifier = application.createdByUser.nomisStaffId!!,
        cas2StaffIdentifier = application.createdByUser.staffIdentifier(),
        name = application.createdByUser.name,
        username = application.createdByUser.username,
        usertype = application.getCreatedByUserType(),
      )
      Cas2ServiceOrigin.BAIL -> cas2v2ApplicationUrlTemplate to Cas2StaffMember(
        staffIdentifier = application.createdByUser.nomisStaffId ?: 0L,
        name = application.createdByUser.name,
        username = application.createdByUser.username,
        usertype = runCatching {
          Cas2StaffMember.Usertype.forValue(application.createdByUser.userType.authSource)
        }.getOrNull(),
      )
    }

    domainEventService.saveCas2ApplicationSubmittedDomainEvent(
      DomainEvent(
        id = domainEventId,
        applicationId = application.id,
        crn = application.crn,
        nomsNumber = application.nomsNumber,
        occurredAt = eventOccurredAt.toInstant(),
        data = Cas2ApplicationSubmittedEvent(
          id = domainEventId,
          timestamp = eventOccurredAt.toInstant(),
          eventType = EventType.applicationSubmitted,
          eventDetails = Cas2ApplicationSubmittedEventDetails(
            applicationId = application.id,
            applicationUrl = applicationUrl.replace("#id", application.id.toString()),
            personReference = PersonReference(
              noms = application.nomsNumber ?: "Unknown NOMS Number",
              crn = application.crn,
            ),
            submittedAt = eventOccurredAt.toInstant(),
            submittedBy = Cas2ApplicationSubmittedEventDetailsSubmittedBy(
              staffMember = staffMember,
            ),
            applicationOrigin = application.applicationOrigin.toString(),
            bailHearingDate = application.bailHearingDate,
            referringPrisonCode = application.referringPrisonCode,
            preferredAreas = application.preferredAreas,
            hdcEligibilityDate = application.hdcEligibilityDate,
            conditionalReleaseDate = application.conditionalReleaseDate,
          ),
        ),
      ),
    )
  }

  fun createAssessment(application: Cas2ApplicationEntity) {
    assessmentService.createCas2Assessment(application)
  }

  @SuppressWarnings("ThrowsCount")
  private fun retrievePrisonCode(application: Cas2ApplicationEntity): String {
    val inmateDetailResult = offenderService.getInmateDetailByNomsNumber(
      crn = application.crn,
      nomsNumber = application.nomsNumber.toString(),
    )
    val inmateDetail = when (inmateDetailResult) {
      is AuthorisableActionResult.NotFound -> throw UpstreamApiException("Inmate Detail not found")
      is AuthorisableActionResult.Unauthorised -> throw UpstreamApiException("Inmate Detail unauthorised")
      is AuthorisableActionResult.Success -> inmateDetailResult.entity
    }

    return inmateDetail?.assignedLivingUnit?.agencyId ?: throw UpstreamApiException("No prison code available")
  }

  private fun sendEmailApplicationSubmitted(user: Cas2UserEntity, application: Cas2ApplicationEntity) {
    when (application.serviceOrigin) {
      Cas2ServiceOrigin.HDC -> sendEmailApplicationSubmittedForHdc(user, application)
      Cas2ServiceOrigin.BAIL -> sendEmailApplicationSubmittedForBail(user, application)
    }
  }

  private fun sendEmailApplicationSubmittedForHdc(user: Cas2UserEntity, application: Cas2ApplicationEntity) {
    emailNotificationService.sendEmail(
      recipientEmailAddress = notifyConfig.emailAddresses.cas2Assessors,
      templateId = Cas2NotifyTemplates.CAS2_APPLICATION_SUBMITTED,
      personalisation = mapOf(
        "name" to user.name,
        "email" to user.email,
        "prisonNumber" to application.nomsNumber,
        "telephoneNumber" to application.telephoneNumber,
        "applicationUrl" to submittedApplicationUrlTemplate.replace("#applicationId", application.id.toString()),
      ),
      replyToEmailId = notifyConfig.emailAddresses.cas2ReplyToId,
    )
  }

  private fun sendEmailApplicationSubmittedForBail(user: Cas2UserEntity, application: Cas2ApplicationEntity) {
    val applicationOrigin = application.applicationOrigin.toString()

    val templateId = when (applicationOrigin) {
      ApplicationOrigin.courtBail.toString() -> Cas2NotifyTemplates.CAS2_V2_APPLICATION_SUBMITTED_COURT_BAIL
      ApplicationOrigin.prisonBail.toString() -> Cas2NotifyTemplates.CAS2_V2_APPLICATION_SUBMITTED_PRISON_BAIL
      else -> Cas2NotifyTemplates.CAS2_APPLICATION_SUBMITTED
    }

    emailNotificationService.sendEmail(
      recipientEmailAddress = notifyConfig.emailAddresses.cas2Assessors,
      templateId = templateId,
      personalisation = mapOf(
        "name" to user.name,
        "email" to user.email,
        "prisonNumber" to application.nomsNumber,
        "nomsNumber" to application.nomsNumber,
        "crn" to application.crn,
        "telephoneNumber" to application.telephoneNumber,
        "timeApplicationSubmitted" to (application.submittedAt?.format(DateTimeFormatter.ofPattern("HH:mm")) ?: ""),
        "dateApplicationSubmitted" to (application.submittedAt?.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) ?: ""),
        "referrerName" to application.createdByUser.name,
        "referrerEmail" to application.createdByUser.email,
        "referrerTelephoneNumber" to application.telephoneNumber,
        "applicationUrl" to cas2v2SubmittedApplicationUrlTemplate.replace("#applicationId", application.id.toString()),
      ),
      replyToEmailId = notifyConfig.emailAddresses.cas2ReplyToId,
    )
  }

  private fun removeXssCharacters(data: String?): String? {
    if (data != null) {
      val xssCharacters = setOf('<', '＜', '〈', '〈', '>', '＞', '〉', '〉')
      var sanitisedData = data
      xssCharacters.forEach { character ->
        sanitisedData = sanitisedData?.replace(character.toString(), "")
      }
      return sanitisedData
    }
    return null
  }
}
