package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas2.model

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 
 * @param name 
 * @param label 
 */
data class Cas2StatusDetail(

    @Schema(example = "changeOfCircumstances", required = true, description = "")
    @get:JsonProperty("name", required = true) val name: kotlin.String,

    @Schema(example = "Change of Circumstances", required = true, description = "")
    @get:JsonProperty("label", required = true) val label: kotlin.String
) {

}

