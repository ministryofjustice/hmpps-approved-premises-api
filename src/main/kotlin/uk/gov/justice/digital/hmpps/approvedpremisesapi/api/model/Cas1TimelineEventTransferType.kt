package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

@Suppress("ktlint:standard:enum-entry-name-case", "EnumNaming")
enum class Cas1TimelineEventTransferType(@get:JsonValue val value: kotlin.String) {

  EMERGENCY("emergency"),
  PLANNED("planned"),
  ;

  companion object {
    @JvmStatic
    @JsonCreator
    fun forValue(value: kotlin.String): Cas1TimelineEventTransferType = values().first { it -> it.value == value }
  }
}
