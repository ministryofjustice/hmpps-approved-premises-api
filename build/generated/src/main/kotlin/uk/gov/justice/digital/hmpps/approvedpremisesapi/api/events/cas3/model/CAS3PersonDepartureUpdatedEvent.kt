package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas3.model

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonValue
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas3.model.CAS3Event
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas3.model.CAS3PersonDepartedEventDetails
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas3.model.EventType
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 
 * @param eventDetails 
 */
data class CAS3PersonDepartureUpdatedEvent(

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("eventDetails", required = true) val eventDetails: CAS3PersonDepartedEventDetails,

    @Schema(example = "364145f9-0af8-488e-9901-b4c46cd9ba37", required = true, description = "The UUID of an event")
    @get:JsonProperty("id", required = true) override val id: java.util.UUID,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("timestamp", required = true) override val timestamp: java.time.Instant,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("eventType", required = true) override val eventType: EventType
) : CAS3Event{

}

