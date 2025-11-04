package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

/**
 *
 * @param confirmedAt
 * @param reason
 * @param notes
 */
data class Cas1SpaceBookingNonArrival(

  val confirmedAt: java.time.Instant? = null,

  val reason: NamedId? = null,

  val notes: kotlin.String? = null,
)
