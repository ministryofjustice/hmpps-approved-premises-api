package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 *
 * @param &#x60;data&#x60;
 */
data class UpdatePlacementApplication(

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("data", required = true) val `data`: kotlin.collections.Map<kotlin.String, kotlin.Any>,
)
