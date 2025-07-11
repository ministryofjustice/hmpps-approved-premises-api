package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas3.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 *
 * @param updatedFields
 */
data class CAS3AssessmentUpdatedEvent(

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("updatedFields", required = true) val updatedFields: kotlin.collections.List<CAS3AssessmentUpdatedField>,

  @Schema(example = "364145f9-0af8-488e-9901-b4c46cd9ba37", required = true, description = "The UUID of an event")
  @get:JsonProperty("id", required = true) override val id: java.util.UUID,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("timestamp", required = true) override val timestamp: java.time.Instant,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("eventType", required = true) override val eventType: EventType,
) : CAS3Event
