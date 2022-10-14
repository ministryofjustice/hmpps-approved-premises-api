package uk.gov.justice.digital.hmpps.approvedpremisesapi.service

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ProbationOfficerRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.ValidatableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.ValidationErrors
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.validated
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.AuthorisableActionResult
import java.time.OffsetDateTime
import java.util.UUID

@Service
class ApplicationService(
  private val probationOfficerRepository: ProbationOfficerRepository,
  private val applicationRepository: ApplicationRepository,
  private val jsonSchemaService: JsonSchemaService,
  private val offenderService: OffenderService,
  private val probationOfficerService: ProbationOfficerService
) {
  fun getAllApplicationsForUsername(userDistinguishedName: String): List<ApplicationEntity> {
    val probationOfficerEntity = probationOfficerRepository.findByDistinguishedName(userDistinguishedName)
      ?: return emptyList()

    return applicationRepository.findAllByCreatedByProbationOfficer_Id(probationOfficerEntity.id)
      .map(jsonSchemaService::attemptSchemaUpgrade)
  }

  fun getApplicationForUsername(applicationId: UUID, userDistinguishedName: String): AuthorisableActionResult<ApplicationEntity> {
    val applicationEntity = applicationRepository.findByIdOrNull(applicationId)
      ?: return AuthorisableActionResult.NotFound()

    val probationOfficerEntity = probationOfficerRepository.findByDistinguishedName(userDistinguishedName)

    if (probationOfficerEntity != applicationEntity.createdByProbationOfficer) {
      return AuthorisableActionResult.Unauthorised()
    }

    return AuthorisableActionResult.Success(jsonSchemaService.attemptSchemaUpgrade(applicationEntity))
  }

  fun createApplication(crn: String, username: String) = validated<ApplicationEntity> {
    when (offenderService.getOffenderByCrn(crn, username)) {
      is AuthorisableActionResult.NotFound -> return "$.crn" hasSingleValidationError "doesNotExist"
      is AuthorisableActionResult.Unauthorised -> return "$.crn" hasSingleValidationError "userPermission"
      is AuthorisableActionResult.Success -> Unit
    }

    val probationOfficer = probationOfficerService.getProbationOfficerForRequestUser()

    val createdApplication = applicationRepository.save(
      ApplicationEntity(
        id = UUID.randomUUID(),
        crn = crn,
        createdByProbationOfficer = probationOfficer,
        data = null,
        document = null,
        schemaVersion = jsonSchemaService.getNewestSchema(),
        createdAt = OffsetDateTime.now(),
        submittedAt = null,
        schemaUpToDate = true
      )
    )

    return success(createdApplication)
  }

  fun updateApplication(applicationId: UUID, data: String, document: String?, submittedAt: OffsetDateTime?, username: String): AuthorisableActionResult<ValidatableActionResult<ApplicationEntity>> {
    val application = applicationRepository.findByIdOrNull(applicationId)
      ?: return AuthorisableActionResult.NotFound()

    val probationOfficer = probationOfficerService.getProbationOfficerForRequestUser()

    if (application.createdByProbationOfficer != probationOfficer) {
      return AuthorisableActionResult.Unauthorised()
    }

    if (application.submittedAt != null) {
      return AuthorisableActionResult.Success(
        ValidatableActionResult.GeneralValidationError("This application has already been submitted")
      )
    }

    val validationErrors = ValidationErrors()

    val latestSchemaVersion = jsonSchemaService.getNewestSchema()

    if (!jsonSchemaService.validate(latestSchemaVersion, data)) {
      validationErrors["$.data"] = "invalid"
    }

    if (submittedAt?.isAfter(OffsetDateTime.now()) == true) {
      validationErrors["$.submittedAt"] = "isInFuture"
    }

    if (submittedAt != null && document == null) {
      validationErrors["$.document"] = "empty"
    }

    if (validationErrors.any()) {
      return AuthorisableActionResult.Success(
        ValidatableActionResult.FieldValidationError(validationErrors)
      )
    }

    application.let {
      it.schemaVersion = latestSchemaVersion
      it.data = data
      it.document = document
      it.submittedAt = submittedAt
    }

    return AuthorisableActionResult.Success(
      ValidatableActionResult.Success(applicationRepository.save(application))
    )
  }
}
