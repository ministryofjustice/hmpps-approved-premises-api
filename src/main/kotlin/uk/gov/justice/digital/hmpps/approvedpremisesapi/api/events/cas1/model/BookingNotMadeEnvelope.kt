package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model

import io.swagger.v3.oas.annotations.media.Schema
import java.util.UUID

/**
 *
 * @param id The UUID of an event
 * @param timestamp
 * @param eventType
 * @param eventDetails
 */
data class BookingNotMadeEnvelope(
  @Schema(example = "364145f9-0af8-488e-9901-b4c46cd9ba37", required = true, description = "The UUID of an event")
  override val id: UUID,

  override val timestamp: java.time.Instant,

  override val eventType: EventType,

  override val eventDetails: BookingNotMade,
) : Cas1DomainEventEnvelopeInterface<BookingNotMade>
