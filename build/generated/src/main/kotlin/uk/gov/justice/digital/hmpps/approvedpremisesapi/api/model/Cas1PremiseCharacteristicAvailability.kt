package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonValue
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1SpaceCharacteristic
import io.swagger.v3.oas.annotations.media.Schema

/**
 * Will only ever be returned for the following characteristics: 'isArsonSuitable', 'hasEnSuite', 'isSingle', 'isStepFreeDesignated', 'isSuitedForSexOffenders' or 'isWheelchairDesignated'
 * @param characteristic 
 * @param availableBedsCount the number of available beds with this characteristic
 * @param bookingsCount the number of bookings requiring this characteristic
 */
data class Cas1PremiseCharacteristicAvailability(

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("characteristic", required = true) val characteristic: Cas1SpaceCharacteristic,

    @Schema(example = "null", required = true, description = "the number of available beds with this characteristic")
    @get:JsonProperty("availableBedsCount", required = true) val availableBedsCount: kotlin.Int,

    @Schema(example = "null", required = true, description = "the number of bookings requiring this characteristic")
    @get:JsonProperty("bookingsCount", required = true) val bookingsCount: kotlin.Int
) {

}

