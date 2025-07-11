package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model.Cas2TimelineEvent

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
 * @param document Any object
 * @param submittedAt
 * @param telephoneNumber
 * @param applicationOrigin
 * @param bailHearingDate
 */
data class Cas2v2SubmittedApplication(

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("id", required = true) val id: UUID,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("person", required = true) val person: Person,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("createdAt", required = true) val createdAt: Instant,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("timelineEvents", required = true) val timelineEvents: List<Cas2TimelineEvent>,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("assessment", required = true) val assessment: Cas2v2Assessment,

  @Schema(example = "null", description = "")
  @get:JsonProperty("submittedBy") val submittedBy: Cas2v2User? = null,

  @Schema(example = "null", description = "Any object")
  @get:JsonProperty("document") val document: Any? = null,

  @Schema(example = "null", description = "")
  @get:JsonProperty("submittedAt") val submittedAt: Instant? = null,

  @Schema(example = "null", description = "")
  @get:JsonProperty("telephoneNumber") val telephoneNumber: String? = null,

  @Schema(example = "null", description = "")
  @get:JsonProperty("applicationOrigin") val applicationOrigin: ApplicationOrigin? = ApplicationOrigin.homeDetentionCurfew,

  @Schema(example = "null", description = "")
  @get:JsonProperty("bailHearingDate") val bailHearingDate: LocalDate? = null,
)
