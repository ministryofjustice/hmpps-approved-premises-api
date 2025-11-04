package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import io.swagger.v3.oas.annotations.media.Schema

/**
 *
 * @param id
 * @param name
 * @param nomisUsername
 * @param isActive
 * @param email
 */
data class NomisUser(

  val id: java.util.UUID,

  @Schema(example = "Roger Smith", required = true, description = "")
  val name: kotlin.String,

  @Schema(example = "SMITHR_GEN", required = true, description = "")
  val nomisUsername: kotlin.String,

  @Schema(example = "true", required = true, description = "")
  val isActive: kotlin.Boolean,

  @Schema(example = "Roger.Smith@justice.gov.uk", description = "")
  val email: kotlin.String? = null,
)
