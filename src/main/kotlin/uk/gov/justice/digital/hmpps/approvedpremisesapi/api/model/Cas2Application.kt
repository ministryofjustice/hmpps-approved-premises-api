package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.Cas2User

/**
 * 
 * @param createdBy 
 * @param schemaVersion 
 * @param outdatedSchema 
 * @param status 
 * @param isTransferredApplication 
 * @param cas2CreatedBy 
 * @param &#x60;data&#x60; Any object
 * @param document Any object
 * @param submittedAt 
 * @param telephoneNumber 
 * @param assessment 
 * @param timelineEvents 
 * @param allocatedPomName 
 * @param currentPrisonName 
 * @param allocatedPomEmailAddress 
 * @param omuEmailAddress 
 * @param assignmentDate 
 * @param applicationOrigin 
 * @param bailHearingDate 
 */
data class Cas2Application(

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("createdBy", required = true) val createdBy: NomisUser,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("schemaVersion", required = true) val schemaVersion: UUID,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("outdatedSchema", required = true) val outdatedSchema: Boolean,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("status", required = true) val status: ApplicationStatus,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("isTransferredApplication", required = true) val isTransferredApplication: Boolean,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("type", required = true) override val type: String,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("id", required = true) override val id: UUID,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("person", required = true) override val person: Person,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("createdAt", required = true) override val createdAt: Instant,

    @Schema(example = "null", description = "")
    @get:JsonProperty("cas2CreatedBy") val cas2CreatedBy: Cas2User? = null,

    @Schema(example = "null", description = "Any object")
    @get:JsonProperty("data") val `data`: Any? = null,

    @Schema(example = "null", description = "Any object")
    @get:JsonProperty("document") val document: Any? = null,

    @Schema(example = "null", description = "")
    @get:JsonProperty("submittedAt") val submittedAt: Instant? = null,

    @Schema(example = "null", description = "")
    @get:JsonProperty("telephoneNumber") val telephoneNumber: String? = null,

    @Schema(example = "null", description = "")
    @get:JsonProperty("assessment") val assessment: Cas2Assessment? = null,

    @Schema(example = "null", description = "")
    @get:JsonProperty("timelineEvents") val timelineEvents: List<Cas2TimelineEvent>? = null,

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

    @Schema(example = "null", description = "")
    @get:JsonProperty("applicationOrigin") val applicationOrigin: ApplicationOrigin? = ApplicationOrigin.homeDetentionCurfew,

    @Schema(example = "null", description = "")
    @get:JsonProperty("bailHearingDate") val bailHearingDate: LocalDate? = null
    ) : Application{

}

