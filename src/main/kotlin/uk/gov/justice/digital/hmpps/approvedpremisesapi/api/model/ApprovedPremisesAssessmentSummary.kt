package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * Please use the Cas1AssessmentSummary endpoint instead
 * @param status
 * @param dueAt
 */
data class ApprovedPremisesAssessmentSummary(

  @get:JsonProperty("status", required = true) val status: ApprovedPremisesAssessmentStatus,

  @get:JsonProperty("dueAt", required = true) val dueAt: java.time.Instant,

  @get:JsonProperty("type", required = true) override val type: kotlin.String,

  @get:JsonProperty("id", required = true) override val id: java.util.UUID,

  @get:JsonProperty("applicationId", required = true) override val applicationId: java.util.UUID,

  @get:JsonProperty("createdAt", required = true) override val createdAt: java.time.Instant,

  @get:JsonProperty("person", required = true) override val person: Person,

  @get:JsonProperty("arrivalDate") override val arrivalDate: java.time.Instant? = null,

  @get:JsonProperty("dateOfInfoRequest") override val dateOfInfoRequest: java.time.Instant? = null,

  @get:JsonProperty("decision") override val decision: AssessmentDecision? = null,

  @get:JsonProperty("risks") override val risks: PersonRisks? = null,
) : AssessmentSummary
