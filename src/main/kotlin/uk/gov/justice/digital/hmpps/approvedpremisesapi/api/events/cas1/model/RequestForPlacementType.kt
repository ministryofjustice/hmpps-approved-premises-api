package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

/**
*
* Values: initial,rotl,releaseFollowingDecisions,additionalPlacement
*/
enum class RequestForPlacementType(@get:JsonValue val value: kotlin.String) {

  initial("initial"),
  rotl("rotl"),
  releaseFollowingDecisions("releaseFollowingDecisions"),
  additionalPlacement("additionalPlacement"),
  ;

  companion object {
    @JvmStatic
    @JsonCreator
    fun forValue(value: kotlin.String): RequestForPlacementType = values().first { it -> it.value == value }
  }
}
