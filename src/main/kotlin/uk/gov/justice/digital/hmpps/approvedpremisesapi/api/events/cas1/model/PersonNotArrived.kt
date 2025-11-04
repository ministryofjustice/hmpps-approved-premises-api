package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 *
 * @param personReference
 * @param deliusEventNumber Used in Delius to identify the 'event' via the first active conviction's 'index'
 * @param applicationId The UUID of an application for an AP place
 * @param applicationUrl The URL on the Approved Premises service at which a user can view a representation of an AP application and related resources, including bookings
 * @param bookingId The UUID of booking for an AP place
 * @param premises
 * @param expectedArrivalOn
 * @param recordedBy
 * @param reason
 * @param legacyReasonCode
 * @param notes
 */
data class PersonNotArrived(

  val personReference: PersonReference,

  @Schema(example = "7", required = true, description = "Used in Delius to identify the 'event' via the first active conviction's 'index'")
  val deliusEventNumber: kotlin.String,

  @Schema(example = "484b8b5e-6c3b-4400-b200-425bbe410713", required = true, description = "The UUID of an application for an AP place")
  val applicationId: java.util.UUID,

  @Schema(example = "https://approved-premises-dev.hmpps.service.justice.gov.uk/applications/484b8b5e-6c3b-4400-b200-425bbe410713", required = true, description = "The URL on the Approved Premises service at which a user can view a representation of an AP application and related resources, including bookings")
  val applicationUrl: kotlin.String,

  @Schema(example = "14c80733-4b6d-4f35-b724-66955aac320c", required = true, description = "The UUID of booking for an AP place")
  val bookingId: java.util.UUID,

  val premises: Premises,

  @Schema(example = "Tue Nov 29 00:00:00 GMT 2022", required = true, description = "")
  val expectedArrivalOn: java.time.LocalDate,

  val recordedBy: StaffMember,

  @Schema(example = "Arrested, remanded in custody, or sentenced", required = true, description = "")
  val reason: kotlin.String,

  @Schema(example = "Q", required = true, description = "")
  val legacyReasonCode: kotlin.String,

  @Schema(example = "We learnt that Mr Smith is in hospital.", description = "")
  val notes: kotlin.String? = null,
) : Cas1DomainEventPayload
