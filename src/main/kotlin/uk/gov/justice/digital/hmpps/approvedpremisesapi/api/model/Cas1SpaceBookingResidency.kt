package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty

/**
*
* Values: upcoming,current,historic
*/
enum class Cas1SpaceBookingResidency(val value: kotlin.String) {

  @JsonProperty("upcoming")
  upcoming("upcoming"),

  @JsonProperty("current")
  current("current"),

  @JsonProperty("historic")
  historic("historic"),
}
