package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

@Suppress("ktlint:standard:enum-entry-name-case", "EnumNaming")
enum class Cas1ChangeRequestDecision(@get:JsonValue val value: kotlin.String) {

  APPROVED("approved"),
  REJECTED("rejected"),
  ;

  companion object {
    @JvmStatic
    @JsonCreator
    fun forValue(value: kotlin.String): Cas1ChangeRequestDecision = values().first { it -> it.value == value }
  }
}
