package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model

data class BookingMadeEnvelope(

  override val id: java.util.UUID,

  override val timestamp: java.time.Instant,

  override val eventType: EventType,

  override val eventDetails: BookingMade,
) : Cas1DomainEventEnvelopeInterface<BookingMade>
