package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

/**
 *
 * @param expectedDepartureDate
 * @param arrivalDate
 * @param arrivalTime
 * @param bookingId
 * @param createdAt
 * @param notes
 */
data class Cas3Arrival(

  @get:JsonProperty("expectedDepartureDate", required = true) val expectedDepartureDate: LocalDate,

  @get:JsonProperty("arrivalDate", required = true) val arrivalDate: LocalDate,

  @get:JsonProperty("arrivalTime", required = true) val arrivalTime: String,

  @get:JsonProperty("bookingId", required = true) val bookingId: UUID,

  @get:JsonProperty("createdAt", required = true) val createdAt: Instant,

  @get:JsonProperty("notes") val notes: String? = null,
)
