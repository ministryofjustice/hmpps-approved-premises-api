package uk.gov.justice.digital.hmpps.approvedpremisesapi.service

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationJsonSchemaEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.OfflineApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.OfflineApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.ValidationErrors
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.validated
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.AuthorisableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.ValidatableActionResult
import java.time.OffsetDateTime
import java.util.UUID

@Service
class ApplicationService(
  private val userRepository: UserRepository,
  private val applicationRepository: ApplicationRepository,
  private val jsonSchemaService: JsonSchemaService,
  private val offenderService: OffenderService,
  private val userService: UserService,
  private val assessmentService: AssessmentService,
  private val jsonLogicService: JsonLogicService,
  private val offlineApplicationRepository: OfflineApplicationRepository
) {
  fun getAllApplicationsForUsername(userDistinguishedName: String, serviceName: ServiceName): List<ApplicationEntity> {
    val userEntity = userRepository.findByDeliusUsername(userDistinguishedName)
      ?: return emptyList()

    val entityType = if (serviceName == ServiceName.approvedPremises) {
      ApprovedPremisesApplicationEntity::class.java
    } else {
      TemporaryAccommodationApplicationEntity::class.java
    }

    val applications = if (userEntity.hasAnyRole(UserRole.WORKFLOW_MANAGER, UserRole.ASSESSOR, UserRole.MATCHER, UserRole.MANAGER)) {
      applicationRepository.findAll()
    } else {
      applicationRepository.findAllByCreatedByUser_Id(userEntity.id, entityType)
    }

    return applications
      .map(jsonSchemaService::checkSchemaOutdated)
      .filter {
        offenderService.canAccessOffender(userDistinguishedName, it.crn)
      }
  }

  fun getAllOfflineApplicationsForUsername(deliusUsername: String, serviceName: ServiceName): List<OfflineApplicationEntity> {
    val userEntity = userRepository.findByDeliusUsername(deliusUsername)
      ?: return emptyList()

    val applications = if (userEntity.hasAnyRole(UserRole.WORKFLOW_MANAGER, UserRole.ASSESSOR, UserRole.MATCHER, UserRole.MANAGER)) {
      offlineApplicationRepository.findAllWhereService(serviceName.value)
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

    if (userEntity.id == applicationEntity.createdByUser.id || userEntity.hasAnyRole(UserRole.WORKFLOW_MANAGER, UserRole.ASSESSOR, UserRole.MATCHER, UserRole.MANAGER)) {
      return AuthorisableActionResult.Success(jsonSchemaService.checkSchemaOutdated(applicationEntity))
    }

    return AuthorisableActionResult.Unauthorised()
  }

  fun getOfflineApplicationForUsername(applicationId: UUID, deliusUsername: String): AuthorisableActionResult<OfflineApplicationEntity> {
    val applicationEntity = offlineApplicationRepository.findByIdOrNull(applicationId)
      ?: return AuthorisableActionResult.NotFound()

    val userEntity = userRepository.findByDeliusUsername(deliusUsername)
      ?: throw RuntimeException("Could not get user")

    if (userEntity.hasAnyRole(UserRole.WORKFLOW_MANAGER, UserRole.ASSESSOR, UserRole.MATCHER, UserRole.MANAGER) &&
      offenderService.canAccessOffender(deliusUsername, applicationEntity.crn)
    ) {
      return AuthorisableActionResult.Success(applicationEntity)
    }

    return AuthorisableActionResult.Unauthorised()
  }

  fun createApplication(crn: String, username: String, jwt: String, service: String, convictionId: Long?, deliusEventNumber: String?, offenceId: String?) = validated<ApplicationEntity> {
    if (service != ServiceName.approvedPremises.value) {
      "$.service" hasValidationError "onlyCas1Supported"
      return fieldValidationError
    }

    when (offenderService.getOffenderByCrn(crn, username)) {
      is AuthorisableActionResult.NotFound -> return "$.crn" hasSingleValidationError "doesNotExist"
      is AuthorisableActionResult.Unauthorised -> return "$.crn" hasSingleValidationError "userPermission"
      is AuthorisableActionResult.Success -> Unit
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

    val user = userService.getUserForRequest()

    val riskRatingsResult = offenderService.getRiskByCrn(crn, jwt, username)

    val riskRatings = when (riskRatingsResult) {
      is AuthorisableActionResult.NotFound -> return "$.crn" hasSingleValidationError "doesNotExist"
      is AuthorisableActionResult.Unauthorised -> return "$.crn" hasSingleValidationError "userPermission"
      is AuthorisableActionResult.Success -> riskRatingsResult.entity
    }

    val createdApplication = applicationRepository.save(
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
        convictionId = convictionId!!,
        eventNumber = deliusEventNumber!!,
        offenceId = offenceId!!,
        schemaUpToDate = true,
        riskRatings = riskRatings
      )
    )

    return success(createdApplication.apply { schemaUpToDate = true })
  }

  fun updateApplication(applicationId: UUID, data: String, username: String): AuthorisableActionResult<ValidatableActionResult<ApplicationEntity>> {
    val application = applicationRepository.findByIdOrNull(applicationId)?.let(jsonSchemaService::checkSchemaOutdated)
      ?: return AuthorisableActionResult.NotFound()

    val user = userService.getUserForRequest()

    if (application.createdByUser != user) {
      return AuthorisableActionResult.Unauthorised()
    }

    if (!application.schemaUpToDate) {
      return AuthorisableActionResult.Success(
        ValidatableActionResult.GeneralValidationError("The schema version is outdated")
      )
    }

    if (application.submittedAt != null) {
      return AuthorisableActionResult.Success(
        ValidatableActionResult.GeneralValidationError("This application has already been submitted")
      )
    }

    application.data = data

    val savedApplication = applicationRepository.save(application)

    return AuthorisableActionResult.Success(
      ValidatableActionResult.Success(savedApplication)
    )
  }

  fun submitApplication(applicationId: UUID, serializedTranslatedDocument: String, username: String): AuthorisableActionResult<ValidatableActionResult<ApplicationEntity>> {
    var application = applicationRepository.findByIdOrNull(applicationId)?.let(jsonSchemaService::checkSchemaOutdated)
      ?: return AuthorisableActionResult.NotFound()

    val user = userService.getUserForRequest()

    if (application.createdByUser != user) {
      return AuthorisableActionResult.Unauthorised()
    }

    if (application !is ApprovedPremisesApplicationEntity) {
      return AuthorisableActionResult.Success(
        ValidatableActionResult.GeneralValidationError("onlyCas1Supported")
      )
    }

    if (application.submittedAt != null) {
      return AuthorisableActionResult.Success(
        ValidatableActionResult.GeneralValidationError("This application has already been submitted")
      )
    }

    if (!application.schemaUpToDate) {
      return AuthorisableActionResult.Success(
        ValidatableActionResult.GeneralValidationError("The schema version is outdated")
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
        ValidatableActionResult.FieldValidationError(validationErrors)
      )
    }

    val schema = application.schemaVersion as? ApprovedPremisesApplicationJsonSchemaEntity
      ?: throw RuntimeException("Incorrect type of JSON schema referenced by AP Application")

    application.apply {
      isWomensApplication = jsonLogicService.resolveBoolean(schema.isWomensJsonLogicRule, applicationData!!)
      isPipeApplication = jsonLogicService.resolveBoolean(schema.isPipeJsonLogicRule, applicationData)
      submittedAt = OffsetDateTime.now()
      document = serializedTranslatedDocument
    }

    assessmentService.createAssessment(application)

    application = applicationRepository.save(application)

    return AuthorisableActionResult.Success(
      ValidatableActionResult.Success(application)
    )
  }
}
