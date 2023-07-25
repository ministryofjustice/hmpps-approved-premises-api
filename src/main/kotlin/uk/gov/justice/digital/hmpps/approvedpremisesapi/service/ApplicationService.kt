package uk.gov.justice.digital.hmpps.approvedpremisesapi.service

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.ApplicationSubmitted
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.ApplicationSubmittedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.ApplicationSubmittedSubmittedBy
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.ApplicationWithdrawn
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.ApplicationWithdrawnEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.ApplicationWithdrawnWithdrawnBy
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.Ldu
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.PersonReference
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.ProbationArea
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.Region
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.StaffMember
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.Team
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SubmitApprovedPremisesApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SubmitTemporaryAccommodationApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ApDeliusContextApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ClientResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.CommunityApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.NotifyConfig
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationTeamCodeEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationTeamCodeRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationJsonSchemaEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2ApplicationJsonSchemaEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.OfflineApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.OfflineApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationApplicationJsonSchemaEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.DomainEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonRisks
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.ValidationErrors
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.OffenderDetailSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.validated
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.AuthorisableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.ValidatableActionResult
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.Period
import java.time.ZoneOffset
import java.util.UUID
import javax.transaction.Transactional

@Service
class ApplicationService(
  private val userRepository: UserRepository,
  private val applicationRepository: ApplicationRepository,
  private val jsonSchemaService: JsonSchemaService,
  private val offenderService: OffenderService,
  private val userService: UserService,
  private val assessmentService: AssessmentService,
  private val offlineApplicationRepository: OfflineApplicationRepository,
  private val domainEventService: DomainEventService,
  private val communityApiClient: CommunityApiClient,
  private val apDeliusContextApiClient: ApDeliusContextApiClient,
  private val applicationTeamCodeRepository: ApplicationTeamCodeRepository,
  private val emailNotificationService: EmailNotificationService,
  private val userAccessService: UserAccessService,
  private val notifyConfig: NotifyConfig,
  private val objectMapper: ObjectMapper,
  @Value("\${url-templates.frontend.application}") private val applicationUrlTemplate: String,
) {
  fun getAllApplicationsForUsername(userDistinguishedName: String, serviceName: ServiceName): List<ApplicationSummary> {
    val userEntity = userRepository.findByDeliusUsername(userDistinguishedName)
      ?: return emptyList()

    val applicationSummaries = when (serviceName) {
      ServiceName.approvedPremises -> getAllApprovedPremisesApplicationsForUser(userEntity)
      ServiceName.cas2 -> getAllCas2ApplicationsForUser(userEntity)
      ServiceName.temporaryAccommodation -> getAllTemporaryAccommodationApplicationsForUser(userEntity)
    }

    return applicationSummaries
      .filter {
        offenderService.canAccessOffender(userDistinguishedName, it.getCrn())
      }
  }

  private fun getAllApprovedPremisesApplicationsForUser(user: UserEntity) =
    applicationRepository.findNonWithdrawnApprovedPremisesSummariesForUser(user.id)

  private fun getAllCas2ApplicationsForUser(user: UserEntity): List<ApplicationSummary> {
    return applicationRepository.findAllCas2ApplicationSummaries()
  }

  private fun getAllTemporaryAccommodationApplicationsForUser(user: UserEntity): List<ApplicationSummary> {
    return when (userAccessService.getTemporaryAccommodationApplicationAccessLevelForUser(user)) {
      TemporaryAccommodationApplicationAccessLevel.SUBMITTED_IN_REGION -> applicationRepository.findAllSubmittedTemporaryAccommodationSummariesByRegion(user.probationRegion.id)
      TemporaryAccommodationApplicationAccessLevel.SELF -> applicationRepository.findAllTemporaryAccommodationSummariesCreatedByUser(user.id)
      TemporaryAccommodationApplicationAccessLevel.NONE -> emptyList()
    }
  }

  fun getAllOfflineApplicationsForUsername(deliusUsername: String, serviceName: ServiceName): List<OfflineApplicationEntity> {
    val userEntity = userRepository.findByDeliusUsername(deliusUsername)
      ?: return emptyList()

    val applications = if (userEntity.hasAnyRole(UserRole.CAS1_WORKFLOW_MANAGER, UserRole.CAS1_ASSESSOR, UserRole.CAS1_MATCHER, UserRole.CAS1_MANAGER)) {
      offlineApplicationRepository.findAllByService(serviceName.value)
    } else {
      emptyList()
    }

    return applications
      .filter {
        offenderService.canAccessOffender(deliusUsername, it.crn)
      }
  }

  fun getApplicationForUsername(applicationId: UUID, userDistinguishedName: String): AuthorisableActionResult<ApplicationEntity> {
    val applicationEntity = applicationRepository.findByIdOrNull(applicationId)
      ?: return AuthorisableActionResult.NotFound()

    val userEntity = userRepository.findByDeliusUsername(userDistinguishedName)
      ?: throw RuntimeException("Could not get user")

    val canAccess = userAccessService.userCanViewApplication(userEntity, applicationEntity)

    return if (canAccess) {
      AuthorisableActionResult.Success(jsonSchemaService.checkSchemaOutdated(applicationEntity))
    } else {
      AuthorisableActionResult.Unauthorised()
    }
  }

  fun getOfflineApplicationForUsername(applicationId: UUID, deliusUsername: String): AuthorisableActionResult<OfflineApplicationEntity> {
    val applicationEntity = offlineApplicationRepository.findByIdOrNull(applicationId)
      ?: return AuthorisableActionResult.NotFound()

    val userEntity = userRepository.findByDeliusUsername(deliusUsername)
      ?: throw RuntimeException("Could not get user")

    if (userEntity.hasAnyRole(UserRole.CAS1_WORKFLOW_MANAGER, UserRole.CAS1_ASSESSOR, UserRole.CAS1_MATCHER, UserRole.CAS1_MANAGER) &&
      offenderService.canAccessOffender(deliusUsername, applicationEntity.crn)
    ) {
      return AuthorisableActionResult.Success(applicationEntity)
    }

    return AuthorisableActionResult.Unauthorised()
  }

  fun createApprovedPremisesApplication(
    offenderDetails: OffenderDetailSummary,
    user: UserEntity,
    jwt: String,
    convictionId: Long?,
    deliusEventNumber: String?,
    offenceId: String?,
    createWithRisks: Boolean? = true,
  ) = validated<ApplicationEntity> {
    val crn = offenderDetails.otherIds.crn

    val managingTeamCodes = when (val managingTeamsResult = apDeliusContextApiClient.getTeamsManagingCase(crn)) {
      is ClientResult.Success -> managingTeamsResult.body.teamCodes
      is ClientResult.Failure -> managingTeamsResult.throwException()
    }

    if (convictionId == null) {
      "$.convictionId" hasValidationError "empty"
    }

    if (deliusEventNumber == null) {
      "$.deliusEventNumber" hasValidationError "empty"
    }

    if (offenceId == null) {
      "$.offenceId" hasValidationError "empty"
    }

    if (validationErrors.any()) {
      return fieldValidationError
    }

    var riskRatings: PersonRisks? = null

    if (createWithRisks == true) {
      val riskRatingsResult = offenderService.getRiskByCrn(crn, jwt, user.deliusUsername)

      riskRatings = when (riskRatingsResult) {
        is AuthorisableActionResult.NotFound -> return "$.crn" hasSingleValidationError "doesNotExist"
        is AuthorisableActionResult.Unauthorised -> return "$.crn" hasSingleValidationError "userPermission"
        is AuthorisableActionResult.Success -> riskRatingsResult.entity
      }
    }

    val createdApplication = applicationRepository.saveAndFlush(
      ApprovedPremisesApplicationEntity(
        id = UUID.randomUUID(),
        crn = crn,
        createdByUser = user,
        data = null,
        document = null,
        schemaVersion = jsonSchemaService.getNewestSchema(ApprovedPremisesApplicationJsonSchemaEntity::class.java),
        createdAt = OffsetDateTime.now(),
        submittedAt = null,
        isWomensApplication = null,
        isPipeApplication = null,
        isEmergencyApplication = null,
        isEsapApplication = null,
        convictionId = convictionId!!,
        eventNumber = deliusEventNumber!!,
        offenceId = offenceId!!,
        schemaUpToDate = true,
        riskRatings = riskRatings,
        assessments = mutableListOf(),
        teamCodes = mutableListOf(),
        placementRequests = mutableListOf(),
        releaseType = null,
        arrivalDate = null,
        isInapplicable = null,
        isWithdrawn = false,
        withdrawalReason = null,
        nomsNumber = offenderDetails.otherIds.nomsNumber,
      ),
    )

    managingTeamCodes.forEach {
      createdApplication.teamCodes += applicationTeamCodeRepository.save(
        ApplicationTeamCodeEntity(
          id = UUID.randomUUID(),
          application = createdApplication,
          teamCode = it,
        ),
      )
    }

    return success(createdApplication.apply { schemaUpToDate = true })
  }

  fun createOfflineApplication(offlineApplication: OfflineApplicationEntity) =
    offlineApplicationRepository.save(offlineApplication)

  fun createCas2Application(crn: String, user: UserEntity, jwt: String) = validated<ApplicationEntity> {
    val offenderDetailsResult = offenderService.getOffenderByCrn(crn, user.deliusUsername)

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

    var riskRatings: PersonRisks? = null

    val riskRatingsResult = offenderService.getRiskByCrn(crn, jwt, user.deliusUsername)

    riskRatings = when (riskRatingsResult) {
      is AuthorisableActionResult.NotFound -> return "$.crn" hasSingleValidationError "doesNotExist"
      is AuthorisableActionResult.Unauthorised -> return "$.crn" hasSingleValidationError "userPermission"
      is AuthorisableActionResult.Success -> riskRatingsResult.entity
    }

    val createdApplication = applicationRepository.save(
      Cas2ApplicationEntity(
        id = UUID.randomUUID(),
        crn = crn,
        createdByUser = user,
        data = null,
        document = null,
        schemaVersion = jsonSchemaService.getNewestSchema(Cas2ApplicationJsonSchemaEntity::class.java),
        createdAt = OffsetDateTime.now(),
        submittedAt = null,
        schemaUpToDate = true,
        riskRatings = riskRatings,
        assessments = mutableListOf(),
        nomsNumber = offenderDetails.otherIds.nomsNumber,
      ),
    )

    return success(createdApplication.apply { schemaUpToDate = true })
  }

  fun createTemporaryAccommodationApplication(
    crn: String,
    user: UserEntity,
    jwt: String,
    convictionId: Long?,
    deliusEventNumber: String?,
    offenceId: String?,
    createWithRisks: Boolean? = true,
  ): AuthorisableActionResult<ValidatableActionResult<ApplicationEntity>> {
    if (!user.hasRole(UserRole.CAS3_REFERRER)) {
      return AuthorisableActionResult.Unauthorised()
    }

    return AuthorisableActionResult.Success(
      validated {
        val offenderDetails = when (val offenderDetailsResult = offenderService.getOffenderByCrn(crn, user.deliusUsername)) {
          is AuthorisableActionResult.NotFound -> return@validated "$.crn" hasSingleValidationError "doesNotExist"
          is AuthorisableActionResult.Unauthorised -> return@validated "$.crn" hasSingleValidationError "userPermission"
          is AuthorisableActionResult.Success -> offenderDetailsResult.entity
        }

        if (convictionId == null) {
          "$.convictionId" hasValidationError "empty"
        }

        if (deliusEventNumber == null) {
          "$.deliusEventNumber" hasValidationError "empty"
        }

        if (offenceId == null) {
          "$.offenceId" hasValidationError "empty"
        }

        if (validationErrors.any()) {
          return@validated fieldValidationError
        }

        var riskRatings: PersonRisks? = null

        if (createWithRisks == true) {
          val riskRatingsResult = offenderService.getRiskByCrn(crn, jwt, user.deliusUsername)

          riskRatings = when (riskRatingsResult) {
            is AuthorisableActionResult.NotFound -> return@validated "$.crn" hasSingleValidationError "doesNotExist"
            is AuthorisableActionResult.Unauthorised -> return@validated "$.crn" hasSingleValidationError "userPermission"
            is AuthorisableActionResult.Success -> riskRatingsResult.entity
          }
        }

        val createdApplication = applicationRepository.save(
          TemporaryAccommodationApplicationEntity(
            id = UUID.randomUUID(),
            crn = crn,
            createdByUser = user,
            data = null,
            document = null,
            schemaVersion = jsonSchemaService.getNewestSchema(TemporaryAccommodationApplicationJsonSchemaEntity::class.java),
            createdAt = OffsetDateTime.now(),
            submittedAt = null,
            convictionId = convictionId!!,
            eventNumber = deliusEventNumber!!,
            offenceId = offenceId!!,
            schemaUpToDate = true,
            riskRatings = riskRatings,
            assessments = mutableListOf(),
            probationRegion = user.probationRegion,
            nomsNumber = offenderDetails.otherIds.nomsNumber,
            arrivalDate = null,
          ),
        )

        success(createdApplication.apply { schemaUpToDate = true })
      },
    )
  }

  fun updateApprovedPremisesApplication(
    applicationId: UUID,
    isWomensApplication: Boolean?,
    isPipeApplication: Boolean?,
    isEmergencyApplication: Boolean?,
    isEsapApplication: Boolean?,
    releaseType: String?,
    arrivalDate: LocalDate?,
    data: String,
    isInapplicable: Boolean?,
    username: String,
  ): AuthorisableActionResult<ValidatableActionResult<ApplicationEntity>> {
    val application = applicationRepository.findByIdOrNull(applicationId)?.let(jsonSchemaService::checkSchemaOutdated)
      ?: return AuthorisableActionResult.NotFound()

    if (application !is ApprovedPremisesApplicationEntity) {
      return AuthorisableActionResult.Success(
        ValidatableActionResult.GeneralValidationError("onlyCas1Supported"),
      )
    }

    val user = userService.getUserForRequest()

    if (application.createdByUser != user) {
      return AuthorisableActionResult.Unauthorised()
    }

    if (!application.schemaUpToDate) {
      return AuthorisableActionResult.Success(
        ValidatableActionResult.GeneralValidationError("The schema version is outdated"),
      )
    }

    if (application.submittedAt != null) {
      return AuthorisableActionResult.Success(
        ValidatableActionResult.GeneralValidationError("This application has already been submitted"),
      )
    }

    application.apply {
      this.isInapplicable = isInapplicable
      this.isWomensApplication = isWomensApplication
      this.isPipeApplication = isPipeApplication
      this.isEmergencyApplication = isEmergencyApplication
      this.isEsapApplication = isEsapApplication
      this.releaseType = releaseType
      this.arrivalDate = if (arrivalDate !== null) OffsetDateTime.of(arrivalDate, LocalTime.MIDNIGHT, ZoneOffset.UTC) else null
      this.data = data
    }

    val savedApplication = applicationRepository.save(application)

    return AuthorisableActionResult.Success(
      ValidatableActionResult.Success(savedApplication),
    )
  }

  @Transactional
  fun withdrawApprovedPremisesApplication(
    applicationId: UUID,
    user: UserEntity,
    withdrawalReason: String?,
  ): AuthorisableActionResult<ValidatableActionResult<Unit>> {
    val application = applicationRepository.findByIdOrNull(applicationId)
      ?: return AuthorisableActionResult.NotFound()

    if (application.createdByUser != user) {
      return AuthorisableActionResult.Unauthorised()
    }

    return AuthorisableActionResult.Success(
      validated {
        if (application !is ApprovedPremisesApplicationEntity) {
          return@validated generalError("onlyCas1Supported")
        }

        if (application.submittedAt != null) {
          return@validated generalError("applicationAlreadySubmitted")
        }

        if (application.isWithdrawn) {
          return@validated generalError("applicationAlreadyWithdrawn")
        }

        applicationRepository.save(
          application.apply {
            this.isWithdrawn = true
            this.withdrawalReason = withdrawalReason
          },
        )

        if (withdrawalReason != null) {
          val domainEventId = UUID.randomUUID()
          val eventOccurredAt = Instant.now()

          val staffDetailsResult = communityApiClient.getStaffUserDetails(user.deliusUsername)
          val staffDetails = when (staffDetailsResult) {
            is ClientResult.Success -> staffDetailsResult.body
            is ClientResult.Failure -> staffDetailsResult.throwException()
          }

          domainEventService.saveApplicationWithdrawnEvent(
            DomainEvent(
              id = domainEventId,
              applicationId = application.id,
              crn = application.crn,
              occurredAt = eventOccurredAt,
              data = ApplicationWithdrawnEnvelope(
                id = domainEventId,
                timestamp = eventOccurredAt,
                eventType = "approved-premises.application.withdrawn",
                eventDetails = ApplicationWithdrawn(
                  applicationId = applicationId,
                  applicationUrl = applicationUrlTemplate.replace("#id", application.id.toString()),
                  personReference = PersonReference(
                    crn = application.crn,
                    noms = application.nomsNumber ?: "Unknown NOMS Number",
                  ),
                  deliusEventNumber = application.eventNumber,
                  withdrawnAt = eventOccurredAt,
                  withdrawnBy = ApplicationWithdrawnWithdrawnBy(
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
                  ),
                  withdrawalReason = withdrawalReason,
                ),
              ),
            ),
          )
        }

        return@validated success(Unit)
      },
    )
  }

  fun updateTemporaryAccommodationApplication(
    applicationId: UUID,
    data: String,
    username: String,
  ): AuthorisableActionResult<ValidatableActionResult<ApplicationEntity>> {
    val application = applicationRepository.findByIdOrNull(applicationId)?.let(jsonSchemaService::checkSchemaOutdated)
      ?: return AuthorisableActionResult.NotFound()

    if (application !is TemporaryAccommodationApplicationEntity) {
      return AuthorisableActionResult.Success(
        ValidatableActionResult.GeneralValidationError("onlyCas3Supported"),
      )
    }

    val user = userService.getUserForRequest()

    if (application.createdByUser != user) {
      return AuthorisableActionResult.Unauthorised()
    }

    if (!application.schemaUpToDate) {
      return AuthorisableActionResult.Success(
        ValidatableActionResult.GeneralValidationError("The schema version is outdated"),
      )
    }

    if (application.submittedAt != null) {
      return AuthorisableActionResult.Success(
        ValidatableActionResult.GeneralValidationError("This application has already been submitted"),
      )
    }

    application.apply {
      this.data = data
    }

    val savedApplication = applicationRepository.save(application)

    return AuthorisableActionResult.Success(
      ValidatableActionResult.Success(savedApplication),
    )
  }

  fun updateCas2Application(applicationId: UUID, data: String?, username: String?): AuthorisableActionResult<ValidatableActionResult<ApplicationEntity>> {
    val application = applicationRepository.findByIdOrNull(applicationId)?.let(jsonSchemaService::checkSchemaOutdated)
      ?: return AuthorisableActionResult.NotFound()

    if (application !is Cas2ApplicationEntity) {
      return AuthorisableActionResult.Success(
        ValidatableActionResult.GeneralValidationError("onlyCas2Supported"),
      )
    }

    val user = userService.getUserForRequest()

    if (application.createdByUser != user) {
      return AuthorisableActionResult.Unauthorised()
    }

    if (!application.schemaUpToDate) {
      return AuthorisableActionResult.Success(
        ValidatableActionResult.GeneralValidationError("The schema version is outdated"),
      )
    }

    if (application.submittedAt != null) {
      return AuthorisableActionResult.Success(
        ValidatableActionResult.GeneralValidationError("This application has already been submitted"),
      )
    }

    application.apply {
      this.data = data
    }

    val savedApplication = applicationRepository.save(application)

    return AuthorisableActionResult.Success(
      ValidatableActionResult.Success(savedApplication),
    )
  }

  @Transactional
  fun submitApprovedPremisesApplication(applicationId: UUID, submitApplication: SubmitApprovedPremisesApplication, username: String, jwt: String): AuthorisableActionResult<ValidatableActionResult<ApplicationEntity>> {
    var application = applicationRepository.findByIdOrNullWithWriteLock(applicationId)?.let(jsonSchemaService::checkSchemaOutdated)
      ?: return AuthorisableActionResult.NotFound()

    val serializedTranslatedDocument = objectMapper.writeValueAsString(submitApplication.translatedDocument)

    val user = userService.getUserForRequest()

    if (application.createdByUser != user) {
      return AuthorisableActionResult.Unauthorised()
    }

    if (application !is ApprovedPremisesApplicationEntity) {
      return AuthorisableActionResult.Success(
        ValidatableActionResult.GeneralValidationError("onlyCas1Supported"),
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

    val schema = application.schemaVersion as? ApprovedPremisesApplicationJsonSchemaEntity
      ?: throw RuntimeException("Incorrect type of JSON schema referenced by AP Application")

    application.apply {
      isWomensApplication = submitApplication.isWomensApplication
      isPipeApplication = submitApplication.isPipeApplication
      this.isEmergencyApplication = isEmergencyApplication
      this.isEsapApplication = isEsapApplication
      submittedAt = OffsetDateTime.now()
      document = serializedTranslatedDocument
      releaseType = submitApplication.releaseType.toString()
      arrivalDate = if (submitApplication.arrivalDate !== null) OffsetDateTime.of(submitApplication.arrivalDate, LocalTime.MIDNIGHT, ZoneOffset.UTC) else null
    }

    assessmentService.createAssessment(application)

    application = applicationRepository.save(application)

    createApplicationSubmittedEvent(application, submitApplication, username, jwt)

    emailNotificationService.sendEmail(
      user = user,
      templateId = notifyConfig.templates.applicationSubmitted,
      personalisation = mapOf(
        "name" to user.name,
        "applicationUrl" to applicationUrlTemplate.replace("#id", application.id.toString()),
        "crn" to application.crn,
      ),
    )

    return AuthorisableActionResult.Success(
      ValidatableActionResult.Success(application),
    )
  }

  @Transactional
  fun submitTemporaryAccommodationApplication(
    applicationId: UUID,
    submitApplication: SubmitTemporaryAccommodationApplication,
  ): AuthorisableActionResult<ValidatableActionResult<ApplicationEntity>> {
    var application = applicationRepository.findByIdOrNullWithWriteLock(applicationId)?.let(jsonSchemaService::checkSchemaOutdated)
      ?: return AuthorisableActionResult.NotFound()

    val serializedTranslatedDocument = objectMapper.writeValueAsString(submitApplication.translatedDocument)

    val user = userService.getUserForRequest()

    if (application.createdByUser != user) {
      return AuthorisableActionResult.Unauthorised()
    }

    if (application !is TemporaryAccommodationApplicationEntity) {
      return AuthorisableActionResult.Success(
        ValidatableActionResult.GeneralValidationError("onlyCas3Supported"),
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

    val schema = application.schemaVersion as? TemporaryAccommodationApplicationJsonSchemaEntity
      ?: throw RuntimeException("Incorrect type of JSON schema referenced by TA Application")

    application.apply {
      submittedAt = OffsetDateTime.now()
      document = serializedTranslatedDocument
      arrivalDate = OffsetDateTime.of(submitApplication.arrivalDate, LocalTime.MIDNIGHT, ZoneOffset.UTC)
    }

    assessmentService.createAssessment(application)

    application = applicationRepository.save(application)

    return AuthorisableActionResult.Success(
      ValidatableActionResult.Success(application),
    )
  }

  private fun createApplicationSubmittedEvent(application: ApprovedPremisesApplicationEntity, submitApplication: SubmitApprovedPremisesApplication, username: String, jwt: String) {
    val domainEventId = UUID.randomUUID()
    val eventOccurredAt = OffsetDateTime.now()

    val offenderDetails = when (val offenderDetailsResult = offenderService.getOffenderByCrn(application.crn, username, true)) {
      is AuthorisableActionResult.Success -> offenderDetailsResult.entity
      is AuthorisableActionResult.Unauthorised -> throw RuntimeException("Unable to get Offender Details when creating Application Submitted Domain Event: Unauthorised")
      is AuthorisableActionResult.NotFound -> throw RuntimeException("Unable to get Offender Details when creating Application Submitted Domain Event: Not Found")
    }

    val risks = when (val riskResult = offenderService.getRiskByCrn(application.crn, jwt, username)) {
      is AuthorisableActionResult.Success -> riskResult.entity
      is AuthorisableActionResult.Unauthorised -> throw RuntimeException("Unable to get Risks when creating Application Submitted Domain Event: Unauthorised")
      is AuthorisableActionResult.NotFound -> throw RuntimeException("Unable to get Risks when creating Application Submitted Domain Event: Not Found")
    }

    val mappaLevel = risks.mappa.value?.level

    val staffDetailsResult = communityApiClient.getStaffUserDetails(username)
    val staffDetails = when (staffDetailsResult) {
      is ClientResult.Success -> staffDetailsResult.body
      is ClientResult.Failure -> staffDetailsResult.throwException()
    }

    // TODO: The integrations team have agreed to build us an endpoint to determine which team's caseload an Offender belongs to
    //       once that is done we should use that here to select the right team when the user is a memnber of more than one

    val team = staffDetails.teams?.firstOrNull()
      ?: throw RuntimeException("No teams present on Staff Details when creating Application Submitted Domain Event")

    domainEventService.saveApplicationSubmittedDomainEvent(
      DomainEvent(
        id = domainEventId,
        applicationId = application.id,
        crn = application.crn,
        occurredAt = eventOccurredAt.toInstant(),
        data = ApplicationSubmittedEnvelope(
          id = domainEventId,
          timestamp = eventOccurredAt.toInstant(),
          eventType = "approved-premises.application.submitted",
          eventDetails = ApplicationSubmitted(
            applicationId = application.id,
            applicationUrl = applicationUrlTemplate
              .replace("#id", application.id.toString()),
            personReference = PersonReference(
              crn = application.crn,
              noms = offenderDetails.otherIds.nomsNumber ?: "Unknown NOMS Number",
            ),
            deliusEventNumber = application.eventNumber,
            mappa = mappaLevel,
            offenceId = application.offenceId,
            releaseType = submitApplication.releaseType.toString(),
            age = Period.between(offenderDetails.dateOfBirth, LocalDate.now()).years,
            gender = when (offenderDetails.gender.lowercase()) {
              "male" -> ApplicationSubmitted.Gender.male
              "female" -> ApplicationSubmitted.Gender.female
              else -> throw RuntimeException("Unknown gender: ${offenderDetails.gender}")
            },
            targetLocation = submitApplication.targetLocation,
            submittedAt = Instant.now(),
            submittedBy = ApplicationSubmittedSubmittedBy(
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
              team = Team(
                code = team.code,
                name = team.description,
              ),
              ldu = Ldu(
                code = team.teamType.code,
                name = team.teamType.description,
              ),
              region = Region(
                code = staffDetails.probationArea.code,
                name = staffDetails.probationArea.description,
              ),
            ),
            sentenceLengthInMonths = null,
          ),
        ),
      ),
    )
  }

  fun getApplicationsForCrn(crn: String, serviceName: ServiceName): List<ApplicationEntity> {
    val entityType = if (serviceName == ServiceName.approvedPremises) {
      ApprovedPremisesApplicationEntity::class.java
    } else {
      TemporaryAccommodationApplicationEntity::class.java
    }

    return applicationRepository.findByCrn(crn, entityType)
  }

  fun getOfflineApplicationsForCrn(crn: String, serviceName: ServiceName) =
    offlineApplicationRepository.findAllByServiceAndCrn(serviceName.value, crn)
}
