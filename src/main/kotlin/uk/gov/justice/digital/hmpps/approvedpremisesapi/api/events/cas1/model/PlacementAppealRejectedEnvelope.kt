package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import java.time.Instant
import java.util.UUID

data class PlacementAppealRejectedEnvelope(
  @Schema(required = true)
  @get:JsonProperty("id", required = true) override val id: UUID,

  @Schema(required = true)
  @get:JsonProperty("timestamp", required = true) override val timestamp: Instant,

  @Schema(required = true)
  @get:JsonProperty("eventType", required = true) override val eventType: EventType,

  @Schema(required = true)
  @get:JsonProperty("eventDetails", required = true) override val eventDetails: PlacementAppealRejected,
): Cas1DomainEventEnvelopeInterface<PlacementAppealRejected>
