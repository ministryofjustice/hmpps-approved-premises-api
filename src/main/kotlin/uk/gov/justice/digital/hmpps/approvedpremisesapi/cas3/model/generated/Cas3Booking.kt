package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Person
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.Cas3BedspaceSummary
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

/**
 *
 * @param id
 * @param person
 * @param arrivalDate
 * @param originalArrivalDate
 * @param departureDate
 * @param originalDepartureDate
 * @param createdAt
 * @param bedspace
 * @param status
 * @param extensions
 * @param departures The full history of the departure
 * @param cancellations The full history of the cancellation
 * @param premises
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
data class Cas3Booking(

  val id: UUID,

  val person: Person,

  val arrivalDate: LocalDate,

  val originalArrivalDate: LocalDate,

  val departureDate: LocalDate,

  val originalDepartureDate: LocalDate,

  val createdAt: Instant,

  val bedspace: Cas3BedspaceSummary,

  val status: Cas3BookingStatus,

  val extensions: List<Cas3Extension>,

  @Schema(example = "null", required = true, description = "The full history of the departure")
  val departures: List<Cas3Departure>,

  @Schema(example = "null", required = true, description = "The full history of the cancellation")
  val cancellations: List<Cas3Cancellation>,

  val premises: Cas3BookingPremisesSummary,

  val arrival: Cas3Arrival? = null,

  @Schema(example = "null", description = "The latest version of the departure, if it exists")
  val departure: Cas3Departure? = null,

  val nonArrival: Cas3NonArrival? = null,

  @Schema(example = "null", description = "The latest version of the cancellation, if it exists")
  val cancellation: Cas3Cancellation? = null,

  val confirmation: Cas3Confirmation? = null,

  @Schema(example = "null", description = "The latest version of the turnaround, if it exists")
  val turnaround: Cas3Turnaround? = null,

  @Schema(example = "null", description = "The full history of turnarounds")
  val turnarounds: List<Cas3Turnaround>? = null,

  val turnaroundStartDate: LocalDate? = null,

  val effectiveEndDate: LocalDate? = null,

  val applicationId: UUID? = null,

  val assessmentId: UUID? = null,
)
