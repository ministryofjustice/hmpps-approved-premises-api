package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2v2.service

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
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApplicationOrigin
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas2v2AssessmentStatusUpdate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.reporting.model.reference.Cas2PersistedApplicationStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.reporting.model.reference.Cas2PersistedApplicationStatusDetail
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.service.Cas2DomainEventService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.transformer.ApplicationStatusTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2v2.jpa.entity.Cas2v2ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2v2.jpa.entity.Cas2v2AssessmentRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2v2.jpa.entity.Cas2v2StatusUpdateDetailEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2v2.jpa.entity.Cas2v2StatusUpdateDetailRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2v2.jpa.entity.Cas2v2StatusUpdateEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2v2.jpa.entity.Cas2v2StatusUpdateRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2v2.jpa.entity.Cas2v2UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2v2.reporting.model.reference.Cas2v2PersistedApplicationStatusFinder
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2v2.util.Cas2v2ApplicationUtils
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.Cas2NotifyTemplates
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.DomainEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.ValidationErrors
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.CasResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.EmailNotificationService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.toCas2UiFormat
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.toCas2UiFormattedHourOfDay
import java.time.OffsetDateTime
import java.util.UUID

@Service("Cas2v2StatusUpdateService")
class Cas2v2StatusUpdateService(
  private val cas2v2AssessmentRepository: Cas2v2AssessmentRepository,
  private val cas2v2StatusUpdateRepository: Cas2v2StatusUpdateRepository,
  private val cas2v2StatusUpdateDetailRepository: Cas2v2StatusUpdateDetailRepository,
  private val domainEventService: Cas2DomainEventService,
  private val emailNotificationService: EmailNotificationService,
  private val cas2v2PersistedApplicationStatusFinder: Cas2v2PersistedApplicationStatusFinder,
  private val statusTransformer: ApplicationStatusTransformer,
  @Value("\${url-templates.frontend.cas2v2.application}") private val applicationUrlTemplate: String,
  @Value("\${url-templates.frontend.cas2v2.application-overview}") private val applicationOverviewUrlTemplate: String,
) {

  private val log = LoggerFactory.getLogger(this::class.java)

  @Transactional
  @SuppressWarnings("ReturnCount")
  fun createForAssessment(
    assessmentId: UUID,
    statusUpdate: Cas2v2AssessmentStatusUpdate,
    assessor: Cas2v2UserEntity,
  ): CasResult<Cas2v2StatusUpdateEntity> {
    val assessment = cas2v2AssessmentRepository.findByIdOrNull(assessmentId)
      ?: return CasResult.NotFound("Cas2v2StatusUpdateEntity", assessmentId.toString())

    val status = findActiveStatusByName(statusUpdate.newStatus)
      ?: return CasResult.GeneralValidationError("The status ${statusUpdate.newStatus} is not valid")

    val newDetails = statusUpdate.newStatusDetails.isNullOrEmpty()
    val statusDetails = if (newDetails) {
      emptyList()
    } else {
      statusUpdate.newStatusDetails?.map { detail ->
        status.findStatusDetailOnStatus(detail)
          ?: return CasResult.GeneralValidationError("The status detail $detail is not valid")
      }
    }

    if (ValidationErrors().any()) {
      return CasResult.FieldValidationError(ValidationErrors())
    }

    val createdStatusUpdate = cas2v2StatusUpdateRepository.save(
      Cas2v2StatusUpdateEntity(
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

    statusDetails?.forEach { detail ->
      cas2v2StatusUpdateDetailRepository.save(
        Cas2v2StatusUpdateDetailEntity(
          id = UUID.randomUUID(),
          statusDetailId = detail.id,
          statusUpdate = createdStatusUpdate,
          label = detail.label,
        ),
      )
    }

    sendEmailStatusUpdated(assessment.application.createdByUser, assessment.application, createdStatusUpdate)

    createStatusUpdatedDomainEvent(createdStatusUpdate, statusDetails)

    return CasResult.Success(createdStatusUpdate)
  }

  private fun findActiveStatusByName(statusName: String): Cas2PersistedApplicationStatus? = cas2v2PersistedApplicationStatusFinder.active()
    .find { status -> status.name == statusName }

  fun createStatusUpdatedDomainEvent(
    statusUpdate: Cas2v2StatusUpdateEntity,
    statusDetails: List<Cas2PersistedApplicationStatusDetail>? = emptyList(),
  ) {
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
            applicationOrigin = application.applicationOrigin.toString(),
            personReference = PersonReference(
              crn = application.crn,
              noms = application.nomsNumber.toString(),
            ),
            newStatus = Cas2Status(
              name = newStatus.name,
              description = newStatus.description,
              label = newStatus.label,
              statusDetails = statusDetails?.let { statusTransformer.transformStatusDetailListToDetailItemList(it) },
            ),
            updatedBy = ExternalUser(
              username = assessor.username,
              name = assessor.name,
              email = assessor.email!!,
              origin = "assessor.origin",
            ),
            updatedAt = eventOccurredAt.toInstant(),
          ),
        ),
      ),
    )
  }

  private fun sendEmailStatusUpdated(user: Cas2v2UserEntity, application: Cas2v2ApplicationEntity, status: Cas2v2StatusUpdateEntity) {
    if (application.createdByUser.email != null) {
      val applicationOrigin = application.applicationOrigin
      val applicationType = Cas2v2ApplicationUtils().getApplicationTypeFromApplicationOrigin(applicationOrigin)

      val templateId = when (applicationOrigin) {
        ApplicationOrigin.courtBail -> Cas2NotifyTemplates.cas2v2ApplicationStatusUpdatedCourtBail
        ApplicationOrigin.prisonBail -> Cas2NotifyTemplates.cas2v2ApplicationStatusUpdatedPrisonBail
        ApplicationOrigin.homeDetentionCurfew -> Cas2NotifyTemplates.cas2ApplicationStatusUpdated
      }

      emailNotificationService.sendCas2Email(
        recipientEmailAddress = user.email!!,
        templateId = templateId,
        personalisation = mapOf(
          "applicationStatus" to status.label,
          "dateStatusChanged" to status.createdAt.toLocalDate().toCas2UiFormat(),
          "timeStatusChanged" to status.createdAt.toCas2UiFormattedHourOfDay(),
          "applicationType" to applicationType,
          "nomsNumber" to application.nomsNumber,
          "crn" to application.crn,
          "applicationUrl" to applicationOverviewUrlTemplate.replace("#id", application.id.toString()),
        ),
      )
    } else {
      log.error("Email not found for User ${application.createdByUser.id}. Unable to send email when updating status of Application ${application.id}")
      Sentry.captureMessage("Email not found for User ${application.createdByUser.id}. Unable to send email when updating status of Application ${application.id}")
    }
  }
}
