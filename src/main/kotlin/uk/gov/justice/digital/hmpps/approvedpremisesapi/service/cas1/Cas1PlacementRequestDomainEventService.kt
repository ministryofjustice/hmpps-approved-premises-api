package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.DatePeriod
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.EventType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.MatchRequestWithdrawn
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.MatchRequestWithdrawnEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.PersonReference
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementRequestEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.DomainEventTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.UrlTemplate
import java.time.Clock
import java.time.Instant
import java.util.UUID

@Service
class Cas1PlacementRequestDomainEventService(
  private val domainEventService: Cas1DomainEventService,
  private val domainEventTransformer: DomainEventTransformer,
  @param:Value("\${url-templates.frontend.application}") private val applicationUrlTemplate: UrlTemplate,
  val clock: Clock,
) {

  fun placementRequestWithdrawn(placementRequest: PlacementRequestEntity, withdrawalContext: WithdrawalContext) {
    /**
     * We only raise domain events for the match request [PlacementRequestEntity] that was created
     * automatically when the application was assessed (i.e. the one created to fulfill the arrival
     * date specified on the application), if there is no corresponding
     * placement_application[automatic=true] (in which case a domain event is raised for that instead)
     *
     * For more information on why we do this, see [PlacementRequestEntity.isForLegacyInitialRequestForPlacement]
     *
     * If this logic was to be changed to raise domain events on withdrawal of _any_ Match Request,
     * the logic setting requestIsForApplicationsArrivalDate on the domain event should be updated
     * and the logic used to render the event description for the timeline should also be reviewed
     */
    if (!placementRequest.isForLegacyInitialRequestForPlacement()) {
      return
    }

    require(withdrawalContext.withdrawalTriggeredBy is WithdrawalTriggeredByUser) { "Only withdrawals triggered by users are supported" }
    val user = withdrawalContext.withdrawalTriggeredBy.user

    val domainEventId = UUID.randomUUID()
    val eventOccurredAt = Instant.now(clock)
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
      SaveCas1DomainEvent(
        id = domainEventId,
        applicationId = application.id,
        crn = application.crn,
        nomsNumber = application.nomsNumber,
        occurredAt = eventOccurredAt,
        data = MatchRequestWithdrawnEnvelope(
          id = domainEventId,
          timestamp = eventOccurredAt,
          eventType = EventType.matchRequestWithdrawn,
          eventDetails = matchRequestEntity,
        ),
      ),
    )
  }
}
