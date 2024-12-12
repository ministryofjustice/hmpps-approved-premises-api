package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas3.model

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonValue
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas3.model.EventType
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 
 * @param id The UUID of an event
 * @param timestamp 
 * @param eventType 
 */

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "eventType", visible = true)
@JsonSubTypes(
      JsonSubTypes.Type(value = CAS3AssessmentUpdatedEvent::class, name = "accommodation.cas3.assessment.updated"),
      JsonSubTypes.Type(value = CAS3BookingCancelledEvent::class, name = "accommodation.cas3.booking.cancelled"),
      JsonSubTypes.Type(value = CAS3BookingCancelledUpdatedEvent::class, name = "accommodation.cas3.booking.cancelled.updated"),
      JsonSubTypes.Type(value = CAS3BookingConfirmedEvent::class, name = "accommodation.cas3.booking.confirmed"),
      JsonSubTypes.Type(value = CAS3BookingProvisionallyMadeEvent::class, name = "accommodation.cas3.booking.provisionally-made"),
      JsonSubTypes.Type(value = CAS3PersonArrivedEvent::class, name = "accommodation.cas3.person.arrived"),
      JsonSubTypes.Type(value = CAS3PersonArrivedUpdatedEvent::class, name = "accommodation.cas3.person.arrived.updated"),
      JsonSubTypes.Type(value = CAS3PersonDepartedEvent::class, name = "accommodation.cas3.person.departed"),
      JsonSubTypes.Type(value = CAS3PersonDepartureUpdatedEvent::class, name = "accommodation.cas3.person.departed.updated"),
      JsonSubTypes.Type(value = CAS3ReferralSubmittedEvent::class, name = "accommodation.cas3.referral.submitted")
)

interface CAS3Event{
                @get:Schema(example = "364145f9-0af8-488e-9901-b4c46cd9ba37", requiredMode = Schema.RequiredMode.REQUIRED, description = "The UUID of an event")
        val id: java.util.UUID

                @get:Schema(example = "null", requiredMode = Schema.RequiredMode.REQUIRED, description = "")
        val timestamp: java.time.Instant

                @get:Schema(example = "null", requiredMode = Schema.RequiredMode.REQUIRED, description = "")
        val eventType: EventType


}

