package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas2

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SubmitCas2Application
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2ApplicationJsonSchemaEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2ApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2ApplicationSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.ValidationErrors
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.validated
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.AuthorisableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.ValidatableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.OffenderService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserService
import java.time.OffsetDateTime
import java.util.UUID
import javax.transaction.Transactional

@Service("Cas2ApplicationService")
class ApplicationService(
  private val userRepository: UserRepository,
  private val applicationRepository: Cas2ApplicationRepository,
  private val jsonSchemaService: JsonSchemaService,
  private val offenderService: OffenderService,
  private val userService: UserService,
  private val userAccessService: UserAccessService,
  private val objectMapper: ObjectMapper,
  @Value("\${url-templates.frontend.application}") private val applicationUrlTemplate: String,
) {

  fun getAllApplicationsForUser(user: UserEntity): List<Cas2ApplicationSummary> {
    return applicationRepository.findAllCas2ApplicationSummariesCreatedByUser(user.id)
  }

  fun getApplicationForUsername(applicationId: UUID, userDistinguishedName: String): AuthorisableActionResult<Cas2ApplicationEntity> {
    val applicationEntity = applicationRepository.findByIdOrNull(applicationId)
      ?: return AuthorisableActionResult.NotFound()

    val userEntity = userRepository.findByDeliusUsername(userDistinguishedName)
      ?: throw RuntimeException("Could not get user")

    val canAccess = userAccessService.userCanViewApplication(userEntity, applicationEntity)

    return if (canAccess) {
      AuthorisableActionResult.Success(
        jsonSchemaService.checkSchemaOutdated
        (applicationEntity),
      )
    } else {
      AuthorisableActionResult.Unauthorised()
    }
  }

  fun createApplication(crn: String, user: UserEntity, jwt: String) =
    validated<Cas2ApplicationEntity> {
      val offenderDetailsResult = offenderService.getOffenderByCrn(crn, user.deliusUsername)

      val offenderDetails = when (offenderDetailsResult) {
        is AuthorisableActionResult.NotFound -> return "$.crn" hasSingleValidationError "doesNotExist"
        is AuthorisableActionResult.Unauthorised -> return "$.crn" hasSingleValidationError "userPermission"
        is AuthorisableActionResult.Success -> offenderDetailsResult.entity
      }

      if (offenderDetails.otherIds.nomsNumber == null) {
        throw RuntimeException("Cannot create an Application for an Offender without a NOMS number")
      }

      if (validationErrors.any()) {
        return fieldValidationError
      }

      val createdApplication = applicationRepository.save(
        Cas2ApplicationEntity(
          id = UUID.randomUUID(),
          crn = crn,
          createdByUser = user,
          data = null,
          document = null,
          schemaVersion = jsonSchemaService.getNewestSchema(Cas2ApplicationJsonSchemaEntity::class.java),
          createdAt = OffsetDateTime.now(),
          submittedAt = null,
          schemaUpToDate = true,
          nomsNumber = offenderDetails.otherIds.nomsNumber,
        ),
      )

      return success(createdApplication.apply { schemaUpToDate = true })
    }

  fun updateApplication(applicationId: UUID, data: String?, username: String?):
    AuthorisableActionResult<ValidatableActionResult<Cas2ApplicationEntity>> {
    val application = applicationRepository.findByIdOrNull(applicationId)?.let(jsonSchemaService::checkSchemaOutdated)
      ?: return AuthorisableActionResult.NotFound()

    if (application !is Cas2ApplicationEntity) {
      return AuthorisableActionResult.Success(
        ValidatableActionResult.GeneralValidationError("onlyCas2Supported"),
      )
    }

    val user = userService.getUserForRequest()

    if (application.createdByUser != user) {
      return AuthorisableActionResult.Unauthorised()
    }

    if (!application.schemaUpToDate) {
      return AuthorisableActionResult.Success(
        ValidatableActionResult.GeneralValidationError("The schema version is outdated"),
      )
    }

    if (application.submittedAt != null) {
      return AuthorisableActionResult.Success(
        ValidatableActionResult.GeneralValidationError("This application has already been submitted"),
      )
    }

    application.apply {
      this.data = data
    }

    val savedApplication = applicationRepository.save(application)

    return AuthorisableActionResult.Success(
      ValidatableActionResult.Success(savedApplication),
    )
  }

  @Transactional
  fun submitApplication(
    applicationId: UUID,
    submitApplication: SubmitCas2Application,
  ): AuthorisableActionResult<ValidatableActionResult<Cas2ApplicationEntity>> {
    var application = applicationRepository.findByIdOrNullWithWriteLock(applicationId)
      ?.let(jsonSchemaService::checkSchemaOutdated)
      ?: return AuthorisableActionResult.NotFound()

    val serializedTranslatedDocument = objectMapper.writeValueAsString(submitApplication.translatedDocument)

    val user = userService.getUserForRequest()

    if (application.createdByUser != user) {
      return AuthorisableActionResult.Unauthorised()
    }

    if (application !is Cas2ApplicationEntity) {
      return AuthorisableActionResult.Success(
        ValidatableActionResult.GeneralValidationError("onlyCas2Supported"),
      )
    }

    if (application.submittedAt != null) {
      return AuthorisableActionResult.Success(
        ValidatableActionResult.GeneralValidationError("This application has already been submitted"),
      )
    }

    if (!application.schemaUpToDate) {
      return AuthorisableActionResult.Success(
        ValidatableActionResult.GeneralValidationError("The schema version is outdated"),
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
        ValidatableActionResult.FieldValidationError(validationErrors),
      )
    }

    val schema = application.schemaVersion as? Cas2ApplicationJsonSchemaEntity
      ?: throw RuntimeException("Incorrect type of JSON schema referenced by CAS2 Application")

    application.apply {
      submittedAt = OffsetDateTime.now()
      document = serializedTranslatedDocument
    }

    application = applicationRepository.save(application)

    return AuthorisableActionResult.Success(
      ValidatableActionResult.Success(application),
    )
  }
}
