package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 *
 * @param id
 * @param person
 * @param createdAt
 * @param schemaVersion
 * @param outdatedSchema
 * @param timelineEvents
 * @param assessment
 * @param submittedBy
 * @param document Any object that conforms to the current JSON schema for an application
 * @param submittedAt
 * @param telephoneNumber
 */
data class Cas2SubmittedApplication(

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("id", required = true) val id: java.util.UUID,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("person", required = true) val person: Person,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("createdAt", required = true) val createdAt: java.time.Instant,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("schemaVersion", required = true) val schemaVersion: java.util.UUID,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("outdatedSchema", required = true) val outdatedSchema: kotlin.Boolean,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("timelineEvents", required = true) val timelineEvents: kotlin.collections.List<Cas2TimelineEvent>,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("assessment", required = true) val assessment: Cas2Assessment,

  @Schema(example = "null", description = "")
  @get:JsonProperty("submittedBy") val submittedBy: NomisUser? = null,

  @Schema(example = "null", description = "Any object that conforms to the current JSON schema for an application")
  @get:JsonProperty("document") val document: kotlin.Any? = null,

  @Schema(example = "null", description = "")
  @get:JsonProperty("submittedAt") val submittedAt: java.time.Instant? = null,

  @Schema(example = "null", description = "")
  @get:JsonProperty("telephoneNumber") val telephoneNumber: kotlin.String? = null,
)
