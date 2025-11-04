package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

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

  val id: java.util.UUID,

  val bookingId: java.util.UUID,

  val date: java.time.LocalDate,

  val reason: NonArrivalReason,

  val createdAt: java.time.Instant,

  val notes: kotlin.String? = null,
)
