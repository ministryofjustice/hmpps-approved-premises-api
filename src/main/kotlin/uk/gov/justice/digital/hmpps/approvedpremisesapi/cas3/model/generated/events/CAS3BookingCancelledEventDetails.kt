package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.events

/**
 *
 * @param personReference
 * @param bookingId
 * @param bookingUrl
 * @param cancellationReason
 * @param applicationId
 * @param applicationUrl
 * @param cancelledAt
 * @param notes
 * @param cancelledBy
 */
data class CAS3BookingCancelledEventDetails(

  val personReference: PersonReference,

  val bookingId: java.util.UUID,

  val bookingUrl: java.net.URI,

  val cancellationReason: kotlin.String,

  val applicationId: java.util.UUID? = null,

  val applicationUrl: java.net.URI? = null,

  val cancelledAt: java.time.LocalDate? = null,

  val notes: kotlin.String? = null,

  val cancelledBy: StaffMember? = null,
)
