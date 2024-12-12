package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonValue
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
* All of the characteristics applicable to a space booking
* Values: HAS_EN_SUITE,IS_ARSON_SUITABLE,IS_SINGLE,IS_STEP_FREE_DESIGNATED,IS_SUITED_FOR_SEX_OFFENDERS,IS_WHEELCHAIR_DESIGNATED
*/
enum class Cas1SpaceBookingCharacteristic(val value: kotlin.String) {

    @JsonProperty("hasEnSuite") HAS_EN_SUITE("hasEnSuite"),
    @JsonProperty("isArsonSuitable") IS_ARSON_SUITABLE("isArsonSuitable"),
    @JsonProperty("isSingle") IS_SINGLE("isSingle"),
    @JsonProperty("isStepFreeDesignated") IS_STEP_FREE_DESIGNATED("isStepFreeDesignated"),
    @JsonProperty("isSuitedForSexOffenders") IS_SUITED_FOR_SEX_OFFENDERS("isSuitedForSexOffenders"),
    @JsonProperty("isWheelchairDesignated") IS_WHEELCHAIR_DESIGNATED("isWheelchairDesignated")
}

