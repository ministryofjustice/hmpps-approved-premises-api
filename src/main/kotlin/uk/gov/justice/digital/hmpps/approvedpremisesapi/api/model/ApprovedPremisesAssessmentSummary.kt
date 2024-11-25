package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 *
 * @param status
 * @param dueAt
 */
data class ApprovedPremisesAssessmentSummary(

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("status", required = true) val status: ApprovedPremisesAssessmentStatus,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("dueAt", required = true) val dueAt: java.time.Instant,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("type", required = true) override val type: kotlin.String,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("id", required = true) override val id: java.util.UUID,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("applicationId", required = true) override val applicationId: java.util.UUID,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("createdAt", required = true) override val createdAt: java.time.Instant,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("person", required = true) override val person: Person,

  @Schema(example = "null", description = "")
  @get:JsonProperty("arrivalDate") override val arrivalDate: java.time.Instant? = null,

  @Schema(example = "null", description = "")
  @get:JsonProperty("dateOfInfoRequest") override val dateOfInfoRequest: java.time.Instant? = null,

  @Schema(example = "null", description = "")
  @get:JsonProperty("decision") override val decision: AssessmentDecision? = null,

  @Schema(example = "null", description = "")
  @get:JsonProperty("risks") override val risks: PersonRisks? = null,
) : AssessmentSummary
