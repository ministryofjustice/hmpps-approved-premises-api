package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 *
 * @param apTypes
 * @param spaceCharacteristics
 * @param genders
 */
data class Cas1SpaceSearchRequirements(

  @Schema(example = "null", description = "")
  @get:JsonProperty("apTypes") val apTypes: kotlin.collections.List<ApType>? = null,

  @Schema(example = "null", description = "")
  @get:JsonProperty("spaceCharacteristics") val spaceCharacteristics: kotlin.collections.List<Cas1SpaceCharacteristic>? = null,

  @Schema(example = "null", description = "")
  @get:JsonProperty("genders") val genders: kotlin.collections.List<Gender>? = null,
)
