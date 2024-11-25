package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 *
 * @param id
 * @param createdAt
 * @param isOfflineApplication
 * @param timelineEvents
 * @param status
 * @param createdBy
 */
data class ApplicationTimeline(

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("id", required = true) val id: java.util.UUID,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("createdAt", required = true) val createdAt: java.time.Instant,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("isOfflineApplication", required = true) val isOfflineApplication: kotlin.Boolean,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("timelineEvents", required = true) val timelineEvents: kotlin.collections.List<TimelineEvent>,

  @Schema(example = "null", description = "")
  @get:JsonProperty("status") val status: ApprovedPremisesApplicationStatus? = null,

  @Schema(example = "null", description = "")
  @get:JsonProperty("createdBy") val createdBy: User? = null,
)
