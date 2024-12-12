package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonValue
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.TimelineEventUrlType
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 
 * @param type 
 * @param url 
 */
data class TimelineEventAssociatedUrl(

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("type", required = true) val type: TimelineEventUrlType,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("url", required = true) val url: kotlin.String
) {

}

