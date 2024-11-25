package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 *
 * @param personReference
 * @param deliusEventNumber Used in Delius to identify the 'event' via the first active conviction's 'index'
 * @param applicationId The UUID of an application for an AP place
 * @param applicationUrl The URL on the Approved Premises service at which a user can view a representation of an AP application and related resources, including bookings
 * @param applicationSubmittedOn
 * @param bookingId The UUID of booking for an AP place
 * @param premises
 * @param arrivedAt
 * @param expectedDepartureOn
 * @param keyWorker
 * @param previousExpectedDepartureOn
 * @param notes
 */
data class PersonArrived(

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("personReference", required = true) val personReference: PersonReference,

  @Schema(example = "7", required = true, description = "Used in Delius to identify the 'event' via the first active conviction's 'index'")
  @get:JsonProperty("deliusEventNumber", required = true) val deliusEventNumber: kotlin.String,

  @Schema(example = "484b8b5e-6c3b-4400-b200-425bbe410713", required = true, description = "The UUID of an application for an AP place")
  @get:JsonProperty("applicationId", required = true) val applicationId: java.util.UUID,

  @Schema(example = "https://approved-premises-dev.hmpps.service.justice.gov.uk/applications/484b8b5e-6c3b-4400-b200-425bbe410713", required = true, description = "The URL on the Approved Premises service at which a user can view a representation of an AP application and related resources, including bookings")
  @get:JsonProperty("applicationUrl", required = true) val applicationUrl: kotlin.String,

  @Schema(example = "Sun Aug 21 01:00:00 BST 2022", required = true, description = "")
  @get:JsonProperty("applicationSubmittedOn", required = true) val applicationSubmittedOn: java.time.LocalDate,

  @Schema(example = "14c80733-4b6d-4f35-b724-66955aac320c", required = true, description = "The UUID of booking for an AP place")
  @get:JsonProperty("bookingId", required = true) val bookingId: java.util.UUID,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("premises", required = true) val premises: Premises,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("arrivedAt", required = true) val arrivedAt: java.time.Instant,

  @Schema(example = "Tue Feb 28 00:00:00 GMT 2023", required = true, description = "")
  @get:JsonProperty("expectedDepartureOn", required = true) val expectedDepartureOn: java.time.LocalDate,

  @Schema(example = "null", description = "")
  @get:JsonProperty("keyWorker") val keyWorker: StaffMember? = null,

  @Schema(example = "Tue Feb 28 00:00:00 GMT 2023", description = "")
  @get:JsonProperty("previousExpectedDepartureOn") val previousExpectedDepartureOn: java.time.LocalDate? = null,

  @Schema(example = "Arrived a day late due to rail strike. Informed in advance by COM.", description = "")
  @get:JsonProperty("notes") val notes: kotlin.String? = null,
)
