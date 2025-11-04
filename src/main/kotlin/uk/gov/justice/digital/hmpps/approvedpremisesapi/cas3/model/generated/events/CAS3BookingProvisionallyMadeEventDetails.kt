package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.events

import com.fasterxml.jackson.annotation.JsonProperty

/**
 *
 * @param personReference
 * @param bookingId
 * @param bookingUrl
 * @param expectedArrivedAt
 * @param notes
 * @param applicationId
 * @param applicationUrl
 * @param bookedBy
 */
data class CAS3BookingProvisionallyMadeEventDetails(

  val personReference: PersonReference,

  val bookingId: java.util.UUID,

  val bookingUrl: java.net.URI,

  val expectedArrivedAt: java.time.Instant,

  val notes: kotlin.String,

  val applicationId: java.util.UUID? = null,

  val applicationUrl: java.net.URI? = null,

  val bookedBy: StaffMember? = null,
)
