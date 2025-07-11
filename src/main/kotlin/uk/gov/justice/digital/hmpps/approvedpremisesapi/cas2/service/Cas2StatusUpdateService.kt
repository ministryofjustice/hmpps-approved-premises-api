package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.service

import io.sentry.Sentry
import jakarta.transaction.Transactional
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas2.model.Cas2ApplicationStatusUpdatedEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas2.model.Cas2ApplicationStatusUpdatedEventDetails
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas2.model.Cas2Status
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas2.model.EventType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas2.model.ExternalUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas2.model.PersonReference
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.Cas2ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.Cas2AssessmentRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.Cas2StatusUpdateDetailEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.Cas2StatusUpdateDetailRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.Cas2StatusUpdateEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.Cas2StatusUpdateRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.ExternalUserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model.Cas2AssessmentStatusUpdate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.reporting.model.reference.Cas2PersistedApplicationStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.reporting.model.reference.Cas2PersistedApplicationStatusDetail
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.reporting.model.reference.Cas2PersistedApplicationStatusFinder
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.service.Constants.HDC_APPLICATION_TYPE
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.transformer.ApplicationStatusTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.Cas2NotifyTemplates
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.DomainEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.ValidationErrors
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.AuthorisableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.ValidatableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.EmailNotificationService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.toCas2UiFormat
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.toCas2UiFormattedHourOfDay
import java.time.OffsetDateTime
import java.util.UUID

object Constants {
  const val HDC_APPLICATION_TYPE = "Home Detention Curfew (HDC)"
  const val CAS2_COURT_BAIL_APPLICATION_TYPE = "Cas2 Court Bail"
  const val CAS2_PRISON_BAIL_APPLICATION_TYPE = "Cas2 Prison Bail"
}

@Service
class StatusUpdateService(
  private val assessmentRepository: Cas2AssessmentRepository,
  private val statusUpdateRepository: Cas2StatusUpdateRepository,
  private val statusUpdateDetailRepository: Cas2StatusUpdateDetailRepository,
  private val domainEventService: Cas2DomainEventService,
  private val emailNotificationService: EmailNotificationService,
  private val statusFinder: Cas2PersistedApplicationStatusFinder,
  private val statusTransformer: ApplicationStatusTransformer,
  private val cas2EmailService: Cas2EmailService,
  @Value("\${url-templates.frontend.cas2.application}") private val applicationUrlTemplate: String,
  @Value("\${url-templates.frontend.cas2.application-overview}") private val applicationOverviewUrlTemplate: String,
) {

  private val log = LoggerFactory.getLogger(this::class.java)

  fun isValidStatus(statusUpdate: Cas2AssessmentStatusUpdate): Boolean = findActiveStatusByName(statusUpdate.newStatus) != null

  @Transactional
  @SuppressWarnings("ReturnCount")
  fun createForAssessment(
    assessmentId: UUID,
    statusUpdate: Cas2AssessmentStatusUpdate,
    assessor: ExternalUserEntity,
  ): AuthorisableActionResult<ValidatableActionResult<Cas2StatusUpdateEntity>> {
    val assessment = assessmentRepository.findByIdOrNull(assessmentId)
      ?: return AuthorisableActionResult.NotFound()

    val status = findActiveStatusByName(statusUpdate.newStatus)
      ?: return AuthorisableActionResult.Success(
        ValidatableActionResult.GeneralValidationError("The status ${statusUpdate.newStatus} is not valid"),
      )

    val newStatusDetails = statusUpdate.newStatusDetails

    val statusDetails = if (newStatusDetails.isNullOrEmpty()) {
      emptyList()
    } else {
      newStatusDetails.map { detail ->
        status.findStatusDetailOnStatus(detail)
          ?: return AuthorisableActionResult.Success(
            ValidatableActionResult.GeneralValidationError("The status detail $detail is not valid"),
          )
      }
    }

    if (ValidationErrors().any()) {
      return AuthorisableActionResult.Success(
        ValidatableActionResult.FieldValidationError(ValidationErrors()),
      )
    }

    val createdStatusUpdate = statusUpdateRepository.save(
      Cas2StatusUpdateEntity(
        id = UUID.randomUUID(),
        assessment = assessment,
        application = assessment.application,
        assessor = assessor,
        statusId = status.id,
        description = status.description,
        label = status.label,
        createdAt = OffsetDateTime.now(),
      ),
    )

    statusDetails.forEach { detail ->
      statusUpdateDetailRepository.save(
        Cas2StatusUpdateDetailEntity(
          id = UUID.randomUUID(),
          statusDetailId = detail.id,
          statusUpdate = createdStatusUpdate,
          label = detail.label,
        ),
      )
    }

    sendEmailStatusUpdated(assessment.application, createdStatusUpdate)

    createStatusUpdatedDomainEvent(createdStatusUpdate, statusDetails)

    return AuthorisableActionResult.Success(
      ValidatableActionResult.Success(createdStatusUpdate),
    )
  }

  private fun findActiveStatusByName(statusName: String): Cas2PersistedApplicationStatus? = statusFinder.active()
    .find { status -> status.name == statusName }

  fun createStatusUpdatedDomainEvent(statusUpdate: Cas2StatusUpdateEntity, statusDetails: List<Cas2PersistedApplicationStatusDetail> = emptyList()) {
    val domainEventId = UUID.randomUUID()
    val eventOccurredAt = statusUpdate.createdAt
    val application = statusUpdate.application
    val newStatus = statusUpdate.status()
    val assessor = statusUpdate.assessor

    domainEventService.saveCas2ApplicationStatusUpdatedDomainEvent(
      DomainEvent(
        id = domainEventId,
        applicationId = application.id,
        crn = application.crn,
        nomsNumber = application.nomsNumber,
        occurredAt = eventOccurredAt.toInstant(),
        data = Cas2ApplicationStatusUpdatedEvent(
          id = domainEventId,
          timestamp = eventOccurredAt.toInstant(),
          eventType = EventType.applicationStatusUpdated,
          eventDetails = Cas2ApplicationStatusUpdatedEventDetails(
            applicationId = application.id,
            applicationUrl = applicationUrlTemplate.replace("#id", application.id.toString()),
            personReference = PersonReference(
              crn = application.crn,
              noms = application.nomsNumber.toString(),
            ),
            newStatus = Cas2Status(
              name = newStatus.name,
              description = newStatus.description,
              label = newStatus.label,
              statusDetails = statusTransformer.transformStatusDetailListToDetailItemList(statusDetails),
            ),
            updatedBy = ExternalUser(
              username = assessor.username,
              name = assessor.name,
              email = assessor.email,
              origin = assessor.origin,
            ),
            updatedAt = eventOccurredAt.toInstant(),
          ),
        ),
      ),
    )
  }

  // BAIL-WIP - we only use the email address in the function, can we just pass that instead
  private fun sendEmailStatusUpdated(application: Cas2ApplicationEntity, status: Cas2StatusUpdateEntity) {
    val email = cas2EmailService.getReferrerEmail(application)
    if (email != null) { // BAIL-WIP
      emailNotificationService.sendCas2Email(
        recipientEmailAddress = email,
        templateId = Cas2NotifyTemplates.cas2ApplicationStatusUpdated,
        personalisation = mapOf(
          "applicationStatus" to status.label,
          "dateStatusChanged" to status.createdAt.toLocalDate().toCas2UiFormat(),
          "timeStatusChanged" to status.createdAt.toCas2UiFormattedHourOfDay(),
          "applicationType" to HDC_APPLICATION_TYPE,
          "nomsNumber" to application.nomsNumber,
          "applicationUrl" to applicationOverviewUrlTemplate.replace("#id", application.id.toString()),
        ),
      )
    } else {
      val msg = "Email not found for User ${application.getCreatedById()}. Unable to send email when updating status of Application ${application.id}"
      log.error(msg)
      Sentry.captureMessage(msg)
    }
  }
}
