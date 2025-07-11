package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

/**
*
* Values: personName,personCrn,bookingStartDate,bookingEndDate,bookingCreatedAt
*/
@Suppress("ktlint:standard:enum-entry-name-case", "EnumNaming")
enum class BookingSearchSortField(@get:JsonValue val value: kotlin.String) {

  personName("name"),
  personCrn("crn"),
  bookingStartDate("startDate"),
  bookingEndDate("endDate"),
  bookingCreatedAt("createdAt"),
  ;

  companion object {
    @JvmStatic
    @JsonCreator
    fun forValue(value: kotlin.String): BookingSearchSortField = entries.first { it.value == value }
  }
}
