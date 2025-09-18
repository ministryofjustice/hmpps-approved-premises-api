package uk.gov.justice.digital.hmpps.approvedpremisesapi.service

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.community.OffenderDetailSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.prisonsapi.InmateStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.OfflineApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.OfflineApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserPermission
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.ApprovedPremisesApplicationStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonInfoResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonRisks
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.validatedCasResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.AuthorisableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.CasResult
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
  private val offenderService: OffenderService,
  private val offenderRisksService: OffenderRisksService,
  private val userService: UserService,
  private val offlineApplicationRepository: OfflineApplicationRepository,
  private val userAccessService: UserAccessService,
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
      CasResult.Success(applicationEntity)
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
      offenderService.canAccessOffender(deliusUsername, applicationEntity.crn)
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

      success(createdApplication)
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
    createdAt = OffsetDateTime.now(),
    submittedAt = null,
    deletedAt = null,
    convictionId = convictionId!!,
    eventNumber = deliusEventNumber!!,
    offenceId = offenceId!!,
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
    previousReferralProbationRegion = null,
    previousReferralProbationDeliveryUnit = null,
  )

  fun updateApprovedPremisesApplicationStatus(applicationId: UUID, status: ApprovedPremisesApplicationStatus) {
    applicationRepository.updateStatus(applicationId, status)
  }

  fun updateTemporaryAccommodationApplication(
    applicationId: UUID,
    data: String,
  ): CasResult<ApplicationEntity> {
    val application = applicationRepository.findByIdOrNull(applicationId)
      ?: return CasResult.NotFound("Application", applicationId.toString())

    if (application !is TemporaryAccommodationApplicationEntity) {
      return CasResult.GeneralValidationError("onlyCas3Supported")
    }

    val user = userService.getUserForRequest()

    if (application.createdByUser != user) {
      return CasResult.Unauthorised()
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
