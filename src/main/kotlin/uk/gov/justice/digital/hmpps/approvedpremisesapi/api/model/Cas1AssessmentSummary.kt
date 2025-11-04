package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty

data class Cas1AssessmentSummary(

  @get:JsonProperty("id", required = true) val id: java.util.UUID,

  @get:JsonProperty("applicationId", required = true) val applicationId: java.util.UUID,

  @get:JsonProperty("createdAt", required = true) val createdAt: java.time.Instant,

  @get:JsonProperty("person", required = true) val person: Person,

  @get:JsonProperty("status", required = true) val status: Cas1AssessmentStatus,

  @get:JsonProperty("dueAt", required = true) val dueAt: java.time.Instant,

  @get:JsonProperty("arrivalDate") val arrivalDate: java.time.Instant? = null,

  @get:JsonProperty("dateOfInfoRequest") val dateOfInfoRequest: java.time.Instant? = null,

  @get:JsonProperty("decision") val decision: AssessmentDecision? = null,

  @get:JsonProperty("risks") val risks: PersonRisks? = null,
)
