package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import java.time.Instant
import java.util.UUID

@Deprecated("The generic [Cas1DomainEventEnvelope] should be used instead of type-specific envelopes")
data class PlacementAppealCreatedEnvelope(
  @Schema(example = "364145f9-0af8-488e-9901-b4c46cd9ba37", required = true, description = "The UUID of an event")
  @get:JsonProperty("id", required = true) override val id: UUID,

  @Schema(example = "null", required = true)
  @get:JsonProperty("timestamp", required = true) override val timestamp: Instant,

  @Schema(example = "null", required = true)
  @get:JsonProperty("eventType", required = true) override val eventType: EventType,

  @Schema(example = "null", required = true)
  @get:JsonProperty("eventDetails", required = true) override val eventDetails: PlacementAppealCreated,
): Cas1DomainEventEnvelopeInterface<PlacementAppealCreated>
