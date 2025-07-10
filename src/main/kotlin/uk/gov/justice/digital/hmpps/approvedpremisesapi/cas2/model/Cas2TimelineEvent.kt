package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.TimelineEventType
import java.time.Instant

/**
 *
 * @param type
 * @param occurredAt
 * @param label
 * @param body
 * @param createdByName
 */
data class Cas2TimelineEvent(

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("type", required = true) val type: TimelineEventType,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("occurredAt", required = true) val occurredAt: Instant,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("label", required = true) val label: String,

  @Schema(example = "null", description = "")
  @get:JsonProperty("body") val body: String? = null,

  @Schema(example = "null", description = "")
  @get:JsonProperty("createdByName") val createdByName: String? = null,
)
