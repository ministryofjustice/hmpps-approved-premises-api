package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model

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
  val email: String,

  @Schema(example = "Roger Smith", required = true, description = "")
  val name: String,

  val body: String,

  val createdAt: Instant,

  val id: UUID? = null,
)
