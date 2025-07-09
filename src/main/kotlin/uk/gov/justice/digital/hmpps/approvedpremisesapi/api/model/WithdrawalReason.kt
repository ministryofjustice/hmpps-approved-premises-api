package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonValue
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
* 
* Values: changeInCircumstancesNewApplicationToBeSubmitted,errorInApplication,duplicateApplication,death,otherAccommodationIdentified,other
*/
enum class WithdrawalReason(@get:JsonValue val value: kotlin.String) {

    changeInCircumstancesNewApplicationToBeSubmitted("change_in_circumstances_new_application_to_be_submitted"),
    errorInApplication("error_in_application"),
    duplicateApplication("duplicate_application"),
    death("death"),
    otherAccommodationIdentified("other_accommodation_identified"),
    other("other");

    companion object {
        @JvmStatic
        @JsonCreator
        fun forValue(value: kotlin.String): WithdrawalReason {
                return values().first{it -> it.value == value}
        }
    }
}

