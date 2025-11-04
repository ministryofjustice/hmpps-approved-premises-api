package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty

data class ApprovedPremisesAssessment(

  @get:JsonProperty("application", required = true) val application: ApprovedPremisesApplication,

  @get:JsonProperty("createdFromAppeal", required = true) val createdFromAppeal: kotlin.Boolean,

  @get:JsonProperty("service", required = true) override val service: kotlin.String,

  @get:JsonProperty("id", required = true) override val id: java.util.UUID,

  @get:JsonProperty("createdAt", required = true) override val createdAt: java.time.Instant,

  @get:JsonProperty("clarificationNotes", required = true) override val clarificationNotes: kotlin.collections.List<ClarificationNote>,

  @get:JsonProperty("allocatedToStaffMember") val allocatedToStaffMember: ApprovedPremisesUser? = null,

  @get:JsonProperty("status") val status: ApprovedPremisesAssessmentStatus? = null,

  @get:JsonProperty("document") val document: kotlin.Any? = null,

  @get:JsonProperty("allocatedAt") override val allocatedAt: java.time.Instant? = null,

  @get:JsonProperty("submittedAt") override val submittedAt: java.time.Instant? = null,

  @get:JsonProperty("decision") override val decision: AssessmentDecision? = null,

  @get:JsonProperty("rejectionRationale") override val rejectionRationale: kotlin.String? = null,

  @get:JsonProperty("data") override val `data`: kotlin.Any? = null,
) : Assessment
