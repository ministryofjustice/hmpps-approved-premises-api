package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated

import com.fasterxml.jackson.annotation.JsonProperty
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

  @get:JsonProperty("id", required = true) val id: UUID,

  @get:JsonProperty("person", required = true) val person: Person,

  @get:JsonProperty("arrivalDate", required = true) val arrivalDate: LocalDate,

  @get:JsonProperty("originalArrivalDate", required = true) val originalArrivalDate: LocalDate,

  @get:JsonProperty("departureDate", required = true) val departureDate: LocalDate,

  @get:JsonProperty("originalDepartureDate", required = true) val originalDepartureDate: LocalDate,

  @get:JsonProperty("createdAt", required = true) val createdAt: Instant,

  @get:JsonProperty("bedspace", required = true) val bedspace: Cas3BedspaceSummary,

  @get:JsonProperty("status", required = true) val status: Cas3BookingStatus,

  @get:JsonProperty("extensions", required = true) val extensions: List<Cas3Extension>,

  @field:Schema(example = "null", required = true, description = "The full history of the departure")
  @get:JsonProperty("departures", required = true) val departures: List<Cas3Departure>,

  @field:Schema(example = "null", required = true, description = "The full history of the cancellation")
  @get:JsonProperty("cancellations", required = true) val cancellations: List<Cas3Cancellation>,

  @get:JsonProperty("premises", required = true) val premises: Cas3BookingPremisesSummary,

  @get:JsonProperty("arrival") val arrival: Cas3Arrival? = null,

  @field:Schema(example = "null", description = "The latest version of the departure, if it exists")
  @get:JsonProperty("departure") val departure: Cas3Departure? = null,

  @get:JsonProperty("nonArrival") val nonArrival: Cas3NonArrival? = null,

  @field:Schema(example = "null", description = "The latest version of the cancellation, if it exists")
  @get:JsonProperty("cancellation") val cancellation: Cas3Cancellation? = null,

  @get:JsonProperty("confirmation") val confirmation: Cas3Confirmation? = null,

  @field:Schema(example = "null", description = "The latest version of the turnaround, if it exists")
  @get:JsonProperty("turnaround") val turnaround: Cas3Turnaround? = null,

  @field:Schema(example = "null", description = "The full history of turnarounds")
  @get:JsonProperty("turnarounds") val turnarounds: List<Cas3Turnaround>? = null,

  @get:JsonProperty("turnaroundStartDate") val turnaroundStartDate: LocalDate? = null,

  @get:JsonProperty("effectiveEndDate") val effectiveEndDate: LocalDate? = null,

  @get:JsonProperty("applicationId") val applicationId: UUID? = null,

  @get:JsonProperty("assessmentId") val assessmentId: UUID? = null,
)
