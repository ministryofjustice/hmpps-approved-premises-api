package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model

import com.fasterxml.jackson.annotation.JsonProperty

/**
*
* Values: initial,rotl,releaseFollowingDecisions,additionalPlacement
*/
enum class RequestForPlacementType(val value: kotlin.String) {

  @JsonProperty("initial")
  initial("initial"),

  @JsonProperty("rotl")
  rotl("rotl"),

  @JsonProperty("releaseFollowingDecisions")
  releaseFollowingDecisions("releaseFollowingDecisions"),

  @JsonProperty("additionalPlacement")
  additionalPlacement("additionalPlacement"),
}
