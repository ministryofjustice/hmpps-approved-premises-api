package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

data class Cancellation(

  val bookingId: java.util.UUID,

  val date: java.time.LocalDate,

  val reason: CancellationReason,

  val createdAt: java.time.Instant,

  val premisesName: kotlin.String,

  val id: java.util.UUID? = null,

  val notes: kotlin.String? = null,

  val otherReason: kotlin.String? = null,
)
