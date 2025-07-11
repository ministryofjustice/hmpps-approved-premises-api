package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 *
 * @param translatedDocument Any object
 * @param placementType
 * @param placementDates
 */
data class SubmitPlacementApplication(

  @Schema(example = "null", required = true, description = "Any object")
  @get:JsonProperty("translatedDocument", required = true) val translatedDocument: kotlin.Any,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("placementType", required = true) val placementType: PlacementType,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("placementDates", required = true) val placementDates: kotlin.collections.List<PlacementDates>,
)
