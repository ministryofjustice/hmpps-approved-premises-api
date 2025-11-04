package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty

data class ApplicationTimelineNote(

  @get:JsonProperty("note", required = true) val note: kotlin.String,

  @get:JsonProperty("id") val id: java.util.UUID? = null,

  @get:JsonProperty("createdByUser") val createdByUser: User? = null,

  @get:JsonProperty("createdAt") val createdAt: java.time.Instant? = null,
)
