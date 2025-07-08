package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model.generated

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas2TimelineEvent
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

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("id", required = true) val id: UUID,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("person", required = true) val person: Person,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("createdAt", required = true) val createdAt: Instant,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("schemaVersion", required = true) val schemaVersion: UUID,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("outdatedSchema", required = true) val outdatedSchema: Boolean,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("timelineEvents", required = true) val timelineEvents: List<Cas2TimelineEvent>,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("assessment", required = true) val assessment: Cas2Assessment,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("isTransferredApplication", required = true) val isTransferredApplication: Boolean,

  @Schema(example = "null", description = "")
  @get:JsonProperty("submittedBy") val submittedBy: NomisUser? = null,

  @Schema(example = "null", description = "Any object")
  @get:JsonProperty("document") val document: Any? = null,

  @Schema(example = "null", description = "")
  @get:JsonProperty("submittedAt") val submittedAt: Instant? = null,

  @Schema(example = "null", description = "")
  @get:JsonProperty("telephoneNumber") val telephoneNumber: String? = null,

  @Schema(example = "null", description = "")
  @get:JsonProperty("allocatedPomName") val allocatedPomName: String? = null,

  @Schema(example = "null", description = "")
  @get:JsonProperty("currentPrisonName") val currentPrisonName: String? = null,

  @Schema(example = "null", description = "")
  @get:JsonProperty("allocatedPomEmailAddress") val allocatedPomEmailAddress: String? = null,

  @Schema(example = "null", description = "")
  @get:JsonProperty("omuEmailAddress") val omuEmailAddress: String? = null,

  @Schema(example = "null", description = "")
  @get:JsonProperty("assignmentDate") val assignmentDate: LocalDate? = null,
)
