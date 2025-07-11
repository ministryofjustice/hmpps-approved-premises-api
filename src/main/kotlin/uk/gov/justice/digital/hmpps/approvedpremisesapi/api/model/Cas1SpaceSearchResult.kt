package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1PremisesSearchResultSummary

/**
 *
 * @param premises
 * @param distanceInMiles
 */
data class Cas1SpaceSearchResult(

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("premises", required = true) val premises: Cas1PremisesSearchResultSummary,

  @Schema(example = "2.1", required = true, description = "")
  @get:JsonProperty("distanceInMiles", required = true) val distanceInMiles: java.math.BigDecimal,
)
