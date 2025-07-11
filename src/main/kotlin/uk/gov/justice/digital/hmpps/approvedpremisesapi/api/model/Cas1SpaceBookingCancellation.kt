package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 *
 * @param occurredAt
 * @param recordedAt
 * @param reason
 * @param reasonNotes
 */
data class Cas1SpaceBookingCancellation(

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("occurredAt", required = true) val occurredAt: java.time.LocalDate,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("recordedAt", required = true) val recordedAt: java.time.Instant,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("reason", required = true) val reason: CancellationReason,

  @Schema(example = "null", description = "")
  @get:JsonProperty("reason_notes") val reasonNotes: kotlin.String? = null,
)
