package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 *
 * @param id
 * @param appealDate
 * @param appealDetail
 * @param decision
 * @param decisionDetail
 * @param createdAt
 * @param applicationId
 * @param createdByUser
 * @param assessmentId
 */
data class Appeal(

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("id", required = true) val id: java.util.UUID,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("appealDate", required = true) val appealDate: java.time.LocalDate,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("appealDetail", required = true) val appealDetail: kotlin.String,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("decision", required = true) val decision: AppealDecision,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("decisionDetail", required = true) val decisionDetail: kotlin.String,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("createdAt", required = true) val createdAt: java.time.Instant,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("applicationId", required = true) val applicationId: java.util.UUID,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("createdByUser", required = true) val createdByUser: User,

  @Schema(example = "null", description = "")
  @get:JsonProperty("assessmentId") val assessmentId: java.util.UUID? = null,
)
