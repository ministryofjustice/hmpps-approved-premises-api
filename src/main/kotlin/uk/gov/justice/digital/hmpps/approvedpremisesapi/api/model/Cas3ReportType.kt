package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonValue
import com.fasterxml.jackson.annotation.JsonCreator

/**
* 
* Values: referral,booking,bedUsage,bedOccupancy,futureBookings,futureBookingsCsv,bookingGap
*/
enum class Cas3ReportType(@get:JsonValue val value: String) {

    referral("referral"),
    booking("booking"),
    bedUsage("bedUsage"),
    bedOccupancy("bedOccupancy"),
    futureBookings("futureBookings"),
    futureBookingsCsv("futureBookingsCsv"),
    bookingGap("bookingGap");

    companion object {
        @JvmStatic
        @JsonCreator
        fun forValue(value: String): Cas3ReportType {
                return values().first{it -> it.value == value}
        }
    }
}

