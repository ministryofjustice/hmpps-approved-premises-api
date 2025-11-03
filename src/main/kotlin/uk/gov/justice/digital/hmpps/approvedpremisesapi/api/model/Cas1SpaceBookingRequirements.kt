package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * this is only used by deprecated fields
 * @param essentialCharacteristics
 */
data class Cas1SpaceBookingRequirements(

  @get:JsonProperty("essentialCharacteristics", required = true) val essentialCharacteristics: kotlin.collections.List<Cas1SpaceCharacteristic>,
)
