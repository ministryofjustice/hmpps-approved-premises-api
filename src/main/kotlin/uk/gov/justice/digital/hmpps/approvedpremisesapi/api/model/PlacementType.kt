package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty

/**
*
* Values: rotl,releaseFollowingDecision,additionalPlacement
*/
enum class PlacementType(val value: kotlin.String) {

  @JsonProperty("rotl")
  rotl("rotl"),

  @JsonProperty("release_following_decision")
  releaseFollowingDecision("release_following_decision"),

  @JsonProperty("additional_placement")
  additionalPlacement("additional_placement"),
}
