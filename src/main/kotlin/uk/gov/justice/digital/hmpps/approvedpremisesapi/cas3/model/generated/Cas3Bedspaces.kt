package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 *
 * @param bedspaces
 * @param totalOnlineBedspaces
 * @param totalUpcomingBedspaces
 * @param totalArchivedBedspaces
 */
data class Cas3Bedspaces(

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("bedspaces", required = true) val bedspaces: List<Cas3Bedspace>,

    @Schema(example = "5", required = true, description = "")
    @get:JsonProperty("totalOnlineBedspaces", required = true) val totalOnlineBedspaces: Int,

    @Schema(example = "1", required = true, description = "")
    @get:JsonProperty("totalUpcomingBedspaces", required = true) val totalUpcomingBedspaces: Int,

    @Schema(example = "2", required = true, description = "")
    @get:JsonProperty("totalArchivedBedspaces", required = true) val totalArchivedBedspaces: Int
)

