package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 *
 * @param id
 * @param name
 * @param isActive
 */
data class NonArrivalReason(

  val id: java.util.UUID,

  @Schema(example = "Recall", required = true, description = "")
  val name: kotlin.String,

  val isActive: kotlin.Boolean,
)
