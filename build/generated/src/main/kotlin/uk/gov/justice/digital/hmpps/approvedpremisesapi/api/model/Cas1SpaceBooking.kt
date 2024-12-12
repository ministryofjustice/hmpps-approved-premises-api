package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonValue
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1KeyWorkerAllocation
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1SpaceBookingCancellation
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1SpaceBookingDates
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1SpaceBookingDeparture
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1SpaceBookingNonArrival
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1SpaceBookingRequirements
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1SpaceBookingSummaryStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NamedId
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Person
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.User
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 
 * @param id 
 * @param applicationId 
 * @param person 
 * @param requirements 
 * @param premises 
 * @param apArea 
 * @param expectedArrivalDate 
 * @param expectedDepartureDate 
 * @param canonicalArrivalDate actual arrival date or, if not known, the expected arrival date
 * @param canonicalDepartureDate actual departure date or, if not known, the expected departure date
 * @param createdAt 
 * @param otherBookingsInPremisesForCrn 
 * @param assessmentId 
 * @param tier 
 * @param bookedBy 
 * @param requestForPlacementId 
 * @param actualArrivalDate Use actualArrivalDateOnly and actualArrivalTime
 * @param actualArrivalDateOnly 
 * @param actualArrivalTime This value may not be defined even if an arrival date is
 * @param actualDepartureDate Use actualDepartureDateOnly and actualDepartureTime
 * @param actualDepartureDateOnly 
 * @param actualDepartureTime This value may not be defined even if a departure date is
 * @param departure 
 * @param keyWorkerAllocation 
 * @param cancellation 
 * @param nonArrival 
 * @param deliusEventNumber 
 * @param status 
 */
data class Cas1SpaceBooking(

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("id", required = true) val id: java.util.UUID,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("applicationId", required = true) val applicationId: java.util.UUID,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("person", required = true) val person: Person,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("requirements", required = true) val requirements: Cas1SpaceBookingRequirements,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("premises", required = true) val premises: NamedId,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("apArea", required = true) val apArea: NamedId,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("expectedArrivalDate", required = true) val expectedArrivalDate: java.time.LocalDate,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("expectedDepartureDate", required = true) val expectedDepartureDate: java.time.LocalDate,

    @Schema(example = "null", required = true, description = "actual arrival date or, if not known, the expected arrival date")
    @get:JsonProperty("canonicalArrivalDate", required = true) val canonicalArrivalDate: java.time.LocalDate,

    @Schema(example = "null", required = true, description = "actual departure date or, if not known, the expected departure date")
    @get:JsonProperty("canonicalDepartureDate", required = true) val canonicalDepartureDate: java.time.LocalDate,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("createdAt", required = true) val createdAt: java.time.Instant,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("otherBookingsInPremisesForCrn", required = true) val otherBookingsInPremisesForCrn: kotlin.collections.List<Cas1SpaceBookingDates>,

    @Schema(example = "null", description = "")
    @get:JsonProperty("assessmentId") val assessmentId: java.util.UUID? = null,

    @Schema(example = "null", description = "")
    @get:JsonProperty("tier") val tier: kotlin.String? = null,

    @Schema(example = "null", description = "")
    @get:JsonProperty("bookedBy") val bookedBy: User? = null,

    @Schema(example = "null", description = "")
    @get:JsonProperty("requestForPlacementId") val requestForPlacementId: java.util.UUID? = null,

    @Schema(example = "null", description = "Use actualArrivalDateOnly and actualArrivalTime")
    @Deprecated(message = "")
    @get:JsonProperty("actualArrivalDate") val actualArrivalDate: java.time.Instant? = null,

    @Schema(example = "null", description = "")
    @get:JsonProperty("actualArrivalDateOnly") val actualArrivalDateOnly: java.time.LocalDate? = null,

    @Schema(example = "23:15", description = "This value may not be defined even if an arrival date is")
    @get:JsonProperty("actualArrivalTime") val actualArrivalTime: kotlin.String? = null,

    @Schema(example = "null", description = "Use actualDepartureDateOnly and actualDepartureTime")
    @Deprecated(message = "")
    @get:JsonProperty("actualDepartureDate") val actualDepartureDate: java.time.Instant? = null,

    @Schema(example = "null", description = "")
    @get:JsonProperty("actualDepartureDateOnly") val actualDepartureDateOnly: java.time.LocalDate? = null,

    @Schema(example = "23:15", description = "This value may not be defined even if a departure date is")
    @get:JsonProperty("actualDepartureTime") val actualDepartureTime: kotlin.String? = null,

    @Schema(example = "null", description = "")
    @get:JsonProperty("departure") val departure: Cas1SpaceBookingDeparture? = null,

    @Schema(example = "null", description = "")
    @get:JsonProperty("keyWorkerAllocation") val keyWorkerAllocation: Cas1KeyWorkerAllocation? = null,

    @Schema(example = "null", description = "")
    @get:JsonProperty("cancellation") val cancellation: Cas1SpaceBookingCancellation? = null,

    @Schema(example = "null", description = "")
    @get:JsonProperty("nonArrival") val nonArrival: Cas1SpaceBookingNonArrival? = null,

    @Schema(example = "null", description = "")
    @get:JsonProperty("deliusEventNumber") val deliusEventNumber: kotlin.String? = null,

    @Schema(example = "null", description = "")
    @get:JsonProperty("status") val status: Cas1SpaceBookingSummaryStatus? = null
) {

}

