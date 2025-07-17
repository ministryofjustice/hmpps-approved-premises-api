package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

data class Cas1SpaceBooking(

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("id", required = true) val id: java.util.UUID,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("applicationId", required = true) val applicationId: java.util.UUID,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("person", required = true) val person: Person,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("premises", required = true) val premises: NamedId,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("apArea", required = true) val apArea: NamedId,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("expectedArrivalDate", required = true) val expectedArrivalDate: java.time.LocalDate,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("expectedDepartureDate", required = true) val expectedDepartureDate: java.time.LocalDate,

  @Schema(example = "null", required = true, description = "actual arrival date or, if not known, the expected arrival date.")
  @get:JsonProperty("canonicalArrivalDate", required = true) val canonicalArrivalDate: java.time.LocalDate,

  @Schema(example = "null", required = true, description = "actual departure date or, if not known, the expected departure date")
  @get:JsonProperty("canonicalDepartureDate", required = true) val canonicalDepartureDate: java.time.LocalDate,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("createdAt", required = true) val createdAt: java.time.Instant,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("otherBookingsInPremisesForCrn", required = true) val otherBookingsInPremisesForCrn: kotlin.collections.List<Cas1SpaceBookingDates>,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("characteristics", required = true) val characteristics: kotlin.collections.List<Cas1SpaceCharacteristic>,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("allowedActions", required = true) val allowedActions: kotlin.collections.List<Cas1SpaceBookingAction>,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("openChangeRequests", required = true) val openChangeRequests: kotlin.collections.List<Cas1ChangeRequestSummary>,

  @Schema(example = "null", description = "")
  @get:JsonProperty("assessmentId") val assessmentId: java.util.UUID? = null,

  @Schema(example = "null", description = "")
  @get:JsonProperty("tier") val tier: kotlin.String? = null,

  @Schema(example = "null", description = "")
  @get:JsonProperty("bookedBy") val bookedBy: User? = null,

  @Schema(example = "null", description = "use the better named 'placementRequestId'")
  @Deprecated(message = "")
  @get:JsonProperty("requestForPlacementId") val requestForPlacementId: java.util.UUID? = null,

  @Schema(example = "null", description = "")
  @get:JsonProperty("placementRequestId") val placementRequestId: java.util.UUID? = null,

  @Schema(example = "null", description = "")
  @get:JsonProperty("actualArrivalDate") val actualArrivalDate: java.time.LocalDate? = null,

  @Schema(example = "null", description = "Use actualArrivalDate")
  @Deprecated(message = "")
  @get:JsonProperty("actualArrivalDateOnly") val actualArrivalDateOnly: java.time.LocalDate? = null,

  @Schema(example = "23:15", description = "This value may not be defined even if an arrival date is")
  @get:JsonProperty("actualArrivalTime") val actualArrivalTime: kotlin.String? = null,

  @Schema(example = "null", description = "")
  @get:JsonProperty("actualDepartureDate") val actualDepartureDate: java.time.LocalDate? = null,

  @Schema(example = "null", description = "Use actualDepartureDate")
  @Deprecated(message = "")
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
)
