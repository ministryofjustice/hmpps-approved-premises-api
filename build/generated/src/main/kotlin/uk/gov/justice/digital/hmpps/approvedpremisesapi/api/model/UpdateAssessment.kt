package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 
 * @param &#x60;data&#x60; 
 * @param releaseDate 
 * @param accommodationRequiredFromDate 
 */
data class UpdateAssessment(

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("data", required = true) val `data`: kotlin.collections.Map<kotlin.String, kotlin.Any>,

    @Schema(example = "null", description = "")
    @get:JsonProperty("releaseDate") val releaseDate: java.time.LocalDate? = null,

    @Schema(example = "null", description = "")
    @get:JsonProperty("accommodationRequiredFromDate") val accommodationRequiredFromDate: java.time.LocalDate? = null
) {

}

