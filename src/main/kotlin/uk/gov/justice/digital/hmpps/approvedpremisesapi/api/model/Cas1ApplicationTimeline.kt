package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty

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

  @get:JsonProperty("id", required = true) val id: java.util.UUID,

  @get:JsonProperty("createdAt", required = true) val createdAt: java.time.Instant,

  @get:JsonProperty("isOfflineApplication", required = true) val isOfflineApplication: kotlin.Boolean,

  @get:JsonProperty("timelineEvents", required = true) val timelineEvents: kotlin.collections.List<Cas1TimelineEvent>,

  @get:JsonProperty("status") val status: Cas1ApplicationStatus? = null,

  @get:JsonProperty("createdBy") val createdBy: User? = null,
)
