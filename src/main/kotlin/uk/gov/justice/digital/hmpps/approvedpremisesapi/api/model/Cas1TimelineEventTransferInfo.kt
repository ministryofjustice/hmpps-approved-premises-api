package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonValue
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1TimelineEventPayloadBookingSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1TimelineEventTransferType
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 
 * @param type 
 * @param booking 
 * @param changeRequestId 
 */
data class Cas1TimelineEventTransferInfo(

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("type", required = true) val type: Cas1TimelineEventTransferType,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("booking", required = true) val booking: Cas1TimelineEventPayloadBookingSummary,

    @Schema(example = "null", description = "")
    @get:JsonProperty("changeRequestId") val changeRequestId: java.util.UUID? = null
    ) {

}

