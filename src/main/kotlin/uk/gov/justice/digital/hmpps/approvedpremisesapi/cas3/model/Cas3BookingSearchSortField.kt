package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

enum class Cas3BookingSearchSortField(val value: String) {
  PERSON_NAME("name"),
  PERSON_CRN("crn"),
  BOOKING_START_DATE("startDate"),
  BOOKING_END_DATE("endDate"),
  BOOKING_CREATED_AT("createdAt"),
  ;

  @JsonValue
  fun toValue(): String = value

  companion object {
    @JvmStatic
    @JsonCreator
    fun forValue(value: String): Cas3BookingSearchSortField = entries.first { it.value == value }
  }
}
