package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

data class Confirmation(

  val id: java.util.UUID,

  val bookingId: java.util.UUID,

  val dateTime: java.time.Instant,

  val createdAt: java.time.Instant,

  val notes: kotlin.String? = null,
)
