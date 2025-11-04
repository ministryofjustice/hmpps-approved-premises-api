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
 * @param confirmedBy
 */
data class CAS3BookingConfirmedEventDetails(

  @get:JsonProperty("personReference", required = true) val personReference: PersonReference,

  @get:JsonProperty("bookingId", required = true) val bookingId: java.util.UUID,

  @get:JsonProperty("bookingUrl", required = true) val bookingUrl: java.net.URI,

  @get:JsonProperty("expectedArrivedAt", required = true) val expectedArrivedAt: java.time.Instant,

  @get:JsonProperty("notes", required = true) val notes: kotlin.String,

  @get:JsonProperty("applicationId") val applicationId: java.util.UUID? = null,

  @get:JsonProperty("applicationUrl") val applicationUrl: java.net.URI? = null,

  @get:JsonProperty("confirmedBy") val confirmedBy: StaffMember? = null,
)
