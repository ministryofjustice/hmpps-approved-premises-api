package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas3.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

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

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("personReference", required = true) val personReference: PersonReference,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("bookingId", required = true) val bookingId: java.util.UUID,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("bookingUrl", required = true) val bookingUrl: java.net.URI,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("cancellationReason", required = true) val cancellationReason: kotlin.String,

  @Schema(example = "null", description = "")
  @get:JsonProperty("applicationId") val applicationId: java.util.UUID? = null,

  @Schema(example = "null", description = "")
  @get:JsonProperty("applicationUrl") val applicationUrl: java.net.URI? = null,

  @Schema(example = "null", description = "")
  @get:JsonProperty("cancelledAt") val cancelledAt: java.time.LocalDate? = null,

  @Schema(example = "null", description = "")
  @get:JsonProperty("notes") val notes: kotlin.String? = null,

  @Schema(example = "null", description = "")
  @get:JsonProperty("cancelledBy") val cancelledBy: StaffMember? = null,
)
