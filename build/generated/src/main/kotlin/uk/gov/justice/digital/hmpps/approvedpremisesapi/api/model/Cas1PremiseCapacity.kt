package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonProperty
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1PremiseCapacityForDay
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1PremisesSummary
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 
 * @param premise 
 * @param startDate 
 * @param endDate 
 * @param capacity Capacity for each day, returning chronologically (oldest first)
 */
data class Cas1PremiseCapacity(

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("premise", required = true) val premise: Cas1PremisesSummary,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("startDate", required = true) val startDate: java.time.LocalDate,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("endDate", required = true) val endDate: java.time.LocalDate,

    @Schema(example = "null", required = true, description = "Capacity for each day, returning chronologically (oldest first)")
    @get:JsonProperty("capacity", required = true) val capacity: kotlin.collections.List<Cas1PremiseCapacityForDay>
) {

}

