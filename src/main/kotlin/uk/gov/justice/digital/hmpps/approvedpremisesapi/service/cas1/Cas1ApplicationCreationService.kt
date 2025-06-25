package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.transaction.Transactional
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1ApplicationTimelinessCategory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1ApplicationUserDetails
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SubmitApprovedPremisesApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ApDeliusContextApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ClientResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApAreaRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationTeamCodeEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationTeamCodeRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationJsonSchemaEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1ApplicationUserDetailsEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1ApplicationUserDetailsRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1CruManagementAreaRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.LockableApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.OfflineApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.OfflineApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationAutomaticEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationAutomaticRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas1.Cas1OffenderEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.ApprovedPremisesApplicationStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.ApprovedPremisesType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonRisks
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.ValidationErrors
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.asApprovedPremisesType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.OffenderDetailSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.validatedCasResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.InternalServerErrorProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.AuthorisableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.CasResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.AssessmentService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.JsonSchemaService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.OffenderRisksService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.OffenderService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.asCaseSummary
import java.time.Clock
import java.time.LocalDate
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

@Service
class Cas1ApplicationCreationService(
  private val applicationRepository: ApplicationRepository,
  private val jsonSchemaService: JsonSchemaService,
  private val offenderService: OffenderService,
  private val offenderRisksService: OffenderRisksService,
  private val assessmentService: AssessmentService,
  private val offlineApplicationRepository: OfflineApplicationRepository,
  private val apDeliusContextApiClient: ApDeliusContextApiClient,
  private val applicationTeamCodeRepository: ApplicationTeamCodeRepository,
  private val objectMapper: ObjectMapper,
  private val apAreaRepository: ApAreaRepository,
  private val cas1ApplicationDomainEventService: Cas1ApplicationDomainEventService,
  private val cas1ApplicationUserDetailsRepository: Cas1ApplicationUserDetailsRepository,
  private val cas1ApplicationEmailService: Cas1ApplicationEmailService,
  private val placementApplicationAutomaticRepository: PlacementApplicationAutomaticRepository,
  private val cas1ApplicationStatusService: Cas1ApplicationStatusService,
  private val clock: Clock,
  private val lockableApplicationRepository: LockableApplicationRepository,
  private val cas1CruManagementAreaRepository: Cas1CruManagementAreaRepository,
  private val cas1OffenderService: Cas1OffenderService,
) {

  fun createApprovedPremisesApplication(
    offenderDetails: OffenderDetailSummary,
    user: UserEntity,
    convictionId: Long?,
    deliusEventNumber: String?,
    offenceId: String?,
    createWithRisks: Boolean? = true,
  ) = validatedCasResult<ApprovedPremisesApplicationEntity> {
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

    val riskRatings = if (createWithRisks == true) {
      offenderRisksService.getPersonRisks(crn)
    } else {
      null
    }

    val createdApplication = applicationRepository.saveAndFlush(
      createApprovedPremisesApplicationEntity(
        crn,
        user,
        convictionId,
        deliusEventNumber,
        offenceId,
        riskRatings,
        cas1OffenderEntity = cas1OffenderService.getOrCreateOffender(offenderDetails.asCaseSummary(), riskRatings),
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
    cas1OffenderEntity: Cas1OffenderEntity,
    offenderDetails: OffenderDetailSummary,
  ): ApprovedPremisesApplicationEntity = ApprovedPremisesApplicationEntity(
    id = UUID.randomUUID(),
    crn = crn,
    createdByUser = user,
    data = null,
    document = null,
    schemaVersion = jsonSchemaService.getNewestSchema(ApprovedPremisesApplicationJsonSchemaEntity::class.java),
    createdAt = OffsetDateTime.now(),
    submittedAt = null,
    deletedAt = null,
    isWomensApplication = null,
    isEmergencyApplication = null,
    apType = ApprovedPremisesType.NORMAL,
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
    cruManagementArea = null,
    applicantUserDetails = null,
    caseManagerIsNotApplicant = null,
    caseManagerUserDetails = null,
    noticeType = null,
    licenceExpiryDate = null,
    cas1OffenderEntity = cas1OffenderEntity,
  )

  fun createOfflineApplication(offlineApplication: OfflineApplicationEntity) = offlineApplicationRepository.save(offlineApplication)

  @SuppressWarnings("CyclomaticComplexMethod", "ReturnCount")
  @Transactional
  fun submitApprovedPremisesApplication(
    applicationId: UUID,
    submitApplication: SubmitApprovedPremisesApplication,
    user: UserEntity,
    apAreaId: UUID,
  ): CasResult<ApplicationEntity> {
    lockableApplicationRepository.acquirePessimisticLock(applicationId)

    var application = applicationRepository.findByIdOrNull(
      applicationId,
    )?.let(jsonSchemaService::checkSchemaOutdated)
      ?: return CasResult.NotFound("ApprovedPremisesApplicationEntity", applicationId.toString())

    val serializedTranslatedDocument = objectMapper.writeValueAsString(submitApplication.translatedDocument)

    if (application.createdByUser != user) {
      return CasResult.Unauthorised()
    }

    if (application !is ApprovedPremisesApplicationEntity) {
      return CasResult.GeneralValidationError("onlyCas1Supported")
    }

    if (application.status != ApprovedPremisesApplicationStatus.STARTED) {
      return CasResult.GeneralValidationError("Only an application with the 'STARTED' status can be submitted")
    }

    if (submitApplication.isUsingLegacyApTypeFields && submitApplication.isUsingNewApTypeField) {
      return CasResult.GeneralValidationError("`isPipeApplication`/`isEsapApplication` should not be used in conjunction with `apType`")
    }

    val apType = when {
      submitApplication.isUsingLegacyApTypeFields -> when {
        submitApplication.isPipeApplication == true -> ApprovedPremisesType.PIPE
        submitApplication.isEsapApplication == true -> ApprovedPremisesType.ESAP
        else -> ApprovedPremisesType.NORMAL
      }

      submitApplication.isUsingNewApTypeField -> submitApplication.apType!!.asApprovedPremisesType()
      else -> ApprovedPremisesType.NORMAL
    }

    if (application.submittedAt != null) {
      return CasResult.GeneralValidationError("This application has already been submitted")
    }

    if (submitApplication.caseManagerIsNotApplicant == true && submitApplication.caseManagerUserDetails == null) {
      return CasResult.GeneralValidationError("caseManagerUserDetails must be provided if caseManagerIsNotApplicant is true")
    }

    if (!application.schemaUpToDate) {
      return CasResult.GeneralValidationError("The schema version is outdated")
    }

    val validationErrors = ValidationErrors()
    val applicationData = application.data

    if (applicationData == null) {
      validationErrors["$.data"] = "empty"
    } else if (!jsonSchemaService.validate(application.schemaVersion, applicationData)) {
      validationErrors["$.data"] = "invalid"
    }

    if (validationErrors.any()) {
      return CasResult.FieldValidationError(validationErrors)
    }

    val inmateDetails = application.nomsNumber?.let { nomsNumber ->
      when (val inmateDetailsResult = offenderService.getInmateDetailByNomsNumber(application.crn, nomsNumber)) {
        is AuthorisableActionResult.Success -> inmateDetailsResult.entity
        else -> null
      }
    }

    val now = OffsetDateTime.now(clock)

    val apArea = apAreaRepository.findByIdOrNull(apAreaId)!!
    application.apply {
      isWomensApplication = submitApplication.isWomensApplication
      this.isEmergencyApplication = isEmergencyApplication
      this.apType = apType
      submittedAt = now
      document = serializedTranslatedDocument
      releaseType = submitApplication.releaseType.toString()
      targetLocation = submitApplication.targetLocation
      arrivalDate = getArrivalDate(submitApplication.arrivalDate)
      sentenceType = submitApplication.sentenceType.toString()
      situation = submitApplication.situation?.toString()
      inmateInOutStatusOnSubmission = inmateDetails?.custodyStatus?.name
      this.apArea = apArea
      this.cruManagementArea = if (submitApplication.isWomensApplication == true) {
        cas1CruManagementAreaRepository.findByIdOrNull(Cas1CruManagementAreaRepository.WOMENS_ESTATE_ID)
          ?: throw InternalServerErrorProblem("Could not find women's estate CRU Management Area Entity with ID ${Cas1CruManagementAreaRepository.WOMENS_ESTATE_ID}")
      } else {
        apArea.defaultCruManagementArea
      }
      this.applicantUserDetails = upsertCas1ApplicationUserDetails(this.applicantUserDetails, submitApplication.applicantUserDetails)
      this.caseManagerIsNotApplicant = submitApplication.caseManagerIsNotApplicant
      this.caseManagerUserDetails = upsertCas1ApplicationUserDetails(
        existingEntry = this.caseManagerUserDetails,
        updatedValues = if (submitApplication.caseManagerIsNotApplicant == true) {
          submitApplication.caseManagerUserDetails
        } else {
          null
        },
      )
      this.noticeType = getNoticeType(submitApplication.noticeType, submitApplication.isEmergencyApplication, this)
      this.licenceExpiryDate = submitApplication.licenseExpiryDate
    }

    cas1ApplicationDomainEventService.applicationSubmitted(application, submitApplication, user.deliusUsername)
    assessmentService.createApprovedPremisesAssessment(application)

    application = applicationRepository.save(application)

    cas1ApplicationEmailService.applicationSubmitted(application)

    if (application.arrivalDate != null) {
      placementApplicationAutomaticRepository.save(
        PlacementApplicationAutomaticEntity(
          id = UUID.randomUUID(),
          application = application,
          submittedAt = application.submittedAt!!,
          expectedArrivalDate = application.arrivalDate!!,
        ),
      )
    }

    return CasResult.Success(application)
  }

  @SuppressWarnings("ReturnCount")
  @Transactional
  fun updateApprovedPremisesApplication(
    applicationId: UUID,
    updateFields: Cas1ApplicationUpdateFields,
    userForRequest: UserEntity,
  ): CasResult<ApprovedPremisesApplicationEntity> {
    val application = applicationRepository.findByIdOrNull(applicationId)?.let(jsonSchemaService::checkSchemaOutdated)
      ?: return CasResult.NotFound("Application", applicationId.toString())

    if (application !is ApprovedPremisesApplicationEntity) {
      return CasResult.GeneralValidationError("onlyCas1Supported")
    }

    if (updateFields.isUsingLegacyApTypeFields && updateFields.isUsingNewApTypeField) {
      return CasResult.GeneralValidationError(
        "`isPipeApplication`/`isEsapApplication` should not be used in conjunction with `apType`",
      )
    }

    if (application.createdByUser.id != userForRequest.id) {
      return CasResult.Unauthorised()
    }

    if (!application.schemaUpToDate) {
      return CasResult.GeneralValidationError("The schema version is outdated")
    }

    if (application.submittedAt != null) {
      return CasResult.GeneralValidationError("This application has already been submitted")
    }

    application.apply {
      this.isInapplicable = updateFields.isInapplicable
      this.isWomensApplication = updateFields.isWomensApplication
      this.isEmergencyApplication = updateFields.isEmergencyApplication
      this.apType = updateFields.deriveApType()
      this.releaseType = updateFields.releaseType
      this.arrivalDate = if (updateFields.arrivalDate !== null) {
        OffsetDateTime.of(updateFields.arrivalDate, LocalTime.MIDNIGHT, ZoneOffset.UTC)
      } else {
        null
      }
      this.data = updateFields.data
      this.noticeType = getNoticeType(updateFields.noticeType, updateFields.isEmergencyApplication, this)
    }

    cas1ApplicationStatusService.unsubmittedApplicationUpdated(application)

    val savedApplication = applicationRepository.save(application)

    return CasResult.Success(savedApplication)
  }

  private fun getArrivalDate(arrivalDate: LocalDate?): OffsetDateTime? {
    if (arrivalDate !== null) {
      return OffsetDateTime.of(arrivalDate, LocalTime.MIDNIGHT, ZoneOffset.UTC)
    }
    return null
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

  private fun getNoticeType(noticeType: Cas1ApplicationTimelinessCategory?, isEmergencyApplication: Boolean?, application: ApprovedPremisesApplicationEntity) = noticeType
    ?: if (isEmergencyApplication == true) {
      Cas1ApplicationTimelinessCategory.emergency
    } else if (application.isShortNoticeApplication() == true) {
      Cas1ApplicationTimelinessCategory.shortNotice
    } else {
      Cas1ApplicationTimelinessCategory.standard
    }

  private val Cas1ApplicationUpdateFields.isUsingLegacyApTypeFields: Boolean
    get() = isPipeApplication != null || isEsapApplication != null

  private val Cas1ApplicationUpdateFields.isUsingNewApTypeField: Boolean
    get() = apType != null

  private fun Cas1ApplicationUpdateFields.deriveApType() = when {
    this.isUsingLegacyApTypeFields -> when {
      this.isPipeApplication == true -> ApprovedPremisesType.PIPE
      this.isEsapApplication == true -> ApprovedPremisesType.ESAP
      else -> ApprovedPremisesType.NORMAL
    }
    this.isUsingNewApTypeField -> this.apType!!.asApprovedPremisesType()
    else -> ApprovedPremisesType.NORMAL
  }

  private val SubmitApprovedPremisesApplication.isUsingLegacyApTypeFields: Boolean
    get() = isPipeApplication != null || isEsapApplication != null

  private val SubmitApprovedPremisesApplication.isUsingNewApTypeField: Boolean
    get() = apType != null

  data class Cas1ApplicationUpdateFields(
    val isWomensApplication: Boolean?,
    @Deprecated("use apType")
    val isPipeApplication: Boolean?,
    @Deprecated("use noticeType")
    val isEmergencyApplication: Boolean?,
    @Deprecated("use apType")
    val isEsapApplication: Boolean?,
    val apType: ApType?,
    val releaseType: String?,
    val arrivalDate: LocalDate?,
    val data: String,
    val isInapplicable: Boolean?,
    val noticeType: Cas1ApplicationTimelinessCategory?,
  )
}
