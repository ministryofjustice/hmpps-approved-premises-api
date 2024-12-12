package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonProperty
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1SpaceCharacteristic
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 
 * @param spaceCharacteristics 
 * @param durationInDays 
 */
data class Cas1SpaceAvailability(

    @Schema(example = "null", description = "")
    @get:JsonProperty("spaceCharacteristics") val spaceCharacteristics: kotlin.collections.List<Cas1SpaceCharacteristic>? = null,

    @Schema(example = "77", description = "")
    @get:JsonProperty("durationInDays") val durationInDays: kotlin.Int? = null
) {

}

