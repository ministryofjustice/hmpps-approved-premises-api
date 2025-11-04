package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

/**
 *
 * @param occurredAt
 * @param recordedAt
 * @param reason
 * @param reasonNotes
 */
data class Cas1SpaceBookingCancellation(

  val occurredAt: java.time.LocalDate,

  val recordedAt: java.time.Instant,

  val reason: CancellationReason,

  val reasonNotes: kotlin.String? = null,
)
