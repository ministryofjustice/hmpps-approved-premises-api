package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty

/**
 *
 * @param expectedDepartureDate
 * @param arrivalDate
 * @param arrivalTime
 * @param bookingId
 * @param createdAt
 * @param notes
 * @param keyWorkerStaffCode
 */
data class Arrival(

  val expectedDepartureDate: java.time.LocalDate,

  val arrivalDate: java.time.LocalDate,

  val arrivalTime: kotlin.String,

  val bookingId: java.util.UUID,

  val createdAt: java.time.Instant,

  val notes: kotlin.String? = null,

  val keyWorkerStaffCode: kotlin.String? = null,
)
