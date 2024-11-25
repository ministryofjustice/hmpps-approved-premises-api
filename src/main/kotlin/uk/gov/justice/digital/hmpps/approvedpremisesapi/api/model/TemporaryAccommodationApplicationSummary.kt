package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 *
 * @param createdByUserId
 * @param status
 * @param risks
 */
data class TemporaryAccommodationApplicationSummary(

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("createdByUserId", required = true) val createdByUserId: java.util.UUID,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("status", required = true) val status: ApplicationStatus,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("type", required = true) override val type: kotlin.String,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("id", required = true) override val id: java.util.UUID,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("person", required = true) override val person: Person,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("createdAt", required = true) override val createdAt: java.time.Instant,

  @Schema(example = "null", description = "")
  @get:JsonProperty("risks") val risks: PersonRisks? = null,

  @Schema(example = "null", description = "")
  @get:JsonProperty("submittedAt") override val submittedAt: java.time.Instant? = null,
) : ApplicationSummary {
}
