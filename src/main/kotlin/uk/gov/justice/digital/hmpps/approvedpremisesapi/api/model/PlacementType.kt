package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

@Suppress("ktlint:standard:enum-entry-name-case", "EnumNaming")
enum class PlacementType(@get:JsonValue val value: kotlin.String) {

  rotl("rotl"),
  releaseFollowingDecision("release_following_decision"),
  additionalPlacement("additional_placement"),
  ;

  companion object {
    @JvmStatic
    @JsonCreator
    fun forValue(value: kotlin.String): PlacementType = values().first { it -> it.value == value }
  }
}
