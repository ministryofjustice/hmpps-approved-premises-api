package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty

/**
 *
 * @param id
 * @param status
 * @param startDate
 * @param endDate
 * @param createdAt
 */
data class BookingSearchResultBookingSummary(

  @get:JsonProperty("id", required = true) val id: java.util.UUID,

  @get:JsonProperty("status", required = true) val status: BookingStatus,

  @get:JsonProperty("startDate", required = true) val startDate: java.time.LocalDate,

  @get:JsonProperty("endDate", required = true) val endDate: java.time.LocalDate,

  @get:JsonProperty("createdAt", required = true) val createdAt: java.time.Instant,
)
