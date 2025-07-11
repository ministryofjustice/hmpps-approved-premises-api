package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

/**
*
* Values: accepted,rejected
*/
@Suppress("ktlint:standard:enum-entry-name-case", "EnumNaming")
enum class AppealDecision(@get:JsonValue val value: kotlin.String) {

  accepted("accepted"),
  rejected("rejected"),
  ;

  companion object {
    @JvmStatic
    @JsonCreator
    fun forValue(value: kotlin.String): AppealDecision = values().first { it -> it.value == value }
  }
}
