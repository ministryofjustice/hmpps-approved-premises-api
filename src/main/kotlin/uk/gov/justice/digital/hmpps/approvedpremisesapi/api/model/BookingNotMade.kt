package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

data class BookingNotMade(

  val id: java.util.UUID,

  val placementRequestId: java.util.UUID,

  val createdAt: java.time.Instant,

  val notes: kotlin.String? = null,
)
