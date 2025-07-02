package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 
 * @param totalPremises 
 * @param results 
 * @param totalOnlineBedspaces 
 * @param totalUpcomingBedspaces 
 */
data class Cas3PremisesSearchResults(

    @Schema(example = "50", required = true, description = "")
    @get:JsonProperty("totalPremises", required = true) val totalPremises: Int,

    @Schema(example = "null", description = "")
    @get:JsonProperty("results") val results: List<Cas3PremisesSearchResult>? = null,

    @Schema(example = "15", description = "")
    @get:JsonProperty("totalOnlineBedspaces") val totalOnlineBedspaces: Int? = null,

    @Schema(example = "3", description = "")
    @get:JsonProperty("totalUpcomingBedspaces") val totalUpcomingBedspaces: Int? = null
    ) {

}

