package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 
 * @param name 
 */
data class Cru(

    @Schema(example = "NPS North East", required = true, description = "")
    @get:JsonProperty("name", required = true) val name: kotlin.String
) {

}

