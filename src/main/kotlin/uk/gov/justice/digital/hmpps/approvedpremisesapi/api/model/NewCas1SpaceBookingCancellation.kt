package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 *
 * @param occurredAt
 * @param reasonId
 * @param reasonNotes
 */
data class NewCas1SpaceBookingCancellation(

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("occurredAt", required = true) val occurredAt: java.time.LocalDate,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("reasonId", required = true) val reasonId: java.util.UUID,

  @Schema(example = "null", description = "")
  @get:JsonProperty("reasonNotes") val reasonNotes: kotlin.String? = null,
)
