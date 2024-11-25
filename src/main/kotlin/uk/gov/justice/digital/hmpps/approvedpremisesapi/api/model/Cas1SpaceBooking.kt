package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty
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
 * @param actualArrivalDate
 * @param actualDepartureDate
 * @param departure
 * @param keyWorkerAllocation
 * @param cancellation
 * @param nonArrival
 * @param deliusEventNumber
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

  @Schema(example = "null", description = "")
  @get:JsonProperty("actualArrivalDate") val actualArrivalDate: java.time.Instant? = null,

  @Schema(example = "null", description = "")
  @get:JsonProperty("actualDepartureDate") val actualDepartureDate: java.time.Instant? = null,

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
