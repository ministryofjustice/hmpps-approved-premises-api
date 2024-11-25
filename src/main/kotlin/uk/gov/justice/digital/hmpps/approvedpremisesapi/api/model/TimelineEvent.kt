package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 *
 * @param type
 * @param id
 * @param occurredAt
 * @param content
 * @param createdBy
 * @param associatedUrls
 * @param triggerSource
 */
data class TimelineEvent(

  @Schema(example = "null", description = "")
  @get:JsonProperty("type") val type: TimelineEventType? = null,

  @Schema(example = "null", description = "")
  @get:JsonProperty("id") val id: kotlin.String? = null,

  @Schema(example = "null", description = "")
  @get:JsonProperty("occurredAt") val occurredAt: java.time.Instant? = null,

  @Schema(example = "null", description = "")
  @get:JsonProperty("content") val content: kotlin.String? = null,

  @Schema(example = "null", description = "")
  @get:JsonProperty("createdBy") val createdBy: User? = null,

  @Schema(example = "null", description = "")
  @get:JsonProperty("associatedUrls") val associatedUrls: kotlin.collections.List<TimelineEventAssociatedUrl>? = null,

  @Schema(example = "null", description = "")
  @get:JsonProperty("triggerSource") val triggerSource: TriggerSourceType? = null,
)
