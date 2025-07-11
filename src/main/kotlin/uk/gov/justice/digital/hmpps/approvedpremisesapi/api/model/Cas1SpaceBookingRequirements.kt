package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1SpaceCharacteristic

/**
 * this is only used by deprecated fields
 * @param essentialCharacteristics
 */
data class Cas1SpaceBookingRequirements(

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("essentialCharacteristics", required = true) val essentialCharacteristics: kotlin.collections.List<Cas1SpaceCharacteristic>,
)
