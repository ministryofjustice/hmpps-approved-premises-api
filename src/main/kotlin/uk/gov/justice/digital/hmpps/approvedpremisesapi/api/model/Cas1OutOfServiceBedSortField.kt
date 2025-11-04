package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

@Suppress("ktlint:standard:enum-entry-name-case", "EnumNaming")
enum class Cas1OutOfServiceBedSortField(@get:JsonValue val value: kotlin.String) {

  premisesName("premisesName"),
  roomName("roomName"),
  bedName("bedName"),
  startDate("startDate"),
  endDate("endDate"),
  reason("reason"),
  daysLost("daysLost"),
  ;

  companion object {
    @JvmStatic
    @JsonCreator
    fun forValue(value: kotlin.String): Cas1OutOfServiceBedSortField = values().first { it -> it.value == value }
  }
}
