package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 *
 * @param id
 * @param person
 * @param arrivalDate
 * @param originalArrivalDate
 * @param departureDate
 * @param originalDepartureDate
 * @param createdAt
 * @param serviceName
 * @param status
 * @param extensions
 * @param departures The full history of the departure
 * @param cancellations The full history of the cancellation
 * @param premises
 * @param keyWorker KeyWorker is a legacy field only used by CAS1. It is not longer being captured or populated
 * @param bed
 * @param arrival
 * @param departure The latest version of the departure, if it exists
 * @param nonArrival
 * @param cancellation The latest version of the cancellation, if it exists
 * @param confirmation
 * @param turnaround The latest version of the turnaround, if it exists
 * @param turnarounds The full history of turnarounds
 * @param turnaroundStartDate
 * @param effectiveEndDate
 * @param applicationId
 * @param assessmentId
 */
data class Booking(

  @get:JsonProperty("id", required = true) val id: java.util.UUID,

  @get:JsonProperty("person", required = true) val person: Person,

  @get:JsonProperty("arrivalDate", required = true) val arrivalDate: java.time.LocalDate,

  @get:JsonProperty("originalArrivalDate", required = true) val originalArrivalDate: java.time.LocalDate,

  @get:JsonProperty("departureDate", required = true) val departureDate: java.time.LocalDate,

  @get:JsonProperty("originalDepartureDate", required = true) val originalDepartureDate: java.time.LocalDate,

  @get:JsonProperty("createdAt", required = true) val createdAt: java.time.Instant,

  @get:JsonProperty("serviceName", required = true) val serviceName: ServiceName,

  @get:JsonProperty("status", required = true) val status: BookingStatus,

  @get:JsonProperty("extensions", required = true) val extensions: kotlin.collections.List<Extension>,

  @Schema(example = "null", required = true, description = "The full history of the departure")
  @get:JsonProperty("departures", required = true) val departures: kotlin.collections.List<Departure>,

  @Schema(example = "null", required = true, description = "The full history of the cancellation")
  @get:JsonProperty("cancellations", required = true) val cancellations: kotlin.collections.List<Cancellation>,

  @get:JsonProperty("premises", required = true) val premises: BookingPremisesSummary,

  @get:JsonProperty("bed") val bed: Bed? = null,

  @get:JsonProperty("arrival") val arrival: Arrival? = null,

  @Schema(example = "null", description = "The latest version of the departure, if it exists")
  @get:JsonProperty("departure") val departure: Departure? = null,

  @get:JsonProperty("nonArrival") val nonArrival: Nonarrival? = null,

  @Schema(example = "null", description = "The latest version of the cancellation, if it exists")
  @get:JsonProperty("cancellation") val cancellation: Cancellation? = null,

  @get:JsonProperty("confirmation") val confirmation: Confirmation? = null,

  @Schema(example = "null", description = "The latest version of the turnaround, if it exists")
  @get:JsonProperty("turnaround") val turnaround: Turnaround? = null,

  @Schema(example = "null", description = "The full history of turnarounds")
  @get:JsonProperty("turnarounds") val turnarounds: kotlin.collections.List<Turnaround>? = null,

  @get:JsonProperty("turnaroundStartDate") val turnaroundStartDate: java.time.LocalDate? = null,

  @get:JsonProperty("effectiveEndDate") val effectiveEndDate: java.time.LocalDate? = null,

  @get:JsonProperty("applicationId") val applicationId: java.util.UUID? = null,

  @get:JsonProperty("assessmentId") val assessmentId: java.util.UUID? = null,
)
