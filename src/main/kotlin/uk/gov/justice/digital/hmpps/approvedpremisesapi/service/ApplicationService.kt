package uk.gov.justice.digital.hmpps.approvedpremisesapi.service

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.community.OffenderDetailSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.prisonsapi.InmateStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.LockableApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.ApprovedPremisesApplicationStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonInfoResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonRisks
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.validatedCasResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.AuthorisableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.CasResult
import java.time.OffsetDateTime
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
  private val applicationRepository: ApplicationRepository,
  private val offenderService: OffenderService,
  private val offenderRisksService: OffenderRisksService,
  private val userService: UserService,
  private val lockableApplicationRepository: LockableApplicationRepository,
) {
  fun getApplication(applicationId: UUID) = applicationRepository.findByIdOrNull(applicationId)

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
    lockableApplicationRepository.acquirePessimisticLock(applicationId)
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
