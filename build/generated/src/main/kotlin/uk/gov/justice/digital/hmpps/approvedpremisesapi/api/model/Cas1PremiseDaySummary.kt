package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonProperty
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1OutOfServiceBedSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1PremiseCapacityForDay
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1SpaceBookingDaySummary
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 
 * @param forDate 
 * @param previousDate 
 * @param nextDate 
 * @param capacity 
 * @param spaceBookings 
 * @param outOfServiceBeds 
 */
data class Cas1PremiseDaySummary(

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("forDate", required = true) val forDate: java.time.LocalDate,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("previousDate", required = true) val previousDate: java.time.LocalDate,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("nextDate", required = true) val nextDate: java.time.LocalDate,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("capacity", required = true) val capacity: Cas1PremiseCapacityForDay,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("spaceBookings", required = true) val spaceBookings: kotlin.collections.List<Cas1SpaceBookingDaySummary>,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("outOfServiceBeds", required = true) val outOfServiceBeds: kotlin.collections.List<Cas1OutOfServiceBedSummary>
) {

}

