package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 *
 * @param id
 * @param name
 * @param isActive
 * @param serviceScope
 */
data class LostBedReason(

  val id: java.util.UUID,

  @Schema(example = "Double Room with Single Occupancy - Other (Non-FM)", required = true, description = "")
  val name: kotlin.String,

  val isActive: kotlin.Boolean,

  val serviceScope: kotlin.String,
)
