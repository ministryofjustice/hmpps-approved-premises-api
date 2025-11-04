package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import io.swagger.v3.oas.annotations.media.Schema

/**
 *
 * @param id
 * @param name
 * @param serviceScope
 * @param isActive
 * @param parentReasonId
 */
data class DepartureReason(

  val id: java.util.UUID,

  @Schema(example = "Admitted to Hospital", required = true, description = "")
  val name: kotlin.String,

  val serviceScope: kotlin.String,

  val isActive: kotlin.Boolean,

  val parentReasonId: java.util.UUID? = null,
)
