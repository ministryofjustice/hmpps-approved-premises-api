package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.service

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.transaction.Transactional
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas2.model.Cas2ApplicationSubmittedEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas2.model.Cas2ApplicationSubmittedEventDetails
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas2.model.Cas2ApplicationSubmittedEventDetailsSubmittedBy
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas2.model.Cas2StaffMember
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas2.model.EventType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas2.model.PersonReference
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApplicationOrigin
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.AssignmentType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.ApplicationSummaryRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.Cas2ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.Cas2ApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.Cas2ApplicationSummaryEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.Cas2LockableApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.Cas2UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.NomisUserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model.Cas2ServiceOrigin
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model.SubmitCas2Application
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.Cas2NotifyTemplates
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.NotifyConfig
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.DomainEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PaginationMetadata
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonInfoResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.ValidationErrors
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.AuthorisableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.CasResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.EmailNotificationService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UpstreamApiException
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.PageCriteria
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.getMetadata
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.getPageableOrAllPages
import java.time.OffsetDateTime
import java.util.UUID

@SuppressWarnings("TooGenericExceptionThrown")
@Service
class Cas2ApplicationService(
  private val applicationRepository: Cas2ApplicationRepository,
  private val lockableApplicationRepository: Cas2LockableApplicationRepository,
  private val applicationSummaryRepository: ApplicationSummaryRepository,
  private val offenderService: Cas2OffenderService,
  private val userAccessService: Cas2UserAccessService,
  private val domainEventService: Cas2DomainEventService,
  private val emailNotificationService: EmailNotificationService,
  private val assessmentService: Cas2AssessmentService,
  private val notifyConfig: NotifyConfig,
  private val objectMapper: ObjectMapper,
  @Value("\${url-templates.frontend.cas2.application}") private val applicationUrlTemplate: String,
  @Value("\${url-templates.frontend.cas2.submitted-application-overview}") private val submittedApplicationUrlTemplate: String,
) {

  fun getApplicationSummaries(
    user: Cas2UserEntity,
    pageCriteria: PageCriteria<String>,
    assignmentType: AssignmentType,
  ): Pair<MutableList<Cas2ApplicationSummaryEntity>, PaginationMetadata?> {
    val response = when (assignmentType) {
      AssignmentType.UNALLOCATED -> applicationSummaryRepository.findUnallocatedApplicationsInSamePrisonAsUser(
        user.activeNomisCaseloadId!!,
        getPageableOrAllPages(pageCriteria),
      )

      AssignmentType.IN_PROGRESS -> applicationSummaryRepository.findInProgressApplications(
        user.id.toString(),
        getPageableOrAllPages(pageCriteria),
      )

      AssignmentType.PRISON -> {
        applicationSummaryRepository.findAllocatedApplicationsInSamePrisonAsUser(
          user.activeNomisCaseloadId!!,
          getPageableOrAllPages(pageCriteria),
        )
      }

      AssignmentType.ALLOCATED -> {
        applicationSummaryRepository.findApplicationsAssignedToUser(
          user.id,
          getPageableOrAllPages(pageCriteria),
        )
      }

      AssignmentType.DEALLOCATED -> {
        val deallocatedApplicationIds =
          applicationRepository.findPreviouslyAssignedApplicationsInDifferentPrisonToUser(user.id, user.activeNomisCaseloadId!!)
        applicationSummaryRepository.findAllByIdIn(
          deallocatedApplicationIds,
          getPageableOrAllPages(pageCriteria),
        )
      }
    }

    val metadata = getMetadata(response, pageCriteria)
    return Pair(response.content, metadata)
  }

  fun findMostRecentApplication(nomsNumber: String): Cas2ApplicationEntity? = applicationRepository.findFirstByNomsNumberAndSubmittedAtIsNotNullOrderBySubmittedAtDesc(nomsNumber)

  fun findApplicationToAssign(nomsNumber: String): Cas2ApplicationEntity? = findMostRecentApplication(nomsNumber)?.takeIf { !it.isMostRecentStatusUpdateANonAssignableStatus() }

  fun getAllSubmittedApplicationsForAssessor(pageCriteria: PageCriteria<String>): Pair<List<Cas2ApplicationSummaryEntity>, PaginationMetadata?> {
    val pageable = getPageableOrAllPages(pageCriteria)

    val response = applicationSummaryRepository.findBySubmittedAtIsNotNull(pageable)

    val metadata = getMetadata(response, pageCriteria)

    return Pair(response.content, metadata)
  }

  fun getSubmittedApplicationForAssessor(applicationId: UUID): CasResult<Cas2ApplicationEntity> {
    val applicationEntity = applicationRepository.findSubmittedApplicationById(applicationId)
      ?: return CasResult.NotFound("Application", applicationId.toString())

    return CasResult.Success(applicationEntity)
  }

  fun getApplicationForUser(applicationId: UUID, user: Cas2UserEntity): CasResult<Cas2ApplicationEntity> {
    val applicationEntity = applicationRepository.findByIdOrNull(applicationId)

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
    val application = applicationRepository.findByIdOrNull(applicationId)
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
    val application = applicationRepository.findByIdOrNull(applicationId)
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

    var application = applicationRepository.findByIdOrNull(applicationId)
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
            applicationUrl = applicationUrlTemplate
              .replace("#id", application.id.toString()),
            submittedAt = eventOccurredAt.toInstant(),
            personReference = PersonReference(
              noms = application.nomsNumber ?: "Unknown NOMS Number",
              crn = application.crn,
            ),
            referringPrisonCode = application.referringPrisonCode,
            preferredAreas = application.preferredAreas,
            hdcEligibilityDate = application.hdcEligibilityDate,
            conditionalReleaseDate = application.conditionalReleaseDate,
            submittedBy = Cas2ApplicationSubmittedEventDetailsSubmittedBy(
              staffMember = Cas2StaffMember(
                staffIdentifier = application.createdByUser!!.nomisStaffId!!,
                cas2StaffIdentifier = application.createdByUser!!.staffIdentifier(),
                name = application.createdByUser!!.name,
                username = application.createdByUser!!.username,
                usertype = application.getCreatedByUserType(),
              ),
            ),
            applicationOrigin = ApplicationOrigin.homeDetentionCurfew.toString(),
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
    emailNotificationService.sendEmail(
      recipientEmailAddress = notifyConfig.emailAddresses.cas2Assessors,
      templateId = Cas2NotifyTemplates.cas2ApplicationSubmitted,
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
