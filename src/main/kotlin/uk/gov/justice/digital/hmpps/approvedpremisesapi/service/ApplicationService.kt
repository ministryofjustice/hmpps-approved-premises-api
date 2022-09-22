package uk.gov.justice.digital.hmpps.approvedpremisesapi.service

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ProbationOfficerRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.AuthorisableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.ValidatableActionResult
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

  fun createApplication(crn: String, username: String): ValidatableActionResult<ApplicationEntity> {
    when (offenderService.getOffenderByCrn(crn, username)) {
      is AuthorisableActionResult.NotFound -> return ValidatableActionResult.FieldValidationError(mapOf("$.crn" to "This CRN does not exist"))
      is AuthorisableActionResult.Unauthorised -> return ValidatableActionResult.FieldValidationError(mapOf("$.crn" to "You do not have permission to access this CRN"))
      is AuthorisableActionResult.Success -> Unit
    }

    val probationOfficer = probationOfficerService.getProbationOfficerForRequestUser()

    val createdApplication = applicationRepository.save(
      ApplicationEntity(
        id = UUID.randomUUID(),
        crn = crn,
        createdByProbationOfficer = probationOfficer,
        data = null,
        schemaVersion = jsonSchemaService.getNewestSchema(),
        createdAt = OffsetDateTime.now(),
        submittedAt = null,
        schemaUpToDate = true
      )
    )

    return ValidatableActionResult.Success(createdApplication)
  }
}
