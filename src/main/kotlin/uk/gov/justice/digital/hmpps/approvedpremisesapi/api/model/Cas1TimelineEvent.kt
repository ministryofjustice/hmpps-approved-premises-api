package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 *
 * @param type
 * @param id
 * @param occurredAt
 * @param content Timeline description. If a value is provided for 'payload', that should be instead be used to build a description
 * @param createdBySummary
 * @param payload
 * @param associatedUrls
 * @param triggerSource
 * @param schemaVersion
 */
data class Cas1TimelineEvent(

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("type", required = true) val type: Cas1TimelineEventType,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("id", required = true) val id: kotlin.String,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("occurredAt", required = true) val occurredAt: java.time.Instant,

  @Schema(example = "null", description = "Timeline description. If a value is provided for 'payload', that should be instead be used to build a description")
  @get:JsonProperty("content") val content: kotlin.String? = null,

  @Schema(example = "null", description = "")
  @get:JsonProperty("createdBySummary") val createdBySummary: UserSummary? = null,

  @Schema(example = "null", description = "")
  @get:JsonProperty("payload") val payload: Cas1TimelineEventContentPayload? = null,

  @Schema(example = "null", description = "")
  @get:JsonProperty("associatedUrls") val associatedUrls: kotlin.collections.List<Cas1TimelineEventAssociatedUrl>? = null,

  @Schema(example = "null", description = "")
  @get:JsonProperty("triggerSource") val triggerSource: Cas1TriggerSourceType? = null,

  @Schema(example = "null", description = "")
  @get:JsonProperty("schemaVersion") val schemaVersion: kotlin.Int? = null,
)
