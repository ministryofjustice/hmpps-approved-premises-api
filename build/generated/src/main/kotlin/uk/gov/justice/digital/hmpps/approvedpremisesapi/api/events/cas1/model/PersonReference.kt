package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 
 * @param crn 
 * @param noms 
 */
data class PersonReference(

    @Schema(example = "C123456", required = true, description = "")
    @get:JsonProperty("crn", required = true) val crn: kotlin.String,

    @Schema(example = "A1234ZX", required = true, description = "")
    @get:JsonProperty("noms", required = true) val noms: kotlin.String
) {

}

