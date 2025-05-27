package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas3

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.transaction.Transactional
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas3SubmitApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.LockableApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ProbationDeliveryUnitRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.ValidationErrors
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.CasResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.AssessmentService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.JsonSchemaService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserAccessService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserService
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

@Service
class Cas3ApplicationService(
  private val applicationRepository: ApplicationRepository,
  private val lockableApplicationRepository: LockableApplicationRepository,
  private val probationDeliveryUnitRepository: ProbationDeliveryUnitRepository,
  private val userService: UserService,
  private val userAccessService: UserAccessService,
  private val assessmentService: AssessmentService,
  private val cas3DomainEventService: Cas3DomainEventService,
  private val jsonSchemaService: JsonSchemaService,
  private val objectMapper: ObjectMapper,
) {
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
      )?.let(jsonSchemaService::checkSchemaOutdated)
        ?: return CasResult.NotFound("TemporaryAccommodationApplicationEntity", applicationId.toString())

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
      probationDeliveryUnit = submitApplication.probationDeliveryUnitId?.let {
        probationDeliveryUnitRepository.findByIdOrNull(it)
      }
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

  private fun isUserAuthorizedToAccessApplication(user: UserEntity, application: ApplicationEntity): Boolean = userAccessService.userCanAccessTemporaryAccommodationApplication(user, application)

  private fun markAsDeleted(application: ApplicationEntity, user: UserEntity): CasResult<Unit> {
    application.deletedAt = OffsetDateTime.now()
    applicationRepository.saveAndFlush(application)
    cas3DomainEventService.saveDraftReferralDeletedEvent(application, user)
    return CasResult.Success(Unit)
  }
}
