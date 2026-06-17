package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto

import com.fasterxml.jackson.annotation.JsonProperty
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApprovedPremisesUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.AssessmentDecision
import java.time.Instant
import java.util.UUID

data class Cas1Assessment(

  @get:JsonProperty("id", required = true) val id: UUID,

  @get:JsonProperty("createdAt", required = true) val createdAt: Instant,

  @get:JsonProperty("clarificationNotes", required = true) val clarificationNotes: List<Cas1ClarificationNote>,

  @get:JsonProperty("application", required = true) val application: Cas1Application,

  @get:JsonProperty("createdFromAppeal", required = true) val createdFromAppeal: Boolean,

  @get:JsonProperty("allocatedAt") val allocatedAt: Instant? = null,

  @get:JsonProperty("submittedAt") val submittedAt: Instant? = null,

  @get:JsonProperty("decision") val decision: AssessmentDecision? = null,

  @get:JsonProperty("rejectionRationale") val rejectionRationale: String? = null,

  @get:JsonProperty("data") val `data`: Any? = null,

  @get:JsonProperty("allocatedToStaffMember") val allocatedToStaffMember: ApprovedPremisesUser? = null,

  @get:JsonProperty("status") val status: Cas1AssessmentStatus? = null,

  @get:JsonProperty("document") val document: Any? = null,
)
