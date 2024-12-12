package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonValue
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
* 
* Values: referral,booking,bedUsage,bedOccupancy,futureBookings,futureBookingsCsv,bookingGap
*/
enum class Cas3ReportType(val value: kotlin.String) {

    @JsonProperty("referral") referral("referral"),
    @JsonProperty("booking") booking("booking"),
    @JsonProperty("bedUsage") bedUsage("bedUsage"),
    @JsonProperty("bedOccupancy") bedOccupancy("bedOccupancy"),
    @JsonProperty("futureBookings") futureBookings("futureBookings"),
    @JsonProperty("futureBookingsCsv") futureBookingsCsv("futureBookingsCsv"),
    @JsonProperty("bookingGap") bookingGap("bookingGap")
}

