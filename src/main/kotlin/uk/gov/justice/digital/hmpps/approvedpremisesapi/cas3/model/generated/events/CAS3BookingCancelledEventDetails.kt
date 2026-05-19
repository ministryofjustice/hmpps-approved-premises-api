package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.events

import com.fasterxml.jackson.annotation.JsonProperty

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

  @get:JsonProperty("personReference", required = true) val personReference: PersonReference,

  @get:JsonProperty("bookingId", required = true) val bookingId: java.util.UUID,

  @get:JsonProperty("bookingUrl", required = true) val bookingUrl: java.net.URI,

  @get:JsonProperty("cancellationReason", required = true) val cancellationReason: String,

  @get:JsonProperty("applicationId") val applicationId: java.util.UUID? = null,

  @get:JsonProperty("applicationUrl") val applicationUrl: java.net.URI? = null,

  @get:JsonProperty("cancelledAt") val cancelledAt: java.time.LocalDate? = null,

  @get:JsonProperty("notes") val notes: String? = null,

  @get:JsonProperty("cancelledBy") val cancelledBy: StaffMember? = null,
)
