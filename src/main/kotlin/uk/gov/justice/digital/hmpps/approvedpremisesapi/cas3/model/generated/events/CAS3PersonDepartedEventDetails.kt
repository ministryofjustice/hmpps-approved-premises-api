package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.events

import com.fasterxml.jackson.annotation.JsonProperty

/**
 *
 * @param personReference
 * @param deliusEventNumber
 * @param bookingId
 * @param bookingUrl
 * @param premises
 * @param departedAt
 * @param reason
 * @param notes
 * @param applicationId
 * @param applicationUrl
 * @param reasonDetail
 * @param recordedBy
 */
data class CAS3PersonDepartedEventDetails(

  val personReference: PersonReference,

  val deliusEventNumber: kotlin.String,

  val bookingId: java.util.UUID,

  val bookingUrl: java.net.URI,

  val premises: Premises,

  val departedAt: java.time.Instant,

  val reason: kotlin.String,

  val notes: kotlin.String,

  val applicationId: java.util.UUID? = null,

  val applicationUrl: java.net.URI? = null,

  val reasonDetail: kotlin.String? = null,

  val recordedBy: StaffMember? = null,
)
