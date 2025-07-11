package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 *
 * @param workingDays
 */
data class NewTurnaround(

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("workingDays", required = true) val workingDays: kotlin.Int,
)
