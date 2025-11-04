package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty

/**
 *
 * @param id
 * @param bookingId
 * @param dateTime
 * @param createdAt
 * @param notes
 */
data class Confirmation(

  val id: java.util.UUID,

  val bookingId: java.util.UUID,

  val dateTime: java.time.Instant,

  val createdAt: java.time.Instant,

  val notes: kotlin.String? = null,
)
