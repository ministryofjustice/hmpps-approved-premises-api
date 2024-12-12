package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonProperty
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1PremisesSearchResultSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1SpaceAvailability
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
    @get:JsonProperty("spacesAvailable") val spacesAvailable: kotlin.collections.List<Cas1SpaceAvailability>? = null
) {

}

