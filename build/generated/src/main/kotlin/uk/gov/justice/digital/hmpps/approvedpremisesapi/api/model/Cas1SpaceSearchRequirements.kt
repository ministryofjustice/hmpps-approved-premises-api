package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonProperty
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1SpaceCharacteristic
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Gender
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 
 * @param apTypes 
 * @param spaceCharacteristics 
 * @param genders gender is obtained from application's associated gender
 */
data class Cas1SpaceSearchRequirements(

    @Schema(example = "null", description = "")
    @get:JsonProperty("apTypes") val apTypes: kotlin.collections.List<ApType>? = null,

    @Schema(example = "null", description = "")
    @get:JsonProperty("spaceCharacteristics") val spaceCharacteristics: kotlin.collections.List<Cas1SpaceCharacteristic>? = null,

    @Schema(example = "null", description = "gender is obtained from application's associated gender")
    @Deprecated(message = "")
    @get:JsonProperty("genders") val genders: kotlin.collections.List<Gender>? = null
) {

}

