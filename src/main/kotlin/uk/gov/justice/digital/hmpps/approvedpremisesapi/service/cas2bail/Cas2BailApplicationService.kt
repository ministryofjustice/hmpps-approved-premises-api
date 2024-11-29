package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas2bail

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.transaction.Transactional
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas2.model.*
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SubmitCas2Application
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.NotifyConfig
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.*
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas2bail.*
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.DomainEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PaginationMetadata
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.ValidationErrors
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.validated
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.AuthorisableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.ValidatableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.EmailNotificationService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UpstreamApiException
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas2.*
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.PageCriteria
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.getMetadata
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.getPageableOrAllPages
import java.time.OffsetDateTime
import java.util.*

@Service("Cas2BailApplicationService")
class Cas2BailApplicationService(
  private val cas2BailApplicationRepository: Cas2BailApplicationRepository,
  private val lockableApplicationRepository: Cas2BailLockableApplicationRepository,
  private val applicationSummaryRepository: Cas2BailApplicationSummaryRepository,
  private val jsonSchemaService: JsonSchemaService,
  private val offenderService: OffenderService,
  private val cas2BailUserAccessService: Cas2BailUserAccessService,
  private val domainEventService: DomainEventService,
  private val emailNotificationService: EmailNotificationService,
  private val assessmentService: Cas2BailAssessmentService,
  private val notifyConfig: NotifyConfig,
  private val objectMapper: ObjectMapper,
  @Value("\${url-templates.frontend.cas2.application}") private val applicationUrlTemplate: String,
  @Value("\${url-templates.frontend.cas2.submitted-application-overview}") private val submittedApplicationUrlTemplate: String,
) {

  val repositoryUserFunctionMap = mapOf(
    null to applicationSummaryRepository::findByUserId,
    true to applicationSummaryRepository::findByUserIdAndSubmittedAtIsNotNull,
    false to applicationSummaryRepository::findByUserIdAndSubmittedAtIsNull,
  )

  val repositoryPrisonFunctionMap = mapOf(
    null to applicationSummaryRepository::findByPrisonCode,
    true to applicationSummaryRepository::findByPrisonCodeAndSubmittedAtIsNotNull,
    false to applicationSummaryRepository::findByPrisonCodeAndSubmittedAtIsNull,
  )

  fun getCas2BailApplications(
    prisonCode: String?,
    isSubmitted: Boolean?,
    user: NomisUserEntity,
    pageCriteria: PageCriteria<String>,
  ): Pair<MutableList<Cas2BailApplicationSummaryEntity>, PaginationMetadata?> {
    val response = if (prisonCode == null) {
      repositoryUserFunctionMap.get(isSubmitted)!!(user.id.toString(), getPageableOrAllPages(pageCriteria))
    } else {
      repositoryPrisonFunctionMap.get(isSubmitted)!!(prisonCode, getPageableOrAllPages(pageCriteria))
    }
    val metadata = getMetadata(response, pageCriteria)
    return Pair(response.content, metadata)
  }

  fun getAllSubmittedCas2BailApplicationsForAssessor(pageCriteria: PageCriteria<String>): Pair<List<Cas2BailApplicationSummaryEntity>, PaginationMetadata?> {
    val pageable = getPageableOrAllPages(pageCriteria)

    val response = applicationSummaryRepository.findBySubmittedAtIsNotNull(pageable)

    val metadata = getMetadata(response, pageCriteria)

    return Pair(response.content, metadata)
  }

  fun getSubmittedCas2BailApplicationForAssessor(applicationId: UUID): AuthorisableActionResult<Cas2BailApplicationEntity> {
    val applicationEntity = cas2BailApplicationRepository.findSubmittedApplicationById(applicationId)
      ?: return AuthorisableActionResult.NotFound()

    return AuthorisableActionResult.Success(
      jsonSchemaService.checkCas2BailSchemaOutdated(applicationEntity),
    )
  }

  fun getCas2BailApplicationForUser(applicationId: UUID, user: NomisUserEntity): AuthorisableActionResult<Cas2BailApplicationEntity> {
    val applicationEntity = cas2BailApplicationRepository.findByIdOrNull(applicationId)
      ?: return AuthorisableActionResult.NotFound()

    if (applicationEntity.abandonedAt != null) {
      return AuthorisableActionResult.NotFound()
    }

    val canAccess = cas2BailUserAccessService.userCanViewCas2BailApplication(user, applicationEntity)

    return if (canAccess) {
      AuthorisableActionResult.Success(
        jsonSchemaService.checkCas2BailSchemaOutdated
          (applicationEntity),
      )
    } else {
      AuthorisableActionResult.Unauthorised()
    }
  }

  fun createCas2BailApplication(crn: String, user: NomisUserEntity, jwt: String) =
    validated<Cas2BailApplicationEntity> {
      val offenderDetailsResult = offenderService.getOffenderByCrn(crn)

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

      val createdApplication = cas2BailApplicationRepository.save(
        Cas2BailApplicationEntity(
          id = UUID.randomUUID(),
          crn = crn,
          createdByUser = user,
          data = null,
          document = null,
          schemaVersion = jsonSchemaService.getNewestSchema(Cas2BailApplicationJsonSchemaEntity::class.java),
          createdAt = OffsetDateTime.now(),
          submittedAt = null,
          schemaUpToDate = true,
          nomsNumber = offenderDetails.otherIds.nomsNumber,
          telephoneNumber = null,
        ),
      )

      return success(createdApplication.apply { schemaUpToDate = true })
    }

  @SuppressWarnings("ReturnCount")
  fun updateCas2BailApplication(applicationId: UUID, data: String?, user: NomisUserEntity): AuthorisableActionResult<ValidatableActionResult<Cas2BailApplicationEntity>> {
    val application = cas2BailApplicationRepository.findByIdOrNull(applicationId)?.let(jsonSchemaService::checkCas2BailSchemaOutdated)
      ?: return AuthorisableActionResult.NotFound()

    if (application.createdByUser != user) {
      return AuthorisableActionResult.Unauthorised()
    }

    if (application.submittedAt != null) {
      return AuthorisableActionResult.Success(
        ValidatableActionResult.GeneralValidationError("This application has already been submitted"),
      )
    }

    if (application.abandonedAt != null) {
      return AuthorisableActionResult.Success(
        ValidatableActionResult.GeneralValidationError("This application has been abandoned"),
      )
    }

    if (!application.schemaUpToDate) {
      return AuthorisableActionResult.Success(
        ValidatableActionResult.GeneralValidationError("The schema version is outdated"),
      )
    }

    application.apply {
      this.data = removeXssCharacters(data)
    }

    val savedApplication = cas2BailApplicationRepository.save(application)

    return AuthorisableActionResult.Success(
      ValidatableActionResult.Success(savedApplication),
    )
  }

  @SuppressWarnings("ReturnCount")
  fun abandonCas2BailApplication(applicationId: UUID, user: NomisUserEntity): AuthorisableActionResult<ValidatableActionResult<Cas2BailApplicationEntity>> {
    val application = cas2BailApplicationRepository.findByIdOrNull(applicationId)
      ?: return AuthorisableActionResult.NotFound()

    if (application.createdByUser != user) {
      return AuthorisableActionResult.Unauthorised()
    }

    if (application.submittedAt != null) {
      return AuthorisableActionResult.Success(
        ValidatableActionResult.ConflictError(applicationId, "This application has already been submitted"),
      )
    }

    if (application.abandonedAt != null) {
      return AuthorisableActionResult.Success(
        ValidatableActionResult.Success(application),
      )
    }

    application.apply {
      this.abandonedAt = OffsetDateTime.now()
      this.data = null
    }

    val savedApplication = cas2BailApplicationRepository.save(application)

    return AuthorisableActionResult.Success(
      ValidatableActionResult.Success(savedApplication),
    )
  }

  @SuppressWarnings("ReturnCount")
  @Transactional
  fun submitCas2BailApplication(
    submitApplication: SubmitCas2Application,
    user: NomisUserEntity,
  ): AuthorisableActionResult<ValidatableActionResult<Cas2BailApplicationEntity>> {
    val applicationId = submitApplication.applicationId

    lockableApplicationRepository.acquirePessimisticLock(applicationId)

    var application = cas2BailApplicationRepository.findByIdOrNull(applicationId)
      ?.let(jsonSchemaService::checkCas2BailSchemaOutdated)
      ?: return AuthorisableActionResult.NotFound()

    val serializedTranslatedDocument = objectMapper.writeValueAsString(submitApplication.translatedDocument)

    if (application.createdByUser != user) {
      return AuthorisableActionResult.Unauthorised()
    }

    if (application.abandonedAt != null) {
      return AuthorisableActionResult.Success(
        ValidatableActionResult.GeneralValidationError("This application has already been abandoned"),
      )
    }

    if (application.submittedAt != null) {
      return AuthorisableActionResult.Success(
        ValidatableActionResult.GeneralValidationError("This application has already been submitted"),
      )
    }

    if (!application.schemaUpToDate) {
      return AuthorisableActionResult.Success(
        ValidatableActionResult.GeneralValidationError("The schema version is outdated"),
      )
    }

    val validationErrors = ValidationErrors()
    val applicationData = application.data

    if (applicationData == null) {
      validationErrors["$.data"] = "empty"
    } else if (!jsonSchemaService.validate(application.schemaVersion, applicationData)) {
      validationErrors["$.data"] = "invalid"
    }

    if (validationErrors.any()) {
      return AuthorisableActionResult.Success(
        ValidatableActionResult.FieldValidationError(validationErrors),
      )
    }

    val schema = application.schemaVersion as? Cas2BailApplicationJsonSchemaEntity
      ?: throw RuntimeException("Incorrect type of JSON schema referenced by CAS2 Bail Application")

    try {
      application.apply {
        submittedAt = OffsetDateTime.now()
        document = serializedTranslatedDocument
        referringPrisonCode = retrievePrisonCode(application)
        preferredAreas = submitApplication.preferredAreas
        hdcEligibilityDate = submitApplication.hdcEligibilityDate
        conditionalReleaseDate = submitApplication.conditionalReleaseDate
        telephoneNumber = submitApplication.telephoneNumber
      }
    } catch (error: UpstreamApiException) {
      return AuthorisableActionResult.Success(
        ValidatableActionResult.GeneralValidationError(error.message.toString()),
      )
    }

    application = cas2BailApplicationRepository.save(application)

    createCas2ApplicationSubmittedEvent(application)

    createAssessment(application)

    sendEmailApplicationSubmitted(user, application)

    return AuthorisableActionResult.Success(
      ValidatableActionResult.Success(application),
    )
  }

  fun createCas2ApplicationSubmittedEvent(application: Cas2BailApplicationEntity) {
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
                staffIdentifier = application.createdByUser.nomisStaffId,
                name = application.createdByUser.name,
                username = application.createdByUser.nomisUsername,
              ),
            ),
          ),
        ),
      ),
    )
  }

  fun createAssessment(application: Cas2BailApplicationEntity) {
    assessmentService.createCas2BailAssessment(application)
  }

  @SuppressWarnings("ThrowsCount")
  private fun retrievePrisonCode(application: Cas2BailApplicationEntity): String {
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

  private fun sendEmailApplicationSubmitted(user: NomisUserEntity, application: Cas2BailApplicationEntity) {
    emailNotificationService.sendEmail(
      recipientEmailAddress = notifyConfig.emailAddresses.cas2Assessors,
      templateId = notifyConfig.templates.cas2ApplicationSubmitted,
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