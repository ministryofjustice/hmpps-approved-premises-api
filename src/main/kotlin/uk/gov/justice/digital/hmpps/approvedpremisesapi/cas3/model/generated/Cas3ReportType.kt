package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

@Suppress("ktlint:standard:enum-entry-name-case", "EnumNaming")
enum class Cas3ReportType(@get:JsonValue val value: String) {

  referral("referral"),
  booking("booking"),
  bedUsage("bedUsage"),
  bedOccupancy("bedOccupancy"),
  futureBookings("futureBookings"),
  futureBookingsCsv("futureBookingsCsv"),
  bookingGap("bookingGap"),
  ;

  companion object {
    @JvmStatic
    @JsonCreator
    fun forValue(value: String): Cas3ReportType = entries.first { it.value == value }
  }
}
