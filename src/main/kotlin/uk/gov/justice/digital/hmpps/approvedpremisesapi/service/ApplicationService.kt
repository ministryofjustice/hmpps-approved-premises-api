package uk.gov.justice.digital.hmpps.approvedpremisesapi.service

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.JsonSchemaType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRepository
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
  private val assessmentService: AssessmentService
) {
  fun getAllApplicationsForUsername(userDistinguishedName: String): List<ApplicationEntity> {
    val userEntity = userRepository.findByDeliusUsername(userDistinguishedName)
      ?: return emptyList()

    return applicationRepository.findAllByCreatedByUser_Id(userEntity.id)
      .map(jsonSchemaService::attemptSchemaUpgrade)
  }

  fun getApplicationForUsername(applicationId: UUID, userDistinguishedName: String): AuthorisableActionResult<ApplicationEntity> {
    val applicationEntity = applicationRepository.findByIdOrNull(applicationId)
      ?: return AuthorisableActionResult.NotFound()

    val userEntity = userRepository.findByDeliusUsername(userDistinguishedName)

    if (userEntity != applicationEntity.createdByUser) {
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

    val user = userService.getUserForRequest()

    val createdApplication = applicationRepository.save(
      ApplicationEntity(
        id = UUID.randomUUID(),
        crn = crn,
        createdByUser = user,
        data = null,
        document = null,
        schemaVersion = jsonSchemaService.getNewestSchema(JsonSchemaType.APPLICATION),
        createdAt = OffsetDateTime.now(),
        submittedAt = null,
        isWomensApplication = null,
        isPipeApplication = null,
        schemaUpToDate = true
      )
    )

    return success(createdApplication.apply { schemaUpToDate = true })
  }

  fun updateApplication(applicationId: UUID, data: String, isWomensApplication: Boolean?, isPipeApplication: Boolean?, username: String): AuthorisableActionResult<ValidatableActionResult<ApplicationEntity>> {
    val application = applicationRepository.findByIdOrNull(applicationId)
      ?: return AuthorisableActionResult.NotFound()

    val user = userService.getUserForRequest()

    if (application.createdByUser != user) {
      return AuthorisableActionResult.Unauthorised()
    }

    if (application.submittedAt != null) {
      return AuthorisableActionResult.Success(
        ValidatableActionResult.GeneralValidationError("This application has already been submitted")
      )
    }

    application.let {
      it.data = data
      it.isPipeApplication = isPipeApplication
      it.isWomensApplication = isWomensApplication
    }

    val savedApplication = applicationRepository.save(application)

    return AuthorisableActionResult.Success(
      ValidatableActionResult.Success(savedApplication)
    )
  }
}
