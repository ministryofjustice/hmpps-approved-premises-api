package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

/**
 *
 * @param id
 * @param status
 * @param startDate
 * @param endDate
 * @param createdAt
 */
data class BookingSearchResultBookingSummary(

  val id: java.util.UUID,

  val status: BookingStatus,

  val startDate: java.time.LocalDate,

  val endDate: java.time.LocalDate,

  val createdAt: java.time.Instant,
)
