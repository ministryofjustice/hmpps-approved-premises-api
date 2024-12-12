package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonValue
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.BookingKeyWorkerAssigned
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.EventType
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 
 * @param id The UUID of an event
 * @param timestamp 
 * @param eventType 
 * @param eventDetails 
 */
data class BookingKeyWorkerAssignedEnvelope(

    @Schema(example = "364145f9-0af8-488e-9901-b4c46cd9ba37", required = true, description = "The UUID of an event")
    @get:JsonProperty("id", required = true) val id: java.util.UUID,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("timestamp", required = true) val timestamp: java.time.Instant,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("eventType", required = true) val eventType: EventType,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("eventDetails", required = true) val eventDetails: BookingKeyWorkerAssigned
) {

}

