package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

/**
*
* Values: accepted,rejected,withdraw,withdrawnByPp
*/
@Suppress("ktlint:standard:enum-entry-name-case", "EnumNaming")
enum class PlacementApplicationDecision(@get:JsonValue val value: kotlin.String) {

  accepted("accepted"),
  rejected("rejected"),
  withdraw("withdraw"),
  withdrawnByPp("withdrawn_by_pp"),
  ;

  companion object {
    @JvmStatic
    @JsonCreator
    fun forValue(value: kotlin.String): PlacementApplicationDecision = values().first { it -> it.value == value }
  }
}
