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

  @get:JsonProperty("personReference", required = true) val personReference: PersonReference,

  @get:JsonProperty("deliusEventNumber", required = true) val deliusEventNumber: kotlin.String,

  @get:JsonProperty("bookingId", required = true) val bookingId: java.util.UUID,

  @get:JsonProperty("bookingUrl", required = true) val bookingUrl: java.net.URI,

  @get:JsonProperty("premises", required = true) val premises: Premises,

  @get:JsonProperty("arrivedAt", required = true) val arrivedAt: java.time.Instant,

  @get:JsonProperty("expectedDepartureOn", required = true) val expectedDepartureOn: java.time.LocalDate,

  @get:JsonProperty("notes", required = true) val notes: kotlin.String,

  @get:JsonProperty("applicationId") val applicationId: java.util.UUID? = null,

  @get:JsonProperty("applicationUrl") val applicationUrl: java.net.URI? = null,

  @get:JsonProperty("recordedBy") val recordedBy: StaffMember? = null,
)
