package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty

data class Cas1Assessment(

  @get:JsonProperty("id", required = true) val id: java.util.UUID,

  @get:JsonProperty("createdAt", required = true) val createdAt: java.time.Instant,

  @get:JsonProperty("clarificationNotes", required = true) val clarificationNotes: List<Cas1ClarificationNote>,

  @get:JsonProperty("application", required = true) val application: Cas1Application,

  @get:JsonProperty("createdFromAppeal", required = true) val createdFromAppeal: Boolean,

  @get:JsonProperty("allocatedAt") val allocatedAt: java.time.Instant? = null,

  @get:JsonProperty("submittedAt") val submittedAt: java.time.Instant? = null,

  @get:JsonProperty("decision") val decision: AssessmentDecision? = null,

  @get:JsonProperty("rejectionRationale") val rejectionRationale: String? = null,

  @get:JsonProperty("data") val `data`: Any? = null,

  @get:JsonProperty("allocatedToStaffMember") val allocatedToStaffMember: ApprovedPremisesUser? = null,

  @get:JsonProperty("status") val status: Cas1AssessmentStatus? = null,

  @get:JsonProperty("document") val document: Any? = null,
)
