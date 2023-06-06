package uk.gov.justice.digital.hmpps.approvedpremisesapi.service

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesPlacementApplicationJsonSchemaEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentDecision
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementDateEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementDateRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.ValidationErrors
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.validated
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.AuthorisableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.ValidatableActionResult
import java.time.OffsetDateTime
import java.util.UUID
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PlacementDates as ApiPlacementDates
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PlacementType as ApiPlacementType

@Service
class PlacementApplicationService(
  private val placementApplicationRepository: PlacementApplicationRepository,
  private val jsonSchemaService: JsonSchemaService,
  private val userService: UserService,
  private val placementDateRepository: PlacementDateRepository,
) {

  fun getVisiblePlacementApplicationsForUser(user: UserEntity): List<PlacementApplicationEntity> {
    return placementApplicationRepository.findAllByAllocatedToUser_IdAndReallocatedAtNull(user.id)
  }

  fun createApplication(
    application: ApprovedPremisesApplicationEntity,
    user: UserEntity,
  ) = validated<PlacementApplicationEntity> {
    val assessment = application.getLatestAssessment()

    if (assessment?.decision !== AssessmentDecision.ACCEPTED) {
      return generalError("You cannot request a placement request for an application that has not been approved")
    }

    val createdApplication = placementApplicationRepository.save(
      PlacementApplicationEntity(
        id = UUID.randomUUID(),
        application = application,
        createdByUser = user,
        data = null,
        document = null,
        schemaVersion = jsonSchemaService.getNewestSchema(ApprovedPremisesPlacementApplicationJsonSchemaEntity::class.java),
        createdAt = OffsetDateTime.now(),
        submittedAt = null,
        schemaUpToDate = true,
        allocatedToUser = null,
        decision = null,
        allocatedAt = null,
        reallocatedAt = null,
        placementDates = mutableListOf(),
        placementType = null,
      ),
    )

    return success(createdApplication.apply { schemaUpToDate = true })
  }

  fun getApplication(id: UUID): AuthorisableActionResult<PlacementApplicationEntity> {
    val placementApplication = placementApplicationRepository.findByIdOrNull(id) ?: return AuthorisableActionResult.NotFound()

    return AuthorisableActionResult.Success(setSchemaUpToDate(placementApplication))
  }

  fun getPlacementApplicationForApplicationId(applicationId: UUID): AuthorisableActionResult<PlacementApplicationEntity> {
    val placementApplication = placementApplicationRepository.findByApplicationId(applicationId) ?: return AuthorisableActionResult.NotFound()

    return AuthorisableActionResult.Success(setSchemaUpToDate(placementApplication))
  }

  fun reallocateApplication(assigneeUser: UserEntity, application: ApprovedPremisesApplicationEntity): AuthorisableActionResult<ValidatableActionResult<PlacementApplicationEntity>> {
    val currentPlacementApplication = placementApplicationRepository.findByApplication_IdAndReallocatedAtNull(application.id)
      ?: return AuthorisableActionResult.NotFound()

    if (currentPlacementApplication.decision != null) {
      return AuthorisableActionResult.Success(
        ValidatableActionResult.GeneralValidationError("This placement application has already been completed"),
      )
    }

    if (!assigneeUser.hasRole(UserRole.CAS1_ASSESSOR)) {
      return AuthorisableActionResult.Success(
        ValidatableActionResult.FieldValidationError(ValidationErrors().apply { this["$.userId"] = "lackingAssessorRole" }),
      )
    }

    currentPlacementApplication.reallocatedAt = OffsetDateTime.now()
    placementApplicationRepository.save(currentPlacementApplication)

    // Make the timestamp precision less precise, so we don't have any issues with microsecond resolution in tests
    val dateTimeNow = OffsetDateTime.now().withNano(0)

    val newPlacementApplication = placementApplicationRepository.save(
      currentPlacementApplication.copy(
        id = UUID.randomUUID(),
        reallocatedAt = null,
        allocatedToUser = assigneeUser,
        createdAt = dateTimeNow,
      ),
    )

    val newPlacementDates = placementDateRepository.saveAll(
      currentPlacementApplication.placementDates.map {
        PlacementDateEntity(
          id = UUID.randomUUID(),
          expectedArrival = it.expectedArrival,
          duration = it.duration,
          placementApplication = newPlacementApplication,
          createdAt = dateTimeNow,
        )
      },
    )

    newPlacementApplication.placementDates = newPlacementDates

    return AuthorisableActionResult.Success(
      ValidatableActionResult.Success(
        newPlacementApplication,
      ),
    )
  }

  fun updateApplication(id: UUID, data: String): AuthorisableActionResult<ValidatableActionResult<PlacementApplicationEntity>> {
    val placementApplicationAuthorisationResult = getApplicationForUpdateOrSubmit(id)

    when (placementApplicationAuthorisationResult) {
      is AuthorisableActionResult.NotFound -> return AuthorisableActionResult.NotFound()
      is AuthorisableActionResult.Unauthorised -> return AuthorisableActionResult.Unauthorised()
      is AuthorisableActionResult.Success -> Unit
    }

    val placementApplicationValidationResult = confirmApplicationCanBeUpdatedOrSubmitted(placementApplicationAuthorisationResult.entity)

    if (placementApplicationValidationResult !is ValidatableActionResult.Success) {
      return AuthorisableActionResult.Success(placementApplicationValidationResult)
    }

    val placementApplicationEntity = placementApplicationValidationResult.entity

    placementApplicationEntity.data = data

    val savedApplication = placementApplicationRepository.save(placementApplicationEntity)

    return AuthorisableActionResult.Success(
      ValidatableActionResult.Success(savedApplication),
    )
  }

  fun submitApplication(id: UUID, translatedDocument: String, apiPlacementType: ApiPlacementType, apiPlacementDates: List<ApiPlacementDates>): AuthorisableActionResult<ValidatableActionResult<PlacementApplicationEntity>> {
    val placementApplicationAuthorisationResult = getApplicationForUpdateOrSubmit(id)

    when (placementApplicationAuthorisationResult) {
      is AuthorisableActionResult.NotFound -> return AuthorisableActionResult.NotFound()
      is AuthorisableActionResult.Unauthorised -> return AuthorisableActionResult.Unauthorised()
      is AuthorisableActionResult.Success -> Unit
    }

    val placementApplicationValidationResult = confirmApplicationCanBeUpdatedOrSubmitted(placementApplicationAuthorisationResult.entity)

    if (placementApplicationValidationResult !is ValidatableActionResult.Success) {
      return AuthorisableActionResult.Success(placementApplicationValidationResult)
    }

    val placementApplicationEntity = placementApplicationValidationResult.entity

    val requiredQualifications = placementApplicationEntity.application.getRequiredQualifications()

    val allocatedUser = userService.getUserForAllocation(requiredQualifications)
      ?: throw RuntimeException("No Users with all of required qualifications (${requiredQualifications.joinToString(", ")}) could be found")

    placementApplicationEntity.apply {
      document = translatedDocument
      allocatedToUser = allocatedUser
      submittedAt = OffsetDateTime.now()
      allocatedAt = OffsetDateTime.now()
      placementType = getPlacementType(apiPlacementType)
    }

    val savedApplication = placementApplicationRepository.save(placementApplicationEntity)

    val placementDates = apiPlacementDates.map {
      PlacementDateEntity(
        id = UUID.randomUUID(),
        expectedArrival = it.expectedArrival,
        duration = it.duration,
        placementApplication = placementApplicationEntity,
        createdAt = OffsetDateTime.now(),
      )
    }.toMutableList()

    placementDateRepository.saveAll(placementDates)

    placementApplicationEntity.placementDates = placementDates

    return AuthorisableActionResult.Success(
      ValidatableActionResult.Success(savedApplication),
    )
  }

  private fun getPlacementType(apiPlacementType: ApiPlacementType): PlacementType {
    return when (apiPlacementType) {
      ApiPlacementType.additionalPlacement -> PlacementType.ADDITIONAL_PLACEMENT
      ApiPlacementType.rotl -> PlacementType.ROTL
      ApiPlacementType.releaseFollowingDecision -> PlacementType.RELEASE_FOLLOWING_DECISION
    }
  }

  fun getAllReallocatable(): List<PlacementApplicationEntity> {
    return placementApplicationRepository.findAllByReallocatedAtNullAndDecisionNull()
  }

  private fun setSchemaUpToDate(placementApplicationEntity: PlacementApplicationEntity): PlacementApplicationEntity {
    val latestSchema = jsonSchemaService.getNewestSchema(ApprovedPremisesPlacementApplicationJsonSchemaEntity::class.java)

    placementApplicationEntity.schemaUpToDate = placementApplicationEntity.schemaVersion.id == latestSchema.id

    return placementApplicationEntity
  }

  private fun getApplicationForUpdateOrSubmit(id: UUID): AuthorisableActionResult<PlacementApplicationEntity> {
    val placementApplication = placementApplicationRepository.findByIdOrNull(id) ?: return AuthorisableActionResult.NotFound()
    val user = userService.getUserForRequest()

    if (placementApplication.createdByUser != user) {
      return AuthorisableActionResult.Unauthorised()
    }

    return AuthorisableActionResult.Success(placementApplication)
  }

  private fun confirmApplicationCanBeUpdatedOrSubmitted(placementApplicationEntity: PlacementApplicationEntity): ValidatableActionResult<PlacementApplicationEntity> {
    val latestSchema = jsonSchemaService.getNewestSchema(ApprovedPremisesPlacementApplicationJsonSchemaEntity::class.java)
    placementApplicationEntity.schemaUpToDate = placementApplicationEntity.schemaVersion.id == latestSchema.id

    if (!placementApplicationEntity.schemaUpToDate) {
      return ValidatableActionResult.GeneralValidationError("The schema version is outdated")
    }

    if (placementApplicationEntity.submittedAt != null) {
      return ValidatableActionResult.GeneralValidationError("This application has already been submitted")
    }

    return ValidatableActionResult.Success(placementApplicationEntity)
  }
}
