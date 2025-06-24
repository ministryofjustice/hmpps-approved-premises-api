package uk.gov.justice.digital.hmpps.approvedpremisesapi.service

import jakarta.transaction.Transactional
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.WithdrawalReason
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.OfflineApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.OfflineApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationApplicationJsonSchemaEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserPermission
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.listeners.ApplicationListener
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.ApprovedPremisesApplicationStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonInfoResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonRisks
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.OffenderDetailSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.prisonsapi.InmateStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.validatedCasResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.AuthorisableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.CasResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1ApplicationDomainEventService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1ApplicationEmailService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.WithdrawableState
import java.time.LocalDate
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

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
  private val offenderRisksService: OffenderRisksService,
  private val userService: UserService,
  private val assessmentService: AssessmentService,
  private val offlineApplicationRepository: OfflineApplicationRepository,
  private val userAccessService: UserAccessService,
  private val cas1ApplicationDomainEventService: Cas1ApplicationDomainEventService,
  private val cas1ApplicationEmailService: Cas1ApplicationEmailService,
  private val applicationListener: ApplicationListener,
) {
  fun getApplication(applicationId: UUID) = applicationRepository.findByIdOrNull(applicationId)

  fun getAllApplicationsForUsername(userEntity: UserEntity, serviceName: ServiceName): List<ApplicationSummary> = when (serviceName) {
    ServiceName.approvedPremises -> getAllApprovedPremisesApplicationsForUser(userEntity)
    ServiceName.cas2 -> throw RuntimeException("CAS2 applications now require NomisUser")
    ServiceName.cas2v2 -> throw RuntimeException("CAS2v2 applications now require Cas2v2User")
    ServiceName.temporaryAccommodation -> getAllTemporaryAccommodationApplicationsForUser(userEntity)
  }

  fun getAllApprovedPremisesApplicationsForUser(user: UserEntity) = applicationRepository.findNonWithdrawnApprovedPremisesSummariesForUser(user.id)

  private fun getAllTemporaryAccommodationApplicationsForUser(user: UserEntity): List<ApplicationSummary> = applicationRepository.findAllTemporaryAccommodationSummariesCreatedByUser(user.id)

  fun getApplicationForUsername(
    applicationId: UUID,
    userDistinguishedName: String,
  ): CasResult<ApplicationEntity> {
    val applicationEntity = applicationRepository.findByIdOrNull(applicationId)
      ?: return CasResult.NotFound("Application", applicationId.toString())

    if (applicationEntity is TemporaryAccommodationApplicationEntity && applicationEntity.deletedAt != null) {
      return CasResult.NotFound("Application", applicationId.toString())
    }

    val userEntity = userRepository.findByDeliusUsername(userDistinguishedName)
      ?: throw RuntimeException("Could not get user")

    val canAccess = userAccessService.userCanViewApplication(userEntity, applicationEntity)

    return if (canAccess) {
      CasResult.Success(jsonSchemaService.checkSchemaOutdated(applicationEntity))
    } else {
      CasResult.Unauthorised()
    }
  }

  fun getOfflineApplicationForUsername(
    applicationId: UUID,
    deliusUsername: String,
  ): CasResult<OfflineApplicationEntity> {
    val applicationEntity = offlineApplicationRepository.findByIdOrNull(applicationId)
      ?: return CasResult.NotFound("Application", applicationId.toString())

    val userEntity = userRepository.findByDeliusUsername(deliusUsername)
      ?: throw RuntimeException("Could not get user")

    if (userEntity.hasPermission(UserPermission.CAS1_OFFLINE_APPLICATION_VIEW) &&
      offenderService.canAccessOffender(deliusUsername, applicationEntity.crn) == true
    ) {
      return CasResult.Success(applicationEntity)
    }

    return CasResult.Unauthorised()
  }

  fun createTemporaryAccommodationApplication(
    crn: String,
    user: UserEntity,
    convictionId: Long?,
    deliusEventNumber: String?,
    offenceId: String?,
    createWithRisks: Boolean? = true,
    personInfo: PersonInfoResult.Success.Full,
  ): CasResult<TemporaryAccommodationApplicationEntity> {
    if (!user.hasRole(UserRole.CAS3_REFERRER)) {
      return CasResult.Unauthorised()
    }

    return validatedCasResult {
      val offenderDetails =
        when (
          val offenderDetailsResult = offenderService.getOffenderByCrn(crn, user.deliusUsername)
        ) {
          is AuthorisableActionResult.NotFound -> return@validatedCasResult "$.crn" hasSingleValidationError "doesNotExist"
          is AuthorisableActionResult.Unauthorised ->
            return@validatedCasResult "$.crn" hasSingleValidationError "userPermission"

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
        return@validatedCasResult fieldValidationError
      }

      val riskRatings: PersonRisks? = if (createWithRisks == true) {
        offenderRisksService.getPersonRisks(crn)
      } else {
        null
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
    }
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
  ): TemporaryAccommodationApplicationEntity = TemporaryAccommodationApplicationEntity(
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
    deletedAt = null,
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
    dutyToReferOutcome = null,
    prisonNameOnCreation = prisonName,
    personReleaseDate = null,
    name = "${offenderDetails.firstName} ${offenderDetails.surname}",
    isHistoryOfSexualOffence = null,
    isConcerningSexualBehaviour = null,
    isConcerningArsonBehaviour = null,
    prisonReleaseTypes = null,
    probationDeliveryUnit = null,
  )

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
      ?: return CasResult.NotFound(entityType = "application", applicationId.toString())

    if (application !is ApprovedPremisesApplicationEntity) {
      return CasResult.GeneralValidationError("onlyCas1Supported")
    }

    if (application.isWithdrawn) {
      return CasResult.Success(Unit)
    }

    val updatedApplication = application.apply {
      this.isWithdrawn = true
      this.withdrawalReason = withdrawalReason
      this.otherWithdrawalReason = if (withdrawalReason == WithdrawalReason.other.value) {
        otherReason
      } else {
        null
      }
    }
    applicationListener.preUpdate(updatedApplication)

    applicationRepository.save(updatedApplication)

    cas1ApplicationDomainEventService.applicationWithdrawn(updatedApplication, withdrawingUser = user)
    cas1ApplicationEmailService.applicationWithdrawn(updatedApplication, user)

    updatedApplication.assessments.map {
      assessmentService.updateCas1AssessmentWithdrawn(it.id, user)
    }

    return CasResult.Success(Unit)
  }

  fun getWithdrawableState(application: ApprovedPremisesApplicationEntity, user: UserEntity): WithdrawableState = WithdrawableState(
    withdrawable = !application.isWithdrawn,
    withdrawn = application.isWithdrawn,
    userMayDirectlyWithdraw = userAccessService.userMayWithdrawApplication(user, application),
  )

  fun updateTemporaryAccommodationApplication(
    applicationId: UUID,
    data: String,
  ): CasResult<ApplicationEntity> {
    val application = applicationRepository.findByIdOrNull(applicationId)?.let(jsonSchemaService::checkSchemaOutdated)
      ?: return CasResult.NotFound("Application", applicationId.toString())

    if (application !is TemporaryAccommodationApplicationEntity) {
      return CasResult.GeneralValidationError("onlyCas3Supported")
    }

    val user = userService.getUserForRequest()

    if (application.createdByUser != user) {
      return CasResult.Unauthorised()
    }

    if (!application.schemaUpToDate) {
      return CasResult.GeneralValidationError("The schema version is outdated")
    }

    if (application.deletedAt != null) {
      return CasResult.GeneralValidationError("This application has already been deleted")
    }

    if (application.submittedAt != null) {
      return CasResult.GeneralValidationError("This application has already been submitted")
    }

    application.apply {
      this.data = data
    }

    val savedApplication = applicationRepository.save(application)

    return CasResult.Success(savedApplication)
  }

  fun getArrivalDate(arrivalDate: LocalDate?): OffsetDateTime? {
    if (arrivalDate !== null) {
      return OffsetDateTime.of(arrivalDate, LocalTime.MIDNIGHT, ZoneOffset.UTC)
    }
    return null
  }

  private fun getPrisonName(personInfo: PersonInfoResult.Success.Full): String? {
    val prisonName = when (personInfo.inmateDetail?.custodyStatus) {
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
