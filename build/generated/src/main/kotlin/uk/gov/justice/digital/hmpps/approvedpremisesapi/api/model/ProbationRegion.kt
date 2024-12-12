package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 
 * @param id 
 * @param name 
 */
data class ProbationRegion(

    @Schema(example = "952790c0-21d7-4fd6-a7e1-9018f08d8bb0", required = true, description = "")
    @get:JsonProperty("id", required = true) val id: java.util.UUID,

    @Schema(example = "NPS North East Central Referrals", required = true, description = "")
    @get:JsonProperty("name", required = true) val name: kotlin.String
) {

}

