package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model

import com.fasterxml.jackson.annotation.JsonProperty
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NomisUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Person
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

/**
 *
 * @param id
 * @param person
 * @param createdAt
 * @param schemaVersion
 * @param outdatedSchema
 * @param timelineEvents
 * @param assessment
 * @param isTransferredApplication
 * @param submittedBy
 * @param document Any object
 * @param submittedAt
 * @param telephoneNumber
 * @param allocatedPomName
 * @param currentPrisonName
 * @param allocatedPomEmailAddress
 * @param omuEmailAddress
 * @param assignmentDate
 */
data class Cas2SubmittedApplication(

  @get:JsonProperty("id", required = true) val id: UUID,

  @get:JsonProperty("person", required = true) val person: Person,

  @get:JsonProperty("createdAt", required = true) val createdAt: Instant,

  @get:JsonProperty("timelineEvents", required = true) val timelineEvents: List<Cas2TimelineEvent>,

  @get:JsonProperty("assessment", required = true) val assessment: Cas2Assessment,

  @get:JsonProperty("isTransferredApplication", required = true) val isTransferredApplication: Boolean,

  @get:JsonProperty("submittedBy") val submittedBy: NomisUser? = null,

  @get:JsonProperty("document") val document: Any? = null,

  @get:JsonProperty("submittedAt") val submittedAt: Instant? = null,

  @get:JsonProperty("telephoneNumber") val telephoneNumber: String? = null,

  @get:JsonProperty("allocatedPomName") val allocatedPomName: String? = null,

  @get:JsonProperty("currentPrisonName") val currentPrisonName: String? = null,

  @get:JsonProperty("allocatedPomEmailAddress") val allocatedPomEmailAddress: String? = null,

  @get:JsonProperty("omuEmailAddress") val omuEmailAddress: String? = null,

  @get:JsonProperty("assignmentDate") val assignmentDate: LocalDate? = null,
)
