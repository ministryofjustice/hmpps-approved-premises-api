package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

data class Nonarrival(

  val id: java.util.UUID,

  val bookingId: java.util.UUID,

  val date: java.time.LocalDate,

  val reason: NonArrivalReason,

  val createdAt: java.time.Instant,

  val notes: kotlin.String? = null,
)
