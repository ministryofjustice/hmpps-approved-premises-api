package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.events

import com.fasterxml.jackson.annotation.JsonProperty

/**
 *
 * @param personReference
 * @param deliusEventNumber
 * @param bookingId
 * @param bookingUrl
 * @param premises
 * @param arrivedAt
 * @param expectedDepartureOn
 * @param notes
 * @param applicationId
 * @param applicationUrl
 * @param recordedBy
 */
data class CAS3PersonArrivedEventDetails(

  val personReference: PersonReference,

  val deliusEventNumber: kotlin.String,

  val bookingId: java.util.UUID,

  val bookingUrl: java.net.URI,

  val premises: Premises,

  val arrivedAt: java.time.Instant,

  val expectedDepartureOn: java.time.LocalDate,

  val notes: kotlin.String,

  val applicationId: java.util.UUID? = null,

  val applicationUrl: java.net.URI? = null,

  val recordedBy: StaffMember? = null,
)
