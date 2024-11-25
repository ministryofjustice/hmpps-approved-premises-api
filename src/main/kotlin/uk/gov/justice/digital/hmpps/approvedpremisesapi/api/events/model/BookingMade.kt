package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 *
 * @param applicationId The UUID of an application for an AP place
 * @param applicationUrl The URL on the Approved Premises service at which a user can view a representation of an AP application and related resources, including bookings
 * @param bookingId The UUID of booking for an AP place
 * @param personReference
 * @param deliusEventNumber Used in Delius to identify the 'event' via the first active conviction's 'index'
 * @param createdAt
 * @param bookedBy
 * @param premises
 * @param arrivalOn
 * @param departureOn
 * @param applicationSubmittedOn
 * @param releaseType
 * @param sentenceType
 * @param situation
 */
data class BookingMade(

  @Schema(example = "484b8b5e-6c3b-4400-b200-425bbe410713", required = true, description = "The UUID of an application for an AP place")
  @get:JsonProperty("applicationId", required = true) val applicationId: java.util.UUID,

  @Schema(example = "https://approved-premises-dev.hmpps.service.justice.gov.uk/applications/484b8b5e-6c3b-4400-b200-425bbe410713", required = true, description = "The URL on the Approved Premises service at which a user can view a representation of an AP application and related resources, including bookings")
  @get:JsonProperty("applicationUrl", required = true) val applicationUrl: kotlin.String,

  @Schema(example = "14c80733-4b6d-4f35-b724-66955aac320c", required = true, description = "The UUID of booking for an AP place")
  @get:JsonProperty("bookingId", required = true) val bookingId: java.util.UUID,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("personReference", required = true) val personReference: PersonReference,

  @Schema(example = "7", required = true, description = "Used in Delius to identify the 'event' via the first active conviction's 'index'")
  @get:JsonProperty("deliusEventNumber", required = true) val deliusEventNumber: kotlin.String,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("createdAt", required = true) val createdAt: java.time.Instant,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("bookedBy", required = true) val bookedBy: BookingMadeBookedBy,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("premises", required = true) val premises: Premises,

  @Schema(example = "Mon Jan 30 00:00:00 GMT 2023", required = true, description = "")
  @get:JsonProperty("arrivalOn", required = true) val arrivalOn: java.time.LocalDate,

  @Schema(example = "Sun Apr 30 01:00:00 BST 2023", required = true, description = "")
  @get:JsonProperty("departureOn", required = true) val departureOn: java.time.LocalDate,

  @Schema(example = "null", description = "")
  @get:JsonProperty("applicationSubmittedOn") val applicationSubmittedOn: java.time.Instant? = null,

  @Schema(example = "null", description = "")
  @get:JsonProperty("releaseType") val releaseType: kotlin.String? = null,

  @Schema(example = "null", description = "")
  @get:JsonProperty("sentenceType") val sentenceType: kotlin.String? = null,

  @Schema(example = "null", description = "")
  @get:JsonProperty("situation") val situation: kotlin.String? = null,
)
