package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

data class BookingSearchResultBookingSummary(

  val id: java.util.UUID,

  val status: BookingStatus,

  val startDate: java.time.LocalDate,

  val endDate: java.time.LocalDate,

  val createdAt: java.time.Instant,
)
