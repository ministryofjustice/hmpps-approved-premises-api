package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty

data class Cas1SpaceBookingRequirements(

  @get:JsonProperty("essentialCharacteristics", required = true) val essentialCharacteristics: List<Cas1SpaceCharacteristic>,
)
