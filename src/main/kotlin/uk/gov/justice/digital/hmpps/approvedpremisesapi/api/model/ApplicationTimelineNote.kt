package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 * Notes added to an application
 * @param note
 * @param id
 * @param createdByUser
 * @param createdAt
 */
data class ApplicationTimelineNote(

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("note", required = true) val note: kotlin.String,

  @Schema(example = "null", description = "")
  @get:JsonProperty("id") val id: java.util.UUID? = null,

  @Schema(example = "null", description = "")
  @get:JsonProperty("createdByUser") val createdByUser: User? = null,

  @Schema(example = "null", description = "")
  @get:JsonProperty("createdAt") val createdAt: java.time.Instant? = null,
)
