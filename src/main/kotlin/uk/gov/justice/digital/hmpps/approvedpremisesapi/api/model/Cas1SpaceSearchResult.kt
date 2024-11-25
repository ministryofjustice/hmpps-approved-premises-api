package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 *
 * @param premises
 * @param distanceInMiles
 * @param spacesAvailable
 */
data class Cas1SpaceSearchResult(

  @Schema(example = "null", description = "")
  @get:JsonProperty("premises") val premises: Cas1PremisesSearchResultSummary? = null,

  @Schema(example = "2.1", description = "")
  @get:JsonProperty("distanceInMiles") val distanceInMiles: java.math.BigDecimal? = null,

  @Schema(example = "null", description = "")
  @get:JsonProperty("spacesAvailable") val spacesAvailable: kotlin.collections.List<Cas1SpaceAvailability>? = null,
)
