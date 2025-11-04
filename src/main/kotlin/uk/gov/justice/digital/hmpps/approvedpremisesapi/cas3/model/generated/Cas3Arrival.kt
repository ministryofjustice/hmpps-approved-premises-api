package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated

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

  val expectedDepartureDate: LocalDate,

  val arrivalDate: LocalDate,

  val arrivalTime: String,

  val bookingId: UUID,

  val createdAt: Instant,

  val notes: String? = null,
)
