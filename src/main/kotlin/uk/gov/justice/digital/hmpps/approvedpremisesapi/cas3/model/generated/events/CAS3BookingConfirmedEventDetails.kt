package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.events

/**
 *
 * @param personReference
 * @param bookingId
 * @param bookingUrl
 * @param expectedArrivedAt
 * @param notes
 * @param applicationId
 * @param applicationUrl
 * @param confirmedBy
 */
data class CAS3BookingConfirmedEventDetails(

  val personReference: PersonReference,

  val bookingId: java.util.UUID,

  val bookingUrl: java.net.URI,

  val expectedArrivedAt: java.time.Instant,

  val notes: kotlin.String,

  val applicationId: java.util.UUID? = null,

  val applicationUrl: java.net.URI? = null,

  val confirmedBy: StaffMember? = null,
)
