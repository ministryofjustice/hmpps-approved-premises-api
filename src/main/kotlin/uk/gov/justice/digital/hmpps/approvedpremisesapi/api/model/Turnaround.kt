package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty

/**
 *
 * @param id
 * @param bookingId
 * @param workingDays
 * @param createdAt
 */
data class Turnaround(

  val id: java.util.UUID,

  val bookingId: java.util.UUID,

  val workingDays: kotlin.Int,

  val createdAt: java.time.Instant,
)
