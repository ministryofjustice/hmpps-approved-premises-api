package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

/**
 *
 * @param bookingId
 * @param date
 * @param reason
 * @param createdAt
 * @param premisesName
 * @param id
 * @param notes
 * @param otherReason
 */
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
