package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty

/**
 *
 * @param id
 * @param bookingId
 * @param date
 * @param reason
 * @param createdAt
 * @param notes
 */
data class Nonarrival(

  @get:JsonProperty("id", required = true) val id: java.util.UUID,

  @get:JsonProperty("bookingId", required = true) val bookingId: java.util.UUID,

  @get:JsonProperty("date", required = true) val date: java.time.LocalDate,

  @get:JsonProperty("reason", required = true) val reason: NonArrivalReason,

  @get:JsonProperty("createdAt", required = true) val createdAt: java.time.Instant,

  @get:JsonProperty("notes") val notes: kotlin.String? = null,
)
