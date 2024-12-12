package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonValue
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.TimelineEventType
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 
 * @param type 
 * @param occurredAt 
 * @param label 
 * @param createdByName 
 * @param body 
 */
data class Cas2TimelineEvent(

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("type", required = true) val type: TimelineEventType,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("occurredAt", required = true) val occurredAt: java.time.Instant,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("label", required = true) val label: kotlin.String,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("createdByName", required = true) val createdByName: kotlin.String,

    @Schema(example = "null", description = "")
    @get:JsonProperty("body") val body: kotlin.String? = null
) {

}

