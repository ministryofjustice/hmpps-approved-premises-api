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

  val id: java.util.UUID,

  val person: Person,

  val arrivalDate: java.time.LocalDate,

  val originalArrivalDate: java.time.LocalDate,

  val departureDate: java.time.LocalDate,

  val originalDepartureDate: java.time.LocalDate,

  val createdAt: java.time.Instant,

  val serviceName: ServiceName,

  val status: BookingStatus,

  val extensions: kotlin.collections.List<Extension>,

  @Schema(example = "null", required = true, description = "The full history of the departure")
  val departures: kotlin.collections.List<Departure>,

  @Schema(example = "null", required = true, description = "The full history of the cancellation")
  val cancellations: kotlin.collections.List<Cancellation>,

  val premises: BookingPremisesSummary,

  val bed: Bed? = null,

  val arrival: Arrival? = null,

  @Schema(example = "null", description = "The latest version of the departure, if it exists")
  val departure: Departure? = null,

  val nonArrival: Nonarrival? = null,

  @Schema(example = "null", description = "The latest version of the cancellation, if it exists")
  val cancellation: Cancellation? = null,

  val confirmation: Confirmation? = null,

  @Schema(example = "null", description = "The latest version of the turnaround, if it exists")
  val turnaround: Turnaround? = null,

  @Schema(example = "null", description = "The full history of turnarounds")
  val turnarounds: kotlin.collections.List<Turnaround>? = null,

  val turnaroundStartDate: java.time.LocalDate? = null,

  val effectiveEndDate: java.time.LocalDate? = null,

  val applicationId: java.util.UUID? = null,

  val assessmentId: java.util.UUID? = null,
)
