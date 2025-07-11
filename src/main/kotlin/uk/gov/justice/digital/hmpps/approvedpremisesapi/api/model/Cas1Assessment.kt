package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 *
 * @param id
 * @param schemaVersion
 * @param outdatedSchema
 * @param createdAt
 * @param clarificationNotes
 * @param application
 * @param createdFromAppeal
 * @param allocatedAt
 * @param submittedAt
 * @param decision
 * @param rejectionRationale
 * @param &#x60;data&#x60; Any object
 * @param allocatedToStaffMember
 * @param status
 * @param document Any object
 */
data class Cas1Assessment(

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("id", required = true) val id: java.util.UUID,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("createdAt", required = true) val createdAt: java.time.Instant,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("clarificationNotes", required = true) val clarificationNotes: kotlin.collections.List<Cas1ClarificationNote>,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("application", required = true) val application: Cas1Application,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("createdFromAppeal", required = true) val createdFromAppeal: kotlin.Boolean,

  @Schema(example = "null", description = "")
  @get:JsonProperty("allocatedAt") val allocatedAt: java.time.Instant? = null,

  @Schema(example = "null", description = "")
  @get:JsonProperty("submittedAt") val submittedAt: java.time.Instant? = null,

  @Schema(example = "null", description = "")
  @get:JsonProperty("decision") val decision: AssessmentDecision? = null,

  @Schema(example = "null", description = "")
  @get:JsonProperty("rejectionRationale") val rejectionRationale: kotlin.String? = null,

  @Schema(example = "null", description = "Any object")
  @get:JsonProperty("data") val `data`: kotlin.Any? = null,

  @Schema(example = "null", description = "")
  @get:JsonProperty("allocatedToStaffMember") val allocatedToStaffMember: ApprovedPremisesUser? = null,

  @Schema(example = "null", description = "")
  @get:JsonProperty("status") val status: Cas1AssessmentStatus? = null,

  @Schema(example = "null", description = "Any object")
  @get:JsonProperty("document") val document: kotlin.Any? = null,
)
