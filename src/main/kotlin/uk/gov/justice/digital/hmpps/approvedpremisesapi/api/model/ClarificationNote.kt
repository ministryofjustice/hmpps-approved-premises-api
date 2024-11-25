package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 *
 * @param id
 * @param createdAt
 * @param createdByStaffMemberId
 * @param query
 * @param responseReceivedOn
 * @param response
 */
data class ClarificationNote(

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("id", required = true) val id: java.util.UUID,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("createdAt", required = true) val createdAt: java.time.Instant,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("createdByStaffMemberId", required = true) val createdByStaffMemberId: java.util.UUID,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("query", required = true) val query: kotlin.String,

  @Schema(example = "null", description = "")
  @get:JsonProperty("responseReceivedOn") val responseReceivedOn: java.time.LocalDate? = null,

  @Schema(example = "null", description = "")
  @get:JsonProperty("response") val response: kotlin.String? = null,
)
