package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1ApplicationStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1TimelineEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.User

/**
 *
 * @param id
 * @param createdAt
 * @param isOfflineApplication
 * @param timelineEvents
 * @param status
 * @param createdBy
 */
data class Cas1ApplicationTimeline(

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("id", required = true) val id: java.util.UUID,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("createdAt", required = true) val createdAt: java.time.Instant,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("isOfflineApplication", required = true) val isOfflineApplication: kotlin.Boolean,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("timelineEvents", required = true) val timelineEvents: kotlin.collections.List<Cas1TimelineEvent>,

  @Schema(example = "null", description = "")
  @get:JsonProperty("status") val status: Cas1ApplicationStatus? = null,

  @Schema(example = "null", description = "")
  @get:JsonProperty("createdBy") val createdBy: User? = null,
)
