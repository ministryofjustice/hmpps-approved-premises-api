package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

data class Cas1TimelineEvent(

  @get:JsonProperty("type", required = true) val type: Cas1TimelineEventType,

  @get:JsonProperty("id", required = true) val id: kotlin.String,

  @get:JsonProperty("occurredAt", required = true) val occurredAt: java.time.Instant,

  @Schema(example = "null", description = "Timeline description. If a value is provided for 'payload', that should be instead be used to build a description")
  @get:JsonProperty("content") val content: kotlin.String? = null,

  @get:JsonProperty("createdBySummary") val createdBySummary: UserSummary? = null,

  @get:JsonProperty("payload") val payload: Cas1TimelineEventContentPayload? = null,

  @get:JsonProperty("associatedUrls") val associatedUrls: kotlin.collections.List<Cas1TimelineEventAssociatedUrl>? = null,

  @get:JsonProperty("triggerSource") val triggerSource: Cas1TriggerSourceType? = null,

  @get:JsonProperty("schemaVersion") val schemaVersion: kotlin.Int? = null,
)
