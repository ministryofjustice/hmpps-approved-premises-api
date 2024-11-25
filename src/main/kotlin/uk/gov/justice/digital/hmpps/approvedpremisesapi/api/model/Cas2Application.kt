package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 *
 * @param createdBy
 * @param schemaVersion
 * @param outdatedSchema
 * @param status
 * @param &#x60;data&#x60; Any object that conforms to the current JSON schema for an application
 * @param document Any object that conforms to the current JSON schema for an application
 * @param submittedAt
 * @param telephoneNumber
 * @param assessment
 * @param timelineEvents
 */
data class Cas2Application(

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("createdBy", required = true) val createdBy: NomisUser,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("schemaVersion", required = true) val schemaVersion: java.util.UUID,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("outdatedSchema", required = true) val outdatedSchema: kotlin.Boolean,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("status", required = true) val status: ApplicationStatus,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("type", required = true) override val type: kotlin.String,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("id", required = true) override val id: java.util.UUID,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("person", required = true) override val person: Person,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("createdAt", required = true) override val createdAt: java.time.Instant,

  @Schema(example = "null", description = "Any object that conforms to the current JSON schema for an application")
  @get:JsonProperty("data") val `data`: kotlin.Any? = null,

  @Schema(example = "null", description = "Any object that conforms to the current JSON schema for an application")
  @get:JsonProperty("document") val document: kotlin.Any? = null,

  @Schema(example = "null", description = "")
  @get:JsonProperty("submittedAt") val submittedAt: java.time.Instant? = null,

  @Schema(example = "null", description = "")
  @get:JsonProperty("telephoneNumber") val telephoneNumber: kotlin.String? = null,

  @Schema(example = "null", description = "")
  @get:JsonProperty("assessment") val assessment: Cas2Assessment? = null,

  @Schema(example = "null", description = "")
  @get:JsonProperty("timelineEvents") val timelineEvents: kotlin.collections.List<Cas2TimelineEvent>? = null,
) : Application
