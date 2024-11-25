package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 *
 * @param essentialCharacteristics
 */
data class Cas1SpaceBookingRequirements(

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("essentialCharacteristics", required = true) val essentialCharacteristics: kotlin.collections.List<Cas1SpaceCharacteristic>,
)
