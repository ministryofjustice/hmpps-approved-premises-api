package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import io.swagger.v3.oas.annotations.media.Schema
import java.time.Instant
import java.util.UUID

/**
 * Notes added to a Cas2v2Application
 * @param email
 * @param name
 * @param body
 * @param createdAt
 * @param id
 */
data class Cas2v2ApplicationNote(

  @Schema(example = "roger@example.com", required = true, description = "")
  val email: String,

  @Schema(example = "Roger Smith", required = true, description = "")
  val name: String,

  val body: String,

  val createdAt: Instant,

  val id: UUID? = null,
)
