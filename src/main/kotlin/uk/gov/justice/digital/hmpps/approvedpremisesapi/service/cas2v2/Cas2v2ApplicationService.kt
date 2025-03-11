package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas2v2

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
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SubmitCas2v2Application
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.NotifyConfig
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2v2ApplicationJsonSchemaEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas2v2.Cas2v2ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas2v2.Cas2v2ApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas2v2.Cas2v2ApplicationSummaryEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas2v2.Cas2v2ApplicationSummaryRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas2v2.Cas2v2LockableApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas2v2.Cas2v2UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.DomainEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PaginationMetadata
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.ValidationErrors
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.validated
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.AuthorisableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.CasResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.EmailNotificationService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UpstreamApiException
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas2.Cas2DomainEventService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas2.Cas2OffenderService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.PageCriteria
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.getMetadata
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.getPageableOrAllPages
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

@Service("Cas2v2ApplicationService")
class Cas2v2ApplicationService(
  private val cas2v2ApplicationRepository: Cas2v2ApplicationRepository,
  private val cas2v2LockableApplicationRepository: Cas2v2LockableApplicationRepository,
  private val cas2v2ApplicationSummaryRepository: Cas2v2ApplicationSummaryRepository,
  private val cas2v2JsonSchemaService: Cas2v2JsonSchemaService,
  private val cas2OffenderService: Cas2OffenderService,
  private val cas2v2UserAccessService: Cas2v2UserAccessService,
  private val domainEventService: Cas2DomainEventService,
  private val emailNotificationService: EmailNotificationService,
  private val cas2v2AssessmentService: Cas2v2AssessmentService,
  private val notifyConfig: NotifyConfig,
  private val objectMapper: ObjectMapper,
  @Value("\${url-templates.frontend.cas2v2.application}") private val applicationUrlTemplate: String,
  @Value("\${url-templates.frontend.cas2v2.submitted-application-overview}") private val submittedApplicationUrlTemplate: String,
) {

  val repositoryUserFunctionMap = mapOf(
    null to cas2v2ApplicationSummaryRepository::findByUserId,
    true to cas2v2ApplicationSummaryRepository::findByUserIdAndSubmittedAtIsNotNull,
    false to cas2v2ApplicationSummaryRepository::findByUserIdAndSubmittedAtIsNull,
  )

  val repositoryPrisonFunctionMap = mapOf(
    null to cas2v2ApplicationSummaryRepository::findByPrisonCode,
    true to cas2v2ApplicationSummaryRepository::findByPrisonCodeAndSubmittedAtIsNotNull,
    false to cas2v2ApplicationSummaryRepository::findByPrisonCodeAndSubmittedAtIsNull,
  )

  fun getCas2v2Applications(
    prisonCode: String?,
    isSubmitted: Boolean?,
    user: Cas2v2UserEntity,
    pageCriteria: PageCriteria<String>,
  ): Pair<MutableList<Cas2v2ApplicationSummaryEntity>, PaginationMetadata?> {
    val response = if (prisonCode == null) {
      repositoryUserFunctionMap.get(isSubmitted)!!(user.id.toString(), getPageableOrAllPages(pageCriteria))
    } else {
      repositoryPrisonFunctionMap.get(isSubmitted)!!(prisonCode, getPageableOrAllPages(pageCriteria))
    }
    val metadata = getMetadata(response, pageCriteria)
    return Pair(response.content, metadata)
  }

  fun getAllSubmittedCas2v2ApplicationsForAssessor(pageCriteria: PageCriteria<String>): Pair<List<Cas2v2ApplicationSummaryEntity>, PaginationMetadata?> {
    val pageable = getPageableOrAllPages(pageCriteria)

    val response = cas2v2ApplicationSummaryRepository.findBySubmittedAtIsNotNull(pageable)

    val metadata = getMetadata(response, pageCriteria)

    return Pair(response.content, metadata)
  }

  fun getSubmittedCas2v2ApplicationForAssessor(applicationId: UUID): CasResult<Cas2v2ApplicationEntity> {
    val applicationEntity = cas2v2ApplicationRepository.findSubmittedApplicationById(applicationId)
      ?: return CasResult.NotFound("Cas2v2ApplicationEntity", applicationId.toString())

    return CasResult.Success(
      cas2v2JsonSchemaService.checkCas2v2SchemaOutdated(applicationEntity),
    )
  }

  fun getCas2v2ApplicationForUser(applicationId: UUID, user: Cas2v2UserEntity): CasResult<Cas2v2ApplicationEntity> {
    val applicationEntity = cas2v2ApplicationRepository.findByIdOrNull(applicationId)
      ?: return CasResult.NotFound("Cas2v2ApplicationEntity", applicationId.toString())

    if (applicationEntity.abandonedAt != null) {
      return CasResult.NotFound("Cas2v2ApplicationEntity", applicationId.toString())
    }

    val canAccess = cas2v2UserAccessService.userCanViewCas2v2Application(user, applicationEntity)

    return if (canAccess) {
      CasResult.Success(cas2v2JsonSchemaService.checkCas2v2SchemaOutdated(applicationEntity))
    } else {
      CasResult.Unauthorised()
    }
  }

  @SuppressWarnings("TooGenericExceptionThrown")
  fun createCas2v2Application(
    crn: String,
    user: Cas2v2UserEntity,
    applicationOrigin: ApplicationOrigin = ApplicationOrigin.homeDetentionCurfew,
    bailHearingDate: LocalDate? = null,
  ) = validated<Cas2v2ApplicationEntity> {
    val offenderDetailsResult = cas2OffenderService.getOffenderByCrnDeprecated(crn)

    val offenderDetails = when (offenderDetailsResult) {
      is AuthorisableActionResult.NotFound -> return "$.crn" hasSingleValidationError "doesNotExist"
      is AuthorisableActionResult.Unauthorised -> return "$.crn" hasSingleValidationError "userPermission"
      is AuthorisableActionResult.Success -> offenderDetailsResult.entity
    }

    if (offenderDetails.otherIds.nomsNumber == null) {
      throw RuntimeException("Cannot create an Application for an Offender without a NOMS number")
    }

    if (validationErrors.any()) {
      return fieldValidationError
    }

    val id = UUID.randomUUID()

    val entityToSave = Cas2v2ApplicationEntity(
      id = id,
      crn = crn,
      createdByUser = user,
      data = null,
      document = null,
      schemaVersion = cas2v2JsonSchemaService.getNewestSchema(Cas2v2ApplicationJsonSchemaEntity::class.java),
      createdAt = OffsetDateTime.now(),
      submittedAt = null,
      schemaUpToDate = true,
      nomsNumber = offenderDetails.otherIds.nomsNumber,
      telephoneNumber = null,
      applicationOrigin = applicationOrigin,
      bailHearingDate = bailHearingDate,
    )

    val createdApplication = cas2v2ApplicationRepository.save(
      entityToSave,
    )

    return success(createdApplication.apply { schemaUpToDate = true })
  }

  @SuppressWarnings("ReturnCount")
  fun updateCas2v2Application(
    applicationId: UUID,
    data: String?,
    user: Cas2v2UserEntity,
    bailHearingDate: LocalDate?,
  ): CasResult<Cas2v2ApplicationEntity> {
    val application = cas2v2ApplicationRepository.findByIdOrNull(applicationId)?.let(cas2v2JsonSchemaService::checkCas2v2SchemaOutdated)
      ?: return CasResult.NotFound("Cas2v2ApplicationEntity", applicationId.toString())

    if (application.createdByUser != user) {
      return CasResult.Unauthorised()
    }

    if (application.submittedAt != null) {
      return CasResult.GeneralValidationError("This application has already been submitted")
    }

    if (application.abandonedAt != null) {
      return CasResult.GeneralValidationError("This application has been abandoned")
    }

    if (!application.schemaUpToDate) {
      return CasResult.GeneralValidationError("The schema version is outdated")
    }

    application.bailHearingDate = bailHearingDate

    application.apply {
      this.data = removeXssCharacters(data)
    }

    val savedApplication = cas2v2ApplicationRepository.save(application)

    return CasResult.Success(savedApplication)
  }

  @SuppressWarnings("ReturnCount")
  fun abandonCas2v2Application(applicationId: UUID, user: Cas2v2UserEntity): CasResult<Cas2v2ApplicationEntity> {
    val application = cas2v2ApplicationRepository.findByIdOrNull(applicationId)
      ?: return CasResult.NotFound("Cas2v2ApplicationEntity", applicationId.toString())

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

    val savedApplication = cas2v2ApplicationRepository.save(application)

    return CasResult.Success(savedApplication)
  }

  @SuppressWarnings("ReturnCount", "TooGenericExceptionThrown")
  @Transactional
  fun submitCas2v2Application(
    submitCas2v2Application: SubmitCas2v2Application,
    user: Cas2v2UserEntity,
  ): CasResult<Cas2v2ApplicationEntity> {
    val applicationId = submitCas2v2Application.applicationId

    cas2v2LockableApplicationRepository.acquirePessimisticLock(applicationId)

    var application = cas2v2ApplicationRepository.findByIdOrNull(applicationId)
      ?.let(cas2v2JsonSchemaService::checkCas2v2SchemaOutdated)
      ?: return CasResult.NotFound("Cas2v2ApplicationEntity", applicationId.toString())

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

    if (!application.schemaUpToDate) {
      return CasResult.GeneralValidationError("The schema version is outdated")
    }

    val validationErrors = ValidationErrors()
    val applicationData = application.data

    if (applicationData == null) {
      validationErrors["$.data"] = "empty"
    } else if (!cas2v2JsonSchemaService.validate(application.schemaVersion, applicationData)) {
      validationErrors["$.data"] = "invalid"
    }

    if (validationErrors.any()) {
      return CasResult.FieldValidationError(validationErrors)
    }

    application.schemaVersion as? Cas2v2ApplicationJsonSchemaEntity
      ?: throw RuntimeException("Incorrect type of JSON schema referenced by CAS2 v2 Application")

    try {
      application.apply {
        submittedAt = OffsetDateTime.now()
        document = serializedTranslatedDocument
        referringPrisonCode = cas2OffenderService.findPrisonCode(application.crn, application.nomsNumber!!)
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

    application = cas2v2ApplicationRepository.save(application)

    createCas2ApplicationSubmittedEvent(application)

    createAssessment(application)

    sendEmailApplicationSubmitted(user, application)

    return CasResult.Success(application)
  }

  fun createCas2ApplicationSubmittedEvent(application: Cas2v2ApplicationEntity) {
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

  fun createAssessment(application: Cas2v2ApplicationEntity) {
    cas2v2AssessmentService.createCas2v2Assessment(application)
  }

  private fun sendEmailApplicationSubmitted(user: Cas2v2UserEntity, application: Cas2v2ApplicationEntity) {
    val applicationOrigin = application.applicationOrigin.toString()

    val templateId = when (applicationOrigin) {
      ApplicationOrigin.courtBail.toString() -> notifyConfig.templates.cas2v2ApplicationSubmittedCourtBail
      ApplicationOrigin.prisonBail.toString() -> notifyConfig.templates.cas2v2ApplicationSubmittedPrisonBail
      else -> notifyConfig.templates.cas2ApplicationSubmitted
    }

    emailNotificationService.sendEmail(
      recipientEmailAddress = notifyConfig.emailAddresses.cas2Assessors,
      templateId = templateId,
      personalisation = mapOf(
        "name" to user.name,
        "email" to user.email,
        "prisonNumber" to application.nomsNumber,
        "crn" to application.crn,
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
