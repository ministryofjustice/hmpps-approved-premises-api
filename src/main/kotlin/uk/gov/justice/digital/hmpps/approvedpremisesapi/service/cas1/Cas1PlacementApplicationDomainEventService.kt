package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.DatePeriod
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.PersonReference
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.PlacementApplicationWithdrawn
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.PlacementApplicationWithdrawnEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.DomainEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.DomainEventService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.WithdrawalContext
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.DomainEventTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.UrlTemplate
import java.time.Instant
import java.util.UUID

@Component
class Cas1PlacementApplicationDomainEventService(
  private val domainEventService: DomainEventService,
  private val domainEventTransformer: DomainEventTransformer,
  @Value("\${url-templates.frontend.application}") private val applicationUrlTemplate: UrlTemplate,
) {

  fun placementApplicationWithdrawn(placementApplication: PlacementApplicationEntity, withdrawalContext: WithdrawalContext) {
    val user = requireNotNull(withdrawalContext.triggeringUser)

    val domainEventId = UUID.randomUUID()
    val eventOccurredAt = Instant.now()
    val application = placementApplication.application

    val placementApplicationWithdrawn = PlacementApplicationWithdrawn(
      applicationId = application.id,
      applicationUrl = applicationUrlTemplate.resolve("id", application.id.toString()),
      placementApplicationId = placementApplication.id,
      personReference = PersonReference(
        crn = application.crn,
        noms = application.nomsNumber ?: "Unknown NOMS Number",
      ),
      deliusEventNumber = application.eventNumber,
      withdrawnAt = eventOccurredAt,
      withdrawnBy = domainEventTransformer.toWithdrawnBy(user),
      withdrawalReason = placementApplication.withdrawalReason!!.name,
      placementDates = placementApplication.placementDates.map {
        DatePeriod(
          it.expectedArrival,
          it.expectedDeparture(),
        )
      },
    )

    domainEventService.savePlacementApplicationWithdrawnEvent(
      DomainEvent(
        id = domainEventId,
        applicationId = application.id,
        crn = application.crn,
        occurredAt = eventOccurredAt,
        data = PlacementApplicationWithdrawnEnvelope(
          id = domainEventId,
          timestamp = eventOccurredAt,
          eventType = "approved-premises.placement-application.withdrawn",
          eventDetails = placementApplicationWithdrawn,
        ),
      ),
    )
  }
}
