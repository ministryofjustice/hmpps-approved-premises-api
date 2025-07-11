package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas3.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

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

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("personReference", required = true) val personReference: PersonReference,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("bookingId", required = true) val bookingId: java.util.UUID,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("bookingUrl", required = true) val bookingUrl: java.net.URI,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("expectedArrivedAt", required = true) val expectedArrivedAt: java.time.Instant,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("notes", required = true) val notes: kotlin.String,

  @Schema(example = "null", description = "")
  @get:JsonProperty("applicationId") val applicationId: java.util.UUID? = null,

  @Schema(example = "null", description = "")
  @get:JsonProperty("applicationUrl") val applicationUrl: java.net.URI? = null,

  @Schema(example = "null", description = "")
  @get:JsonProperty("bookedBy") val bookedBy: StaffMember? = null,
)
