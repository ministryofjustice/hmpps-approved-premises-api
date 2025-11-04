package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 *
 * @param id The UUID of an event
 * @param timestamp
 * @param eventType
 * @param eventDetails
 */
@Deprecated("The generic [Cas1DomainEventEnvelope] should be used instead of type-specific envelopes")
data class BookingChangedEnvelope(

  @Schema(example = "364145f9-0af8-488e-9901-b4c46cd9ba37", required = true, description = "The UUID of an event")
  @get:JsonProperty("id", required = true) override val id: java.util.UUID,

  @get:JsonProperty("timestamp", required = true) override val timestamp: java.time.Instant,

  @get:JsonProperty("eventType", required = true) override val eventType: EventType,

  @get:JsonProperty("eventDetails", required = true) override val eventDetails: BookingChanged,
) : Cas1DomainEventEnvelopeInterface<BookingChanged>
