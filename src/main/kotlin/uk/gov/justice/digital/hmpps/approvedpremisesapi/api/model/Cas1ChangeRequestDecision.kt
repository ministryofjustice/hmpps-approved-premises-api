package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

/**
*
* Values: APPROVED,REJECTED
*/
enum class Cas1ChangeRequestDecision(@get:JsonValue val value: kotlin.String) {

  APPROVED("approved"),
  REJECTED("rejected"),
  ;

  companion object {
    @JvmStatic
    @JsonCreator
    fun forValue(value: kotlin.String): Cas1ChangeRequestDecision = entries.first { it.value == value }
  }
}
