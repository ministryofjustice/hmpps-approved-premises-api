package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 
 * @param type 
 * @param title 
 * @param status 
 * @param detail 
 * @param instance 
 */
data class Problem(

    @Schema(example = "https://example.net/validation-error", description = "")
    @get:JsonProperty("type") val type: kotlin.String? = null,

    @Schema(example = "Invalid request parameters", description = "")
    @get:JsonProperty("title") val title: kotlin.String? = null,

    @Schema(example = "400", description = "")
    @get:JsonProperty("status") val status: kotlin.Int? = null,

    @Schema(example = "You provided invalid request parameters", description = "")
    @get:JsonProperty("detail") val detail: kotlin.String? = null,

    @Schema(example = "f7493e12-546d-42c3-b838-06c12671ab5b", description = "")
    @get:JsonProperty("instance") val instance: kotlin.String? = null
) {

}

