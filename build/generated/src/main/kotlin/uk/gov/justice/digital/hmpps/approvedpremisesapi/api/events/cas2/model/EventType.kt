package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas2.model

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonValue
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
* The type of an event
* Values: applicationSubmitted,applicationStatusUpdated
*/
enum class EventType(val value: kotlin.String) {

    @JsonProperty("applications.cas2.application.submitted") applicationSubmitted("applications.cas2.application.submitted"),
    @JsonProperty("applications.cas2.application.status-updated") applicationStatusUpdated("applications.cas2.application.status-updated")
}

