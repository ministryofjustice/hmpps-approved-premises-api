package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty

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

  @get:JsonProperty("id", required = true) val id: java.util.UUID,

  @get:JsonProperty("createdAt", required = true) val createdAt: java.time.Instant,

  @get:JsonProperty("clarificationNotes", required = true) val clarificationNotes: kotlin.collections.List<Cas1ClarificationNote>,

  @get:JsonProperty("application", required = true) val application: Cas1Application,

  @get:JsonProperty("createdFromAppeal", required = true) val createdFromAppeal: kotlin.Boolean,

  @get:JsonProperty("allocatedAt") val allocatedAt: java.time.Instant? = null,

  @get:JsonProperty("submittedAt") val submittedAt: java.time.Instant? = null,

  @get:JsonProperty("decision") val decision: AssessmentDecision? = null,

  @get:JsonProperty("rejectionRationale") val rejectionRationale: kotlin.String? = null,

  @get:JsonProperty("data") val `data`: kotlin.Any? = null,

  @get:JsonProperty("allocatedToStaffMember") val allocatedToStaffMember: ApprovedPremisesUser? = null,

  @get:JsonProperty("status") val status: Cas1AssessmentStatus? = null,

  @get:JsonProperty("document") val document: kotlin.Any? = null,
)
