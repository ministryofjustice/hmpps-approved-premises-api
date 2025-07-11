package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

/**
*
* Values: notMatched,unableToMatch,matched
*/
enum class PlacementRequestStatus(@get:JsonValue val value: kotlin.String) {

  notMatched("notMatched"),
  unableToMatch("unableToMatch"),
  matched("matched"),
  ;

  companion object {
    @JvmStatic
    @JsonCreator
    fun forValue(value: kotlin.String): PlacementRequestStatus = values().first { it -> it.value == value }
  }
}
