package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto

import com.fasterxml.jackson.annotation.JsonProperty
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.AssessmentDecision
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Person
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PersonRisks
import java.time.Instant
import java.util.UUID

data class Cas1AssessmentSummary(

  @get:JsonProperty("id", required = true) val id: UUID,

  @get:JsonProperty("applicationId", required = true) val applicationId: UUID,

  @get:JsonProperty("createdAt", required = true) val createdAt: Instant,

  @get:JsonProperty("person", required = true) val person: Person,

  @get:JsonProperty("status", required = true) val status: Cas1AssessmentStatus,

  @get:JsonProperty("dueAt", required = true) val dueAt: Instant,

  @get:JsonProperty("arrivalDate") val arrivalDate: Instant? = null,

  @get:JsonProperty("dateOfInfoRequest") val dateOfInfoRequest: Instant? = null,

  @get:JsonProperty("decision") val decision: AssessmentDecision? = null,

  @get:JsonProperty("risks") val risks: PersonRisks? = null,
)
