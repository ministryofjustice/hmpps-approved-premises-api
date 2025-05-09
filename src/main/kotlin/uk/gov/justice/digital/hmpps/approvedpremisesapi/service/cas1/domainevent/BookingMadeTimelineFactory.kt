package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.domainevent

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.BookingMade
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.Cas1DomainEventEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1BookingMadeContentPayload
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1TimelineEventPayloadBookingSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1TimelineEventType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NamedId
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1DomainEventDescriber
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1DomainEventService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.GetCas1DomainEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.toUiFormat
import java.util.UUID

@Service
class BookingMadeTimelineFactory(val domainEventService: Cas1DomainEventService) : LegacyTimelineFactory<Cas1BookingMadeContentPayload> {
  override fun produce(domainEventId: UUID): Cas1DomainEventDescriber.EventDescriptionAndPayload<Cas1BookingMadeContentPayload> {
    val event = domainEventService.get(domainEventId, BookingMade::class)!!

    val eventDetails = event.data.eventDetails

    return Cas1DomainEventDescriber.EventDescriptionAndPayload(
      buildDescription(event),
      Cas1BookingMadeContentPayload(
        type = Cas1TimelineEventType.bookingMade,
        booking = Cas1TimelineEventPayloadBookingSummary(
          bookingId = eventDetails.bookingId,
          premises = NamedId(
            eventDetails.premises.id,
            eventDetails.premises.name,
          ),
          arrivalDate = eventDetails.arrivalOn,
          departureDate = eventDetails.departureOn,
        ),
        eventNumber = eventDetails.deliusEventNumber,
      ),
    )
  }

  private fun buildDescription(event: GetCas1DomainEvent<Cas1DomainEventEnvelope<BookingMade>>) = event.describe {
    "A placement at ${it.eventDetails.premises.name} was booked for " +
      "${it.eventDetails.arrivalOn.toUiFormat()} to ${it.eventDetails.departureOn.toUiFormat()} " +
      "against Delius Event Number ${it.eventDetails.deliusEventNumber}"
  }

  override fun forType() = DomainEventType.APPROVED_PREMISES_BOOKING_MADE
}
