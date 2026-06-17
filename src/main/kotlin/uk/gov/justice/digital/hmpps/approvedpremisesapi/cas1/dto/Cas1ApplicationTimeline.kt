package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto

import com.fasterxml.jackson.annotation.JsonProperty
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.User
import java.time.Instant
import java.util.UUID

data class Cas1ApplicationTimeline(

  @get:JsonProperty("id", required = true) val id: UUID,

  @get:JsonProperty("createdAt", required = true) val createdAt: Instant,

  @get:JsonProperty("isOfflineApplication", required = true) val isOfflineApplication: Boolean,

  @get:JsonProperty("timelineEvents", required = true) val timelineEvents: List<Cas1TimelineEvent>,

  @get:JsonProperty("status") val status: Cas1ApplicationStatus? = null,

  @get:JsonProperty("createdBy") val createdBy: User? = null,
)
