package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import io.swagger.v3.oas.annotations.media.Schema

/**
 *
 * @param id
 * @param name
 * @param serviceScope
 * @param isActive
 */
data class MoveOnCategory(

  val id: java.util.UUID,

  @Schema(example = "Housing Association - Rented", required = true, description = "")
  val name: kotlin.String,

  val serviceScope: kotlin.String,

  val isActive: kotlin.Boolean,
)
