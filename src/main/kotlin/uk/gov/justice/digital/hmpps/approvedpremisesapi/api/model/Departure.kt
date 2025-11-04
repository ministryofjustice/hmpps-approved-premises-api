package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

/**
 *
 * @param id
 * @param bookingId
 * @param dateTime
 * @param reason
 * @param moveOnCategory
 * @param createdAt
 * @param notes
 * @param destinationProvider
 */
data class Departure(

  val id: java.util.UUID,

  val bookingId: java.util.UUID,

  val dateTime: java.time.Instant,

  val reason: DepartureReason,

  val moveOnCategory: MoveOnCategory,

  val createdAt: java.time.Instant,

  val notes: kotlin.String? = null,

  val destinationProvider: DestinationProvider? = null,
)
