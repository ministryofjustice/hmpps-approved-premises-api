package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.service

import jakarta.transaction.Transactional
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas2.model.Cas2ApplicationStatusUpdatedEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas2.model.Cas2ApplicationStatusUpdatedEventDetails
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas2.model.Cas2EventCohort
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas2.model.Cas2Status
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas2.model.EventType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas2.model.ExternalUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas2.model.PersonReference
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model.Cas2AssessmentStatusUpdate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model.Cas2PersistedApplicationStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model.Cas2PersistedApplicationStatusDetail
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model.Cas2ServiceOrigin
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.jpa.entity.Cas2AssessmentRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.jpa.entity.Cas2StatusUpdateDetailEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.jpa.entity.Cas2StatusUpdateDetailRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.jpa.entity.Cas2StatusUpdateEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.jpa.entity.Cas2StatusUpdateRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.jpa.entity.Cas2UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.transformer.Cas2HdcApplicationStatusTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.results.CasResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.results.ValidationErrors
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.DomainEvent
import java.time.OffsetDateTime
import java.util.UUID

@Service
class Cas2StatusUpdateService(
  private val cas2AssessmentRepository: Cas2AssessmentRepository,
  private val cas2StatusUpdateRepository: Cas2StatusUpdateRepository,
  private val cas2StatusUpdateDetailRepository: Cas2StatusUpdateDetailRepository,
  private val domainEventService: Cas2DomainEventService,
  private val cas2PersistedApplicationStatusFinder: Cas2PersistedApplicationStatusFinder,
  private val statusTransformer: Cas2HdcApplicationStatusTransformer,
  private val cas2ApplicationStatusUpdateEmailService: Cas2ApplicationStatusUpdateEmailService,
  @Value("\${url-templates.frontend.cas2v2.application}") private val applicationUrlTemplate: String,
) {

  @Transactional
  @SuppressWarnings("ReturnCount")
  fun createForAssessment(
    assessmentId: UUID,
    statusUpdate: Cas2AssessmentStatusUpdate,
    assessor: Cas2UserEntity,
  ): CasResult<Cas2StatusUpdateEntity> {
    val assessment = cas2AssessmentRepository.findByIdAndServiceOrigin(assessmentId, Cas2ServiceOrigin.BAIL)
      ?: return CasResult.NotFound("Cas2StatusUpdateEntity", assessmentId.toString())

    val status = findActiveStatusByName(statusUpdate.newStatus)
      ?: return CasResult.GeneralValidationError("The status ${statusUpdate.newStatus} is not valid")

    val newDetails = statusUpdate.newStatusDetails.isNullOrEmpty()
    val statusDetails = if (newDetails) {
      emptyList()
    } else {
      statusUpdate.newStatusDetails.map { detail ->
        status.findStatusDetailOnStatus(detail)
          ?: return CasResult.GeneralValidationError("The status detail $detail is not valid")
      }
    }

    if (ValidationErrors().any()) {
      return CasResult.FieldValidationError(ValidationErrors())
    }

    val createdStatusUpdate = cas2StatusUpdateRepository.save(
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
      cas2StatusUpdateDetailRepository.save(
        Cas2StatusUpdateDetailEntity(
          id = UUID.randomUUID(),
          statusDetailId = detail.id,
          statusUpdate = createdStatusUpdate,
          label = detail.label,
        ),
      )
    }

    cas2ApplicationStatusUpdateEmailService.statusUpdate(assessment, createdStatusUpdate)

    createStatusUpdatedDomainEvent(createdStatusUpdate, statusDetails)

    return CasResult.Success(createdStatusUpdate)
  }

  private fun findActiveStatusByName(statusName: String): Cas2PersistedApplicationStatus? = cas2PersistedApplicationStatusFinder.active()
    .find { status -> status.name == statusName }

  fun createStatusUpdatedDomainEvent(
    statusUpdate: Cas2StatusUpdateEntity,
    statusDetails: List<Cas2PersistedApplicationStatusDetail>? = emptyList(),
  ) {
    val domainEventId = UUID.randomUUID()
    val eventOccurredAt = statusUpdate.createdAt
    val application = statusUpdate.application
    val newStatus = statusUpdate.status()
    val assessor = statusUpdate.assessor

    domainEventService.saveApplicationStatusUpdatedDomainEvent(
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
            cohort = application.cohort?.let {
              Cas2EventCohort(code = it.name, longDisplayName = it.longDisplayName)
            },
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
}
