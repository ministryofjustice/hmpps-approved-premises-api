package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.UserSummary
import java.time.Instant

data class Cas1TimelineEvent(

  @get:JsonProperty("type", required = true) val type: Cas1TimelineEventType,

  @get:JsonProperty("id", required = true) val id: String,

  @get:JsonProperty("occurredAt", required = true) val occurredAt: Instant,

  @Schema(example = "null", description = "Timeline description. If a value is provided for 'payload', that should be instead be used to build a description")
  @get:JsonProperty("content") val content: String? = null,

  @get:JsonProperty("createdBySummary") val createdBySummary: UserSummary? = null,

  @get:JsonProperty("payload") val payload: Cas1TimelineEventContentPayload? = null,

  @get:JsonProperty("associatedUrls") val associatedUrls: List<Cas1TimelineEventAssociatedUrl>? = null,

  @get:JsonProperty("triggerSource") val triggerSource: Cas1TriggerSourceType? = null,

  @get:JsonProperty("schemaVersion") val schemaVersion: Int? = null,
)
