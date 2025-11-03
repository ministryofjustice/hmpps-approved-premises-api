package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import java.time.Instant
import java.util.UUID

/**
 * Notes added to an application
 * @param email
 * @param name
 * @param body
 * @param createdAt
 * @param id
 */
data class Cas2ApplicationNote(

  @Schema(example = "roger@example.com", required = true, description = "")
  @get:JsonProperty("email", required = true) val email: String,

  @Schema(example = "Roger Smith", required = true, description = "")
  @get:JsonProperty("name", required = true) val name: String,

  @get:JsonProperty("body", required = true) val body: String,

  @get:JsonProperty("createdAt", required = true) val createdAt: Instant,

  @get:JsonProperty("id") val id: UUID? = null,
)
