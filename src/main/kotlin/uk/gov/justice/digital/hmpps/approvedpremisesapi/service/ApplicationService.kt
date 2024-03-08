package uk.gov.justice.digital.hmpps.approvedpremisesapi.service

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.ApplicationWithdrawn
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.ApplicationWithdrawnEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.PersonReference
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApplicationSortField
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApplicationTimelineNote
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1ApplicationUserDetails
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SortDirection
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SubmitApprovedPremisesApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SubmitTemporaryAccommodationApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.TimelineEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.WithdrawalReason
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ApDeliusContextApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ClientResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.NotifyConfig
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApAreaRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationTeamCodeEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationTeamCodeRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationJsonSchemaEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1ApplicationUserDetailsEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1ApplicationUserDetailsRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.OfflineApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.OfflineApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationApplicationJsonSchemaEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.ApprovedPremisesApplicationStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.DomainEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PaginationMetadata
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonInfoResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonRisks
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.ValidationErrors
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.OffenderDetailSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.prisonsapi.InmateStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.validated
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.AuthorisableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.CasResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.ValidatableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1ApplicationDomainEventService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.WithdrawableState
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.ApplicationTimelineNoteTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.ApplicationTimelineTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.AssessmentClarificationNoteTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.DomainEventTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.getMetadata
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.getPageable
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID
import javax.transaction.Transactional
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas3.DomainEventService as Cas3DomainEventService

@Service
@Suppress(
  "LongParameterList",
  "TooManyFunctions",
  "ReturnCount",
  "ThrowsCount",
  "TooGenericExceptionThrown",
)
class ApplicationService(
  private val userRepository: UserRepository,
  private val applicationRepository: ApplicationRepository,
  private val jsonSchemaService: JsonSchemaService,
  private val offenderService: OffenderService,
  private val userService: UserService,
  private val assessmentService: AssessmentService,
  private val offlineApplicationRepository: OfflineApplicationRepository,
  private val applicationTimelineNoteService: ApplicationTimelineNoteService,
  private val applicationTimelineNoteTransformer: ApplicationTimelineNoteTransformer,
  private val domainEventService: DomainEventService,
  private val cas3DomainEventService: Cas3DomainEventService,
  private val apDeliusContextApiClient: ApDeliusContextApiClient,
  private val applicationTeamCodeRepository: ApplicationTeamCodeRepository,
  private val emailNotificationService: EmailNotificationService,
  private val userAccessService: UserAccessService,
  private val notifyConfig: NotifyConfig,
  private val assessmentClarificationNoteTransformer: AssessmentClarificationNoteTransformer,
  private val objectMapper: ObjectMapper,
  @Value("\${url-templates.frontend.application}") private val applicationUrlTemplate: String,
  private val apAreaRepository: ApAreaRepository,
  private val applicationTimelineTransformer: ApplicationTimelineTransformer,
  private val domainEventTransformer: DomainEventTransformer,
  private val cas1ApplicationDomainEventService: Cas1ApplicationDomainEventService,
  private val cas1ApplicationUserDetailsRepository: Cas1ApplicationUserDetailsRepository,
) {
  fun getApplication(applicationId: UUID) = applicationRepository.findByIdOrNull(applicationId)

  fun getAllApplicationsForUsername(userDistinguishedName: String, serviceName: ServiceName): List<ApplicationSummary> {
    val userEntity = userRepository.findByDeliusUsername(userDistinguishedName)
      ?: return emptyList()

    val applicationSummaries = when (serviceName) {
      ServiceName.approvedPremises -> getAllApprovedPremisesApplicationsForUser(userEntity)
      ServiceName.cas2 -> throw RuntimeException(
        "CAS2 applications now require " +
          "NomisUser",
      )

      ServiceName.temporaryAccommodation -> getAllTemporaryAccommodationApplicationsForUser(userEntity)
    }

    return applicationSummaries
      .filter {
        offenderService.canAccessOffender(userDistinguishedName, it.getCrn())
      }
  }

  fun getAllApprovedPremisesApplications(
    page: Int?,
    crnOrName: String?,
    sortDirection: SortDirection?,
    status: ApprovedPremisesApplicationStatus?,
    sortBy: ApplicationSortField?,
  ): Pair<List<ApprovedPremisesApplicationSummary>, PaginationMetadata?> {
    val sortField = when (sortBy) {
      ApplicationSortField.arrivalDate -> "arrivalDate"
      ApplicationSortField.createdAt -> "a.created_at"
      ApplicationSortField.tier -> "tier"
      else -> "a.created_at"
    }
    val pageable = getPageable(sortField, sortDirection, page)

    val response = applicationRepository.findAllApprovedPremisesSummaries(
      pageable,
      crnOrName,
      status,
    )

    return Pair(response.content, getMetadata(response, page))
  }

  private fun getAllApprovedPremisesApplicationsForUser(user: UserEntity) =
    applicationRepository.findNonWithdrawnApprovedPremisesSummariesForUser(user.id)

  private fun getAllTemporaryAccommodationApplicationsForUser(user: UserEntity): List<ApplicationSummary> {
    return when (userAccessService.getTemporaryAccommodationApplicationAccessLevelForUser(user)) {
      TemporaryAccommodationApplicationAccessLevel.SUBMITTED_IN_REGION ->
        applicationRepository.findAllSubmittedTemporaryAccommodationSummariesByRegion(user.probationRegion.id)

      TemporaryAccommodationApplicationAccessLevel.SELF ->
        applicationRepository.findAllTemporaryAccommodationSummariesCreatedByUser(user.id)

      TemporaryAccommodationApplicationAccessLevel.NONE -> emptyList()
    }
  }

  fun getAllOfflineApplicationsForUsername(
    deliusUsername: String,
    serviceName: ServiceName,
  ): List<OfflineApplicationEntity> {
    val userEntity = userRepository.findByDeliusUsername(deliusUsername)
      ?: return emptyList()

    val applications =
      if (
        userEntity.hasAnyRole(
          UserRole.CAS1_WORKFLOW_MANAGER,
          UserRole.CAS1_ASSESSOR,
          UserRole.CAS1_MATCHER,
          UserRole.CAS1_MANAGER,
        )
      ) {
        offlineApplicationRepository.findAllByService(serviceName.value)
      } else {
        emptyList()
      }

    return applications
      .filter {
        offenderService.canAccessOffender(deliusUsername, it.crn)
      }
  }

  fun getApplicationForUsername(
    applicationId: UUID,
    userDistinguishedName: String,
  ): AuthorisableActionResult<ApplicationEntity> {
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

  fun getOfflineApplicationForUsername(
    applicationId: UUID,
    deliusUsername: String,
  ): AuthorisableActionResult<OfflineApplicationEntity> {
    val applicationEntity = offlineApplicationRepository.findByIdOrNull(applicationId)
      ?: return AuthorisableActionResult.NotFound()

    val userEntity = userRepository.findByDeliusUsername(deliusUsername)
      ?: throw RuntimeException("Could not get user")

    if (userEntity.hasAnyRole(
        UserRole.CAS1_WORKFLOW_MANAGER,
        UserRole.CAS1_ASSESSOR,
        UserRole.CAS1_MATCHER,
        UserRole.CAS1_MANAGER,
      ) &&
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
      createApprovedPremisesApplicationEntity(
        crn,
        user,
        convictionId,
        deliusEventNumber,
        offenceId,
        riskRatings,
        offenderDetails,
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

  fun createApprovedPremisesApplicationEntity(
    crn: String,
    user: UserEntity,
    convictionId: Long?,
    deliusEventNumber: String?,
    offenceId: String?,
    riskRatings: PersonRisks?,
    offenderDetails: OffenderDetailSummary,
  ): ApprovedPremisesApplicationEntity {
    return ApprovedPremisesApplicationEntity(
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
      otherWithdrawalReason = null,
      nomsNumber = offenderDetails.otherIds.nomsNumber,
      name = "${offenderDetails.firstName.uppercase()} ${offenderDetails.surname.uppercase()}",
      targetLocation = null,
      status = ApprovedPremisesApplicationStatus.STARTED,
      sentenceType = null,
      situation = null,
      inmateInOutStatusOnSubmission = null,
      apArea = null,
      applicantUserDetails = null,
      caseManagerIsNotApplicant = null,
      caseManagerUserDetails = null,
    )
  }

  fun createOfflineApplication(offlineApplication: OfflineApplicationEntity) =
    offlineApplicationRepository.save(offlineApplication)

  fun createTemporaryAccommodationApplication(
    crn: String,
    user: UserEntity,
    jwt: String,
    convictionId: Long?,
    deliusEventNumber: String?,
    offenceId: String?,
    createWithRisks: Boolean? = true,
    personInfo: PersonInfoResult.Success.Full,
  ): AuthorisableActionResult<ValidatableActionResult<ApplicationEntity>> {
    if (!user.hasRole(UserRole.CAS3_REFERRER)) {
      return AuthorisableActionResult.Unauthorised()
    }

    return AuthorisableActionResult.Success(
      validated {
        val offenderDetails =
          when (
            val offenderDetailsResult = offenderService.getOffenderByCrn(crn, user.deliusUsername)
          ) {
            is AuthorisableActionResult.NotFound -> return@validated "$.crn" hasSingleValidationError "doesNotExist"
            is AuthorisableActionResult.Unauthorised ->
              return@validated "$.crn" hasSingleValidationError "userPermission"

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
            is AuthorisableActionResult.NotFound ->
              return@validated "$.crn" hasSingleValidationError "doesNotExist"

            is AuthorisableActionResult.Unauthorised ->
              return@validated "$.crn" hasSingleValidationError "userPermission"

            is AuthorisableActionResult.Success -> riskRatingsResult.entity
          }
        }

        val prisonName = getPrisonName(personInfo)

        val createdApplication = applicationRepository.save(
          createTemporaryAccommodationApplicationEntity(
            crn,
            user,
            convictionId,
            deliusEventNumber,
            offenceId,
            riskRatings,
            offenderDetails,
            prisonName,
          ),
        )

        success(createdApplication.apply { schemaUpToDate = true })
      },
    )
  }

  private fun createTemporaryAccommodationApplicationEntity(
    crn: String,
    user: UserEntity,
    convictionId: Long?,
    deliusEventNumber: String?,
    offenceId: String?,
    riskRatings: PersonRisks?,
    offenderDetails: OffenderDetailSummary,
    prisonName: String?,
  ): TemporaryAccommodationApplicationEntity {
    return TemporaryAccommodationApplicationEntity(
      id = UUID.randomUUID(),
      crn = crn,
      createdByUser = user,
      data = null,
      document = null,
      schemaVersion = jsonSchemaService.getNewestSchema(
        TemporaryAccommodationApplicationJsonSchemaEntity::class.java,
      ),
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
      isRegisteredSexOffender = null,
      needsAccessibleProperty = null,
      hasHistoryOfArson = null,
      isDutyToReferSubmitted = null,
      dutyToReferSubmissionDate = null,
      isEligible = null,
      eligibilityReason = null,
      dutyToReferLocalAuthorityAreaName = null,
      prisonNameOnCreation = prisonName,
      personReleaseDate = null,
    )
  }

  data class Cas1ApplicationUpdateFields(
    val isWomensApplication: Boolean?,
    val isPipeApplication: Boolean?,
    val isEmergencyApplication: Boolean?,
    val isEsapApplication: Boolean?,
    val releaseType: String?,
    val arrivalDate: LocalDate?,
    val data: String,
    val isInapplicable: Boolean?,
  )

  @Transactional
  fun updateApprovedPremisesApplication(
    applicationId: UUID,
    updateFields: Cas1ApplicationUpdateFields,
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
      this.isInapplicable = updateFields.isInapplicable
      this.isWomensApplication = updateFields.isWomensApplication
      this.isPipeApplication = updateFields.isPipeApplication
      this.isEmergencyApplication = updateFields.isEmergencyApplication
      this.isEsapApplication = updateFields.isEsapApplication
      this.releaseType = updateFields.releaseType
      this.arrivalDate = if (updateFields.arrivalDate !== null) {
        OffsetDateTime.of(updateFields.arrivalDate, LocalTime.MIDNIGHT, ZoneOffset.UTC)
      } else {
        null
      }
      this.data = updateFields.data
    }

    val savedApplication = applicationRepository.save(application)

    return AuthorisableActionResult.Success(
      ValidatableActionResult.Success(savedApplication),
    )
  }

  private fun upsertCas1ApplicationUserDetails(
    existingEntry: Cas1ApplicationUserDetailsEntity?,
    updatedValues: Cas1ApplicationUserDetails?,
  ): Cas1ApplicationUserDetailsEntity? {
    if (updatedValues == null) {
      existingEntry?.let {
        cas1ApplicationUserDetailsRepository.delete(it)
      }
      return null
    }

    return cas1ApplicationUserDetailsRepository.save(
      Cas1ApplicationUserDetailsEntity(
        id = existingEntry?.id ?: UUID.randomUUID(),
        name = updatedValues.name,
        email = updatedValues.email,
        telephoneNumber = updatedValues.telephoneNumber,
      ),
    )
  }

  fun updateApprovedPremisesApplicationStatus(applicationId: UUID, status: ApprovedPremisesApplicationStatus) {
    applicationRepository.updateStatus(applicationId, status)
  }

  /**
   * This function should not be called directly. Instead, use [WithdrawableService.withdrawApplication] that
   * will indirectly invoke this function. It will also ensure that:
   *
   * 1. The entity is withdrawable, and error if not
   * 2. The user is allowed to withdraw it, and error if not
   * 3. If withdrawn, all descdents entities are withdrawn, where applicable
   */
  @Transactional
  fun withdrawApprovedPremisesApplication(
    applicationId: UUID,
    user: UserEntity,
    withdrawalReason: String,
    otherReason: String?,
  ): CasResult<Unit> {
    val application = applicationRepository.findByIdOrNull(applicationId)
      ?: return CasResult.NotFound()

    if (application !is ApprovedPremisesApplicationEntity) {
      return CasResult.GeneralValidationError("onlyCas1Supported")
    }

    if (application.isWithdrawn) {
      return CasResult.Success(Unit)
    }

    applicationRepository.save(
      application.apply {
        this.isWithdrawn = true
        this.withdrawalReason = withdrawalReason
        this.otherWithdrawalReason = if (withdrawalReason == WithdrawalReason.other.value) {
          otherReason
        } else {
          null
        }
      },
    )

    val domainEventId = UUID.randomUUID()
    val eventOccurredAt = Instant.now()

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
          eventDetails = getApplicationWithdrawn(application, user, eventOccurredAt),
        ),
      ),
    )

    val premisesName = application.getLatestBooking()?.premises?.name
    sendEmailApplicationWithdrawn(user, application, premisesName)

    application.assessments.map {
      assessmentService.updateCas1AssessmentWithdrawn(it.id)
    }

    return CasResult.Success(Unit)
  }

  fun getWithdrawableState(application: ApprovedPremisesApplicationEntity, user: UserEntity): WithdrawableState {
    return WithdrawableState(
      withdrawable = !application.isWithdrawn,
      userMayDirectlyWithdraw = userAccessService.userMayWithdrawApplication(user, application),
    )
  }

  fun sendEmailApplicationWithdrawn(user: UserEntity, application: ApplicationEntity, premisesName: String?) {
    user.email?.let { email ->
      emailNotificationService.sendEmail(
        recipientEmailAddress = email,
        templateId = notifyConfig.templates.applicationWithdrawn,
        personalisation = mapOf(
          "name" to user.name,
          "apName" to premisesName,
          "applicationUrl" to applicationUrlTemplate.replace("#id", application.id.toString()),
          "crn" to application.crn,
        ),
      )
    }
  }

  private fun getApplicationWithdrawn(
    application: ApprovedPremisesApplicationEntity,
    user: UserEntity,
    eventOccurredAt: Instant,
  ): ApplicationWithdrawn {
    return ApplicationWithdrawn(
      applicationId = application.id,
      applicationUrl = applicationUrlTemplate.replace("#id", application.id.toString()),
      personReference = PersonReference(
        crn = application.crn,
        noms = application.nomsNumber ?: "Unknown NOMS Number",
      ),
      deliusEventNumber = application.eventNumber,
      withdrawnAt = eventOccurredAt,
      withdrawnBy = domainEventTransformer.toWithdrawnBy(user),
      withdrawalReason = application.withdrawalReason!!,
      otherWithdrawalReason = application.otherWithdrawalReason,
    )
  }

  fun updateTemporaryAccommodationApplication(
    applicationId: UUID,
    data: String,
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

  @SuppressWarnings("CyclomaticComplexMethod")
  @Transactional
  fun submitApprovedPremisesApplication(
    applicationId: UUID,
    submitApplication: SubmitApprovedPremisesApplication,
    username: String,
    jwt: String,
    apAreaId: UUID?,
  ): AuthorisableActionResult<ValidatableActionResult<ApplicationEntity>> {
    var application = applicationRepository.findByIdOrNullWithWriteLock(
      applicationId,
    )?.let(jsonSchemaService::checkSchemaOutdated)
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

    if (submitApplication.caseManagerIsNotApplicant == true && submitApplication.caseManagerUserDetails == null) {
      return AuthorisableActionResult.Success(
        ValidatableActionResult.GeneralValidationError("caseManagerUserDetails must be provided if caseManagerIsNotApplicant is true"),
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

    val inmateDetails = application.nomsNumber?.let { nomsNumber ->
      when (val inmateDetailsResult = offenderService.getInmateDetailByNomsNumber(application.crn, nomsNumber)) {
        is AuthorisableActionResult.Success -> inmateDetailsResult.entity
        else -> null
      }
    }

    application.apply {
      isWomensApplication = submitApplication.isWomensApplication
      isPipeApplication = submitApplication.isPipeApplication
      this.isEmergencyApplication = isEmergencyApplication
      this.isEsapApplication = isEsapApplication
      submittedAt = OffsetDateTime.now()
      document = serializedTranslatedDocument
      releaseType = submitApplication.releaseType.toString()
      targetLocation = submitApplication.targetLocation
      arrivalDate = getArrivalDate(submitApplication.arrivalDate)
      sentenceType = submitApplication.sentenceType.toString()
      situation = submitApplication.situation?.toString()
      inmateInOutStatusOnSubmission = inmateDetails?.status?.name
      apArea = apAreaRepository.findByIdOrNull(apAreaId)
      this.applicantUserDetails = upsertCas1ApplicationUserDetails(this.applicantUserDetails, submitApplication.applicantUserDetails)
      this.caseManagerIsNotApplicant = submitApplication.caseManagerIsNotApplicant
      this.caseManagerUserDetails = upsertCas1ApplicationUserDetails(
        existingEntry = this.caseManagerUserDetails,
        updatedValues = if (submitApplication.caseManagerIsNotApplicant == true) { submitApplication.caseManagerUserDetails } else null,
      )
    }

    assessmentService.createApprovedPremisesAssessment(application)
    application = applicationRepository.save(application)

    cas1ApplicationDomainEventService.applicationSubmitted(application, submitApplication, username, jwt)
    if (user.email != null) {
      sendEmailApplicationSubmitted(user, application)
    }

    return AuthorisableActionResult.Success(
      ValidatableActionResult.Success(application),
    )
  }

  fun getArrivalDate(arrivalDate: LocalDate?): OffsetDateTime? {
    if (arrivalDate !== null) {
      return OffsetDateTime.of(arrivalDate, LocalTime.MIDNIGHT, ZoneOffset.UTC)
    }
    return null
  }

  fun sendEmailApplicationSubmitted(user: UserEntity, application: ApplicationEntity) {
    emailNotificationService.sendEmail(
      recipientEmailAddress = user.email!!,
      templateId = notifyConfig.templates.applicationSubmitted,
      personalisation = mapOf(
        "name" to user.name,
        "applicationUrl" to applicationUrlTemplate.replace("#id", application.id.toString()),
        "crn" to application.crn,
      ),
    )
  }

  @Transactional
  fun submitTemporaryAccommodationApplication(
    applicationId: UUID,
    submitApplication: SubmitTemporaryAccommodationApplication,
  ): AuthorisableActionResult<ValidatableActionResult<ApplicationEntity>> {
    var application =
      applicationRepository.findByIdOrNullWithWriteLock(
        applicationId,
      )?.let(jsonSchemaService::checkSchemaOutdated)
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

    application.apply {
      submittedAt = OffsetDateTime.now()
      document = serializedTranslatedDocument
      arrivalDate = OffsetDateTime.of(submitApplication.arrivalDate, LocalTime.MIDNIGHT, ZoneOffset.UTC)
      isRegisteredSexOffender = submitApplication.isRegisteredSexOffender
      needsAccessibleProperty = submitApplication.needsAccessibleProperty
      hasHistoryOfArson = submitApplication.hasHistoryOfArson
      isDutyToReferSubmitted = submitApplication.isDutyToReferSubmitted
      dutyToReferSubmissionDate = submitApplication.dutyToReferSubmissionDate
      isEligible = submitApplication.isApplicationEligible
      eligibilityReason = submitApplication.eligibilityReason
      dutyToReferLocalAuthorityAreaName = submitApplication.dutyToReferLocalAuthorityAreaName
      personReleaseDate = submitApplication.personReleaseDate
    }

    assessmentService.createTemporaryAccommodationAssessment(application, submitApplication.summaryData)

    application = applicationRepository.save(application)

    cas3DomainEventService.saveReferralSubmittedEvent(application)

    return AuthorisableActionResult.Success(
      ValidatableActionResult.Success(application),
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

  fun getApplicationTimeline(applicationId: UUID): List<TimelineEvent> {
    val domainEvents = domainEventService.getAllDomainEventsForApplication(applicationId)
    val timelineEvents = domainEvents.map {
      applicationTimelineTransformer.transformDomainEventSummaryToTimelineEvent(it)
    }.toMutableList()

    timelineEvents += getAllInformationRequestEventsForApplication(applicationId)
    timelineEvents += getAllApplicationTimelineNotesByApplicationId(applicationId)

    return timelineEvents
  }

  fun getAllInformationRequestEventsForApplication(applicationId: UUID): List<TimelineEvent> {
    val assessments = applicationRepository.findAllAssessmentsById(applicationId)
    val allClarifications = assessments.flatMap { it.clarificationNotes }
    return allClarifications.map {
      assessmentClarificationNoteTransformer.transformToTimelineEvent(it)
    }
  }

  fun getAllApplicationTimelineNotesByApplicationId(applicationId: UUID): List<TimelineEvent> {
    val noteEntities = applicationTimelineNoteService.getApplicationTimelineNotesByApplicationId(applicationId)
    return noteEntities.map {
      applicationTimelineNoteTransformer.transformToTimelineEvents(it)
    }
  }

  fun addNoteToApplication(
    applicationId: UUID,
    note: String,
    user: UserEntity,
  ): ApplicationTimelineNote {
    val savedNote = applicationTimelineNoteService.saveApplicationTimelineNote(applicationId, note, user)
    return applicationTimelineNoteTransformer.transformJpaToApi(savedNote)
  }

  private fun getPrisonName(personInfo: PersonInfoResult.Success.Full): String? {
    val prisonName = when (personInfo.inmateDetail?.status) {
      InmateStatus.IN,
      InmateStatus.TRN,
      -> {
        personInfo.inmateDetail?.assignedLivingUnit?.agencyName ?: personInfo.inmateDetail?.assignedLivingUnit?.agencyId
      }
      else -> null
    }
    return prisonName
  }
}
