package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.service

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.transaction.Transactional
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApplicationStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas3SuitableApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.Cas3SubmitApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.community.OffenderDetailSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.prisonsapi.InmateStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.LockableApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ProbationDeliveryUnitRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ProbationRegionRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonInfoResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonRisks
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.ValidationErrors
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.validatedCasResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.AuthorisableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.CasResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.AssessmentService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.OffenderRisksService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.OffenderService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserAccessService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserService
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

@Service
class Cas3ApplicationService(
  private val applicationRepository: ApplicationRepository,
  private val temporaryAccommodationApplicationRepository: TemporaryAccommodationApplicationRepository,
  private val lockableApplicationRepository: LockableApplicationRepository,
  private val probationDeliveryUnitRepository: ProbationDeliveryUnitRepository,
  private val probationRegionRepository: ProbationRegionRepository,
  private val userRepository: UserRepository,
  private val userService: UserService,
  private val userAccessService: UserAccessService,
  private val assessmentService: AssessmentService,
  private val cas3DomainEventService: Cas3DomainEventService,
  private val offenderService: OffenderService,
  private val offenderRisksService: OffenderRisksService,
  private val objectMapper: ObjectMapper,
) {
  fun getApplicationSummariesForUser(user: UserEntity): List<ApplicationSummary> = applicationRepository.findAllTemporaryAccommodationSummariesCreatedByUser(user.id)

  fun getSuitableApplicationByCrn(crn: String): Cas3SuitableApplication? {
    @SuppressWarnings("MagicNumber")
    val suitableStatusesAsc = mapOf(
      ApplicationStatus.requestedFurtherInformation to 0,
      ApplicationStatus.submitted to 1,
    )

    return temporaryAccommodationApplicationRepository.findByCrn(crn)
      .filter { it.getStatus() in suitableStatusesAsc.keys }
      .maxWithOrNull(compareBy<TemporaryAccommodationApplicationEntity> { suitableStatusesAsc[it.getStatus()] }.thenBy { it.submittedAt })
      ?.let {
        Cas3SuitableApplication(
          id = it.id,
          applicationStatus = it.getStatus(),
        )
      }
  }

  @Suppress("TooGenericExceptionThrown")
  fun getApplicationForUsername(
    applicationId: UUID,
    userDistinguishedName: String,
  ): CasResult<TemporaryAccommodationApplicationEntity> {
    val applicationEntity = temporaryAccommodationApplicationRepository.findByIdOrNull(applicationId)
      ?: return CasResult.NotFound("Application", applicationId.toString())

    if (applicationEntity.deletedAt != null) {
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

  fun createApplication(
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
        createApplicationEntity(
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

  @SuppressWarnings("ReturnCount")
  fun updateApplication(
    applicationId: UUID,
    data: String,
  ): CasResult<TemporaryAccommodationApplicationEntity> {
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

  @SuppressWarnings("ReturnCount")
  @Transactional
  fun submitApplication(
    applicationId: UUID,
    submitApplication: Cas3SubmitApplication,
  ): CasResult<ApplicationEntity> {
    lockableApplicationRepository.acquirePessimisticLock(applicationId)
    var application =
      applicationRepository.findByIdOrNull(
        applicationId,
      ) ?: return CasResult.NotFound("TemporaryAccommodationApplicationEntity", applicationId.toString())

    if (application.deletedAt != null) {
      return CasResult.GeneralValidationError("This application has already been deleted")
    }

    val serializedTranslatedDocument = objectMapper.writeValueAsString(submitApplication.translatedDocument)

    val user = userService.getUserForRequest()

    if (application.createdByUser != user) {
      return CasResult.Unauthorised()
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

    (application as TemporaryAccommodationApplicationEntity).apply {
      submittedAt = OffsetDateTime.now()
      document = serializedTranslatedDocument
      arrivalDate = OffsetDateTime.of(submitApplication.arrivalDate, LocalTime.MIDNIGHT, ZoneOffset.UTC)
      isRegisteredSexOffender = submitApplication.isRegisteredSexOffender
      isHistoryOfSexualOffence = submitApplication.isHistoryOfSexualOffence
      isConcerningSexualBehaviour = submitApplication.isConcerningSexualBehaviour
      needsAccessibleProperty = submitApplication.needsAccessibleProperty
      hasHistoryOfArson = submitApplication.hasHistoryOfArson
      isConcerningArsonBehaviour = submitApplication.isConcerningArsonBehaviour
      isDutyToReferSubmitted = submitApplication.isDutyToReferSubmitted
      dutyToReferSubmissionDate = submitApplication.dutyToReferSubmissionDate
      dutyToReferOutcome = submitApplication.dutyToReferOutcome
      isEligible = submitApplication.isApplicationEligible
      eligibilityReason = submitApplication.eligibilityReason
      dutyToReferLocalAuthorityAreaName = submitApplication.dutyToReferLocalAuthorityAreaName
      personReleaseDate = submitApplication.personReleaseDate
      prisonReleaseTypes = submitApplication.prisonReleaseTypes?.joinToString(",")
      probationRegion = when (submitApplication.outOfRegionProbationRegionId) {
        null -> user.probationRegion
        else -> probationRegionRepository.findByIdOrNull(submitApplication.outOfRegionProbationRegionId)!!
      }
      probationDeliveryUnit = when (submitApplication.outOfRegionPduId) {
        null -> probationDeliveryUnitRepository.findByIdOrNull(submitApplication.probationDeliveryUnitId)
        else -> probationDeliveryUnitRepository.findByIdOrNull(submitApplication.outOfRegionPduId)
      }
      previousReferralProbationRegion = submitApplication.outOfRegionProbationRegionId?.let { user.probationRegion }
      previousReferralProbationDeliveryUnit = submitApplication.outOfRegionPduId?.let { probationDeliveryUnitRepository.findByIdOrNull(submitApplication.probationDeliveryUnitId)!! }
    }

    assessmentService.createTemporaryAccommodationAssessment(application, submitApplication.summaryData!!)

    application = applicationRepository.save(application)

    cas3DomainEventService.saveReferralSubmittedEvent(application)

    return CasResult.Success(application)
  }

  @Transactional
  fun markApplicationAsDeleted(applicationId: UUID): CasResult<Unit> {
    val user = userService.getUserForRequest()
    val application = applicationRepository.findByIdOrNull(applicationId)
      ?: return CasResult.NotFound("TemporaryAccommodationApplication", applicationId.toString())

    if (!isUserAuthorizedToAccessApplication(user, application)) {
      return CasResult.Unauthorised()
    }

    return if (application.submittedAt == null) {
      markAsDeleted(application, user)
    } else {
      CasResult.GeneralValidationError("Cannot mark as deleted: temporary accommodation application already submitted.")
    }
  }

  private fun createApplicationEntity(
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

  private fun getPrisonName(personInfo: PersonInfoResult.Success.Full): String? {
    val prisonName = when (personInfo.inmateDetail?.custodyStatus) {
      InmateStatus.IN,
      InmateStatus.TRN,
      -> {
        personInfo.inmateDetail.assignedLivingUnit?.agencyName ?: personInfo.inmateDetail.assignedLivingUnit?.agencyId
      }
      else -> null
    }
    return prisonName
  }

  private fun isUserAuthorizedToAccessApplication(user: UserEntity, application: ApplicationEntity): Boolean = userAccessService.userCanAccessTemporaryAccommodationApplication(user, application)

  private fun markAsDeleted(application: ApplicationEntity, user: UserEntity): CasResult<Unit> {
    application.deletedAt = OffsetDateTime.now()
    applicationRepository.saveAndFlush(application)
    cas3DomainEventService.saveDraftReferralDeletedEvent(application, user)
    return CasResult.Success(Unit)
  }
}
