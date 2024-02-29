package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.DatePeriod
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.MatchRequestCreated
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.MatchRequestCreatedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.MatchRequestWithdrawn
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.MatchRequestWithdrawnEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.PersonReference
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PlacementRequest
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementRequestEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.DomainEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.DomainEventService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.WithdrawalContext
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.DomainEventTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.UrlTemplate
import java.time.Instant
import java.util.UUID

@Service
class Cas1PlacementRequestDomainEventService(
  private val domainEventService: DomainEventService,
  private val domainEventTransformer: DomainEventTransformer,
  @Value("\${url-templates.frontend.application}") private val applicationUrlTemplate: UrlTemplate,
) {

  fun placementRequestCreated(placementRequest: PlacementRequestEntity) {
    /**
     * We only raise domain events for the match request [PlacementRequestEntity] that was created
     * automatically when the application was assessed (i.e. the one created to fulfill the arrival
     * date specified on the application).
     *
     * For more information on why we do this, see [PlacementRequestEntity.isForApplicationsArrivalDate]
     *
     * If this logic was to be changed to raise domain events on withdrawal of _any_ Match Request,
     * the logic setting requestIsForApplicationsArrivalDate on the domain event should be updated
     * and the logic used to render the event description for the timeline should also be reviewed
     */
    if (!placementRequest.isForApplicationsArrivalDate()) {
      return
    }

    val domainEventId = UUID.randomUUID()
    val eventOccurredAt = Instant.now()
    val application = placementRequest.application

    val matchRequestEntity = MatchRequestCreated(
      applicationId = application.id,
      applicationUrl = applicationUrlTemplate.resolve("id", application.id.toString()),
      matchRequestId = placementRequest.id,
      personReference = PersonReference(
        crn = application.crn,
        noms = application.nomsNumber ?: "Unknown NOMS Number",
      ),
      deliusEventNumber = application.eventNumber,
      requestIsForApplicationsArrivalDate = true,
      datePeriod = DatePeriod(
        placementRequest.expectedArrival,
        placementRequest.expectedDeparture(),
      ),
    )

    domainEventService.saveMatchRequestCreatedEvent(
      DomainEvent(
        id = domainEventId,
        applicationId = application.id,
        crn = application.crn,
        occurredAt = eventOccurredAt,
        data = MatchRequestCreatedEnvelope(
          id = domainEventId,
          timestamp = eventOccurredAt,
          eventType = "approved-premises.match-request.created",
          eventDetails = matchRequestEntity,
        ),
      ),
    )

  }

  fun placementRequestWithdrawn(placementRequest: PlacementRequestEntity, withdrawalContext: WithdrawalContext) {

    /**
     * See javadoc on placementRequestCreated
     */
    if (!placementRequest.isForApplicationsArrivalDate()) {
      return
    }

    val user = requireNotNull(withdrawalContext.triggeringUser)

    val domainEventId = UUID.randomUUID()
    val eventOccurredAt = Instant.now()
    val application = placementRequest.application

    val matchRequestEntity = MatchRequestWithdrawn(
      applicationId = application.id,
      applicationUrl = applicationUrlTemplate.resolve("id", application.id.toString()),
      matchRequestId = placementRequest.id,
      personReference = PersonReference(
        crn = application.crn,
        noms = application.nomsNumber ?: "Unknown NOMS Number",
      ),
      deliusEventNumber = application.eventNumber,
      withdrawnAt = eventOccurredAt,
      withdrawnBy = domainEventTransformer.toWithdrawnBy(user),
      withdrawalReason = placementRequest.withdrawalReason!!.name,
      requestIsForApplicationsArrivalDate = true,
      datePeriod = DatePeriod(
        placementRequest.expectedArrival,
        placementRequest.expectedDeparture(),
      ),
    )

    domainEventService.saveMatchRequestWithdrawnEvent(
      DomainEvent(
        id = domainEventId,
        applicationId = application.id,
        crn = application.crn,
        occurredAt = eventOccurredAt,
        data = MatchRequestWithdrawnEnvelope(
          id = domainEventId,
          timestamp = eventOccurredAt,
          eventType = "approved-premises.match-request.withdrawn",
          eventDetails = matchRequestEntity,
        ),
      ),
    )
  }
}
