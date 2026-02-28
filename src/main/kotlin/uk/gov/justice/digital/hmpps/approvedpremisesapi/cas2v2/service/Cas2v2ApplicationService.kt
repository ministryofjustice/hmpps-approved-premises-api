package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2v2.service

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
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SubmitCas2v2Application
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.Cas2ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.Cas2ApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.Cas2ApplicationSummaryEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.Cas2ApplicationSummaryRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.Cas2LockableApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.Cas2UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model.Cas2ServiceOrigin
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.service.Cas2DomainEventService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.service.Cas2UserAccessService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2v2.jpa.entity.Cas2v2ApplicationSummarySpecifications
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.Cas2NotifyTemplates
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.NotifyConfig
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.DomainEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PaginationMetadata
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

@Service("Cas2v2ApplicationService")
class Cas2v2ApplicationService(
  private val cas2ApplicationRepository: Cas2ApplicationRepository,
  private val cas2LockableApplicationRepository: Cas2LockableApplicationRepository,
  private val cas2ApplicationSummaryRepository: Cas2ApplicationSummaryRepository,
  private val cas2v2OffenderService: Cas2v2OffenderService,
  private val cas2UserAccessService: Cas2UserAccessService,
  private val domainEventService: Cas2DomainEventService,
  private val emailNotificationService: EmailNotificationService,
  private val cas2v2AssessmentService: Cas2v2AssessmentService,
  private val notifyConfig: NotifyConfig,
  private val objectMapper: ObjectMapper,
  private val sentryService: SentryService,
  @Value("\${url-templates.frontend.cas2v2.application}") private val applicationUrlTemplate: String,
  @Value("\${url-templates.frontend.cas2v2.submitted-application-overview}") private val submittedApplicationUrlTemplate: String,
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
      Specification { _, _, cb -> cb.conjunction() } // Start with no-op

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

    val response = cas2ApplicationSummaryRepository.findAll(spec, getPageableOrAllPages(pageCriteria))

    val metadata = getMetadata(response, pageCriteria)
    return Pair(response.content, metadata)
  }

  fun getAllSubmittedCas2v2ApplicationsForAssessor(pageCriteria: PageCriteria<String>): Pair<List<Cas2ApplicationSummaryEntity>, PaginationMetadata?> {
    val pageable = getPageableOrAllPages(pageCriteria)

    val response = cas2ApplicationSummaryRepository.findByServiceOriginAndSubmittedAtIsNotNull(Cas2ServiceOrigin.BAIL.toString(), pageable)

    val metadata = getMetadata(response, pageCriteria)

    return Pair(response.content, metadata)
  }

  fun getSubmittedCas2v2ApplicationForAssessor(applicationId: UUID): CasResult<Cas2ApplicationEntity> {
    val applicationEntity = cas2ApplicationRepository.findByIdAndServiceOriginAndSubmittedAtIsNotNull(applicationId, Cas2ServiceOrigin.BAIL)
      ?: return CasResult.NotFound("Cas2ApplicationEntity", applicationId.toString())

    return CasResult.Success(
      applicationEntity,
    )
  }

  fun getCas2v2ApplicationForUser(applicationId: UUID, user: Cas2UserEntity): CasResult<Cas2ApplicationEntity> {
    val applicationEntity = cas2ApplicationRepository.findByIdAndServiceOrigin(applicationId, Cas2ServiceOrigin.BAIL)
      ?: return CasResult.NotFound("Cas2ApplicationEntity", applicationId.toString())

    if (applicationEntity.abandonedAt != null) {
      return CasResult.NotFound("Cas2ApplicationEntity", applicationId.toString())
    }

    val canAccess = cas2UserAccessService.userCanViewCas2v2Application(user, applicationEntity)

    return if (canAccess) {
      CasResult.Success(applicationEntity)
    } else {
      CasResult.Unauthorised()
    }
  }

  fun getSubmittedApplicationsByCrn(crn: String): List<Cas2ApplicationEntity> = cas2ApplicationRepository.findAllByCrnAndSubmittedAtIsNotNullAndAssessmentIdIsNotNull(crn)

  @SuppressWarnings("TooGenericExceptionThrown")
  fun createCas2v2Application(
    crn: String,
    user: Cas2UserEntity,
    applicationOrigin: ApplicationOrigin = ApplicationOrigin.homeDetentionCurfew,
    bailHearingDate: LocalDate? = null,
  ) = validated<Cas2ApplicationEntity> {
    val offenderDetailsResult = cas2v2OffenderService.getPersonByNomisIdOrCrn(crn)

    val offenderDetails = when (offenderDetailsResult) {
      is Cas2v2OffenderSearchResult.NotFound -> return "$.crn" hasSingleValidationError "doesNotExist"
      is Cas2v2OffenderSearchResult.Forbidden -> return "$.crn" hasSingleValidationError "userPermission"
      is Cas2v2OffenderSearchResult.Unknown -> return "$.crn" hasSingleValidationError "unknown"
      is Cas2v2OffenderSearchResult.Success.Full -> offenderDetailsResult.person
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

    val createdApplication = cas2ApplicationRepository.save(
      entityToSave,
    )

    return success(createdApplication)
  }

  @SuppressWarnings("ReturnCount")
  fun updateCas2v2Application(
    applicationId: UUID,
    data: String?,
    user: Cas2UserEntity,
    bailHearingDate: LocalDate?,
  ): CasResult<Cas2ApplicationEntity> {
    val application = cas2ApplicationRepository.findByIdAndServiceOrigin(applicationId, Cas2ServiceOrigin.BAIL)
      ?: return CasResult.NotFound("Cas2ApplicationEntity", applicationId.toString())

    if (application.createdByUser != user) {
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

    val savedApplication = cas2ApplicationRepository.save(application)

    return CasResult.Success(savedApplication)
  }

  @SuppressWarnings("ReturnCount")
  fun abandonCas2v2Application(applicationId: UUID, user: Cas2UserEntity): CasResult<Cas2ApplicationEntity> {
    val application = cas2ApplicationRepository.findByIdAndServiceOrigin(applicationId, Cas2ServiceOrigin.BAIL)
      ?: return CasResult.NotFound("Cas2ApplicationEntity", applicationId.toString())

    if (application.createdByUser != user) {
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

    val savedApplication = cas2ApplicationRepository.save(application)

    return CasResult.Success(savedApplication)
  }

  @SuppressWarnings("ReturnCount", "TooGenericExceptionThrown")
  @Transactional
  fun submitCas2v2Application(
    submitCas2v2Application: SubmitCas2v2Application,
    user: Cas2UserEntity,
  ): CasResult<Cas2ApplicationEntity> {
    val applicationId = submitCas2v2Application.applicationId

    cas2LockableApplicationRepository.acquirePessimisticLock(applicationId)

    var application = cas2ApplicationRepository.findByIdAndServiceOrigin(applicationId, Cas2ServiceOrigin.BAIL)
      ?: return CasResult.NotFound("Cas2ApplicationEntity", applicationId.toString())

    val serializedTranslatedDocument = objectMapper.writeValueAsString(submitCas2v2Application.translatedDocument)

    if (application.createdByUser != user) {
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

    application = cas2ApplicationRepository.save(application)

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
            bailHearingDate = application.bailHearingDate,
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
                staffIdentifier = application.createdByUser.nomisStaffId ?: 0,
                name = application.createdByUser.name,
                username = application.createdByUser.username,
                usertype = Cas2StaffMember.Usertype.valueOf(application.createdByUser.userType.authSource),
              ),
            ),
            applicationOrigin = application.applicationOrigin.toString(),
          ),
        ),
      ),
    )
  }

  fun createAssessment(application: Cas2ApplicationEntity) {
    cas2v2AssessmentService.createCas2v2Assessment(application)
  }

  @SuppressWarnings("ThrowsCount")
  private fun retrievePrisonCode(application: Cas2ApplicationEntity): String {
    val inmateDetailResult = cas2v2OffenderService.getInmateDetailByNomsNumber(
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
