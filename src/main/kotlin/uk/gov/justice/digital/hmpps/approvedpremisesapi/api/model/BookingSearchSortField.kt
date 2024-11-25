package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty

/**
*
* Values: personName,personCrn,bookingStartDate,bookingEndDate,bookingCreatedAt
*/
enum class BookingSearchSortField(val value: kotlin.String) {

  @JsonProperty("name")
  personName("name"),

  @JsonProperty("crn")
  personCrn("crn"),

  @JsonProperty("startDate")
  bookingStartDate("startDate"),

  @JsonProperty("endDate")
  bookingEndDate("endDate"),

  @JsonProperty("createdAt")
  bookingCreatedAt("createdAt"),
}
