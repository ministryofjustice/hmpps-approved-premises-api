package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas2

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas2.model.Cas2ApplicationStatusUpdatedEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas2.model.Cas2ApplicationStatusUpdatedEventDetails
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas2.model.Cas2Status
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas2.model.EventType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas2.model.ExternalUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas2.model.PersonReference
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas2ApplicationStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas2ApplicationStatusUpdate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2ApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2StatusUpdateEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2StatusUpdateRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ExternalUserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.DomainEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.ValidationErrors
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.reference.Cas2ApplicationStatusSeeding
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.AuthorisableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.ValidatableActionResult
import java.time.OffsetDateTime
import java.util.UUID

@Service("Cas2StatusUpdateService")
class StatusUpdateService(
  private val applicationRepository: Cas2ApplicationRepository,
  private val statusUpdateRepository: Cas2StatusUpdateRepository,
  private val domainEventService: DomainEventService,
  @Value("\${url-templates.frontend.application}") private val applicationUrlTemplate: String,
) {

  fun isValidStatus(statusUpdate: Cas2ApplicationStatusUpdate): Boolean {
    return findStatusByName(statusUpdate.newStatus) != null
  }

  fun create(
    applicationId: UUID,
    statusUpdate: Cas2ApplicationStatusUpdate,
    assessor: ExternalUserEntity,
  ): AuthorisableActionResult<ValidatableActionResult<Cas2StatusUpdateEntity>> {
    val application = applicationRepository.findSubmittedApplicationById(applicationId)
      ?: return AuthorisableActionResult.NotFound()

    val status = findStatusByName(statusUpdate.newStatus)
      ?: return AuthorisableActionResult.Success(
        ValidatableActionResult.GeneralValidationError("The status ${statusUpdate.newStatus} is not valid"),
      )

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
      ),
    )

    createStatusUpdatedDomainEvent(createdStatusUpdate)

    return AuthorisableActionResult.Success(
      ValidatableActionResult.Success(createdStatusUpdate),
    )
  }

  private fun findStatusByName(statusName: String): Cas2ApplicationStatus? {
    return Cas2ApplicationStatusSeeding.statusList()
      .find { status -> status.name == statusName }
  }

  private fun createStatusUpdatedDomainEvent(statusUpdate: Cas2StatusUpdateEntity) {
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
            applicationUrl = applicationUrlTemplate.replace("id", application.id.toString()),
            personReference = PersonReference(
              crn = application.crn,
              noms = application.nomsNumber.toString(),
            ),
            newStatus = Cas2Status(
              name = newStatus.name,
              description = newStatus.description,
              label = newStatus.label,
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
