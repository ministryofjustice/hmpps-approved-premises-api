package uk.gov.justice.digital.hmpps.approvedpremisesapi.service

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesPlacementApplicationJsonSchemaEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentDecision
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementRequestEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.validated
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.AuthorisableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.ValidatableActionResult
import java.time.OffsetDateTime
import java.util.UUID

@Service
class PlacementApplicationService(
  private val placementApplicationRepository: PlacementApplicationRepository,
  private val jsonSchemaService: JsonSchemaService,
  private val userService: UserService,
) {

  fun createApplication(
    application: ApplicationEntity,
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
      ),
    )

    return success(createdApplication.apply { schemaUpToDate = true })
  }

  fun getApplication(id: UUID): AuthorisableActionResult<PlacementApplicationEntity> {
    val placementApplication = placementApplicationRepository.findByIdOrNull(id) ?: return AuthorisableActionResult.NotFound()
    val latestSchema = jsonSchemaService.getNewestSchema(ApprovedPremisesPlacementApplicationJsonSchemaEntity::class.java)

    placementApplication.schemaUpToDate = placementApplication.schemaVersion.id == latestSchema.id

    return AuthorisableActionResult.Success(placementApplication)
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

  fun submitApplication(id: UUID, translatedDocument: String): AuthorisableActionResult<ValidatableActionResult<PlacementApplicationEntity>> {
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
    }

    val savedApplication = placementApplicationRepository.save(placementApplicationEntity)

    return AuthorisableActionResult.Success(
      ValidatableActionResult.Success(savedApplication),
    )
  }

  fun getAllReallocatable(): List<PlacementApplicationEntity> {
    return placementApplicationRepository.findAllByReallocatedAtNullAndDecisionNull()
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
