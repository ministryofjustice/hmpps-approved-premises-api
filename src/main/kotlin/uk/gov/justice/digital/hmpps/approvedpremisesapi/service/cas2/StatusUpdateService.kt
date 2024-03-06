package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas2

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas2.model.Cas2ApplicationStatusUpdatedEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas2.model.Cas2ApplicationStatusUpdatedEventDetails
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas2.model.Cas2Status
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas2.model.EventType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas2.model.ExternalUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas2.model.PersonReference
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas2ApplicationStatusUpdate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2ApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2StatusUpdateDetailEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2StatusUpdateDetailRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2StatusUpdateEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2StatusUpdateRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ExternalUserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.DomainEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.ValidationErrors
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.reference.Cas2PersistedApplicationStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.reference.Cas2PersistedApplicationStatusDetail
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.reference.Cas2PersistedApplicationStatusFinder
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.AuthorisableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.ValidatableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas2.ApplicationStatusTransformer
import java.time.OffsetDateTime
import java.util.UUID
import javax.transaction.Transactional

@Service("Cas2StatusUpdateService")
class StatusUpdateService(
  private val applicationRepository: Cas2ApplicationRepository,
  private val statusUpdateRepository: Cas2StatusUpdateRepository,
  private val statusUpdateDetailRepository: Cas2StatusUpdateDetailRepository,
  private val domainEventService: DomainEventService,
  private val statusFinder: Cas2PersistedApplicationStatusFinder,
  private val statusTransformer: ApplicationStatusTransformer,
  @Value("\${url-templates.frontend.cas2.application}") private val applicationUrlTemplate: String,
) {

  fun isValidStatus(statusUpdate: Cas2ApplicationStatusUpdate): Boolean {
    return findActiveStatusByName(statusUpdate.newStatus) != null
  }

  @SuppressWarnings("ReturnCount")
  @Transactional
  fun create(
    applicationId: UUID,
    statusUpdate: Cas2ApplicationStatusUpdate,
    assessor: ExternalUserEntity,
  ): AuthorisableActionResult<ValidatableActionResult<Cas2StatusUpdateEntity>> {
    val application = applicationRepository.findSubmittedApplicationById(applicationId)
      ?: return AuthorisableActionResult.NotFound()

    val status = findActiveStatusByName(statusUpdate.newStatus)
      ?: return AuthorisableActionResult.Success(
        ValidatableActionResult.GeneralValidationError("The status ${statusUpdate.newStatus} is not valid"),
      )

    val statusDetails = if (statusUpdate.newStatusDetails.isNullOrEmpty()) {
      emptyList()
    } else {
      statusUpdate.newStatusDetails.map { detail ->
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
        application = application,
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

    createStatusUpdatedDomainEvent(createdStatusUpdate, statusDetails)

    return AuthorisableActionResult.Success(
      ValidatableActionResult.Success(createdStatusUpdate),
    )
  }

  private fun findActiveStatusByName(statusName: String): Cas2PersistedApplicationStatus? {
    return statusFinder.active()
      .find { status -> status.name == statusName }
  }

  fun createStatusUpdatedDomainEvent(statusUpdate: Cas2StatusUpdateEntity, statusDetails: List<Cas2PersistedApplicationStatusDetail> = emptyList()) {
    val domainEventId = UUID.randomUUID()
    val eventOccurredAt = statusUpdate.createdAt ?: OffsetDateTime.now()
    val application = statusUpdate.application
    val newStatus = statusUpdate.status()
    val assessor = statusUpdate.assessor

    domainEventService.saveCas2ApplicationStatusUpdatedDomainEvent(
      DomainEvent(
        id = domainEventId,
        applicationId = application.id,
        crn = application.crn,
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
}
