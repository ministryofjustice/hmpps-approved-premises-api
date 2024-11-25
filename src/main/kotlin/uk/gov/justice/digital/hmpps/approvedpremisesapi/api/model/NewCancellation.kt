package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 *
 * @param date
 * @param reason
 * @param notes
 * @param otherReason
 */
data class NewCancellation(

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("date", required = true) val date: java.time.LocalDate,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("reason", required = true) val reason: java.util.UUID,

  @Schema(example = "null", description = "")
  @get:JsonProperty("notes") val notes: kotlin.String? = null,

  @Schema(example = "null", description = "")
  @get:JsonProperty("otherReason") val otherReason: kotlin.String? = null,
)
