package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model.Cas2TimelineEvent

/**
 * 
 * @param createdBy 
 * @param schemaVersion 
 * @param outdatedSchema 
 * @param status 
 * @param applicationOrigin 
 * @param &#x60;data&#x60; Any object
 * @param document Any object
 * @param submittedAt 
 * @param telephoneNumber 
 * @param assessment 
 * @param timelineEvents 
 * @param bailHearingDate 
 */
data class Cas2v2Application(

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("createdBy", required = true) val createdBy: Cas2v2User,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("status", required = true) val status: ApplicationStatus,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("applicationOrigin", required = true) val applicationOrigin: ApplicationOrigin = ApplicationOrigin.homeDetentionCurfew,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("type", required = true) override val type: String,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("id", required = true) override val id: UUID,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("person", required = true) override val person: Person,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("createdAt", required = true) override val createdAt: Instant,

    @Schema(example = "null", description = "Any object")
    @get:JsonProperty("data") val `data`: Any? = null,

    @Schema(example = "null", description = "Any object")
    @get:JsonProperty("document") val document: Any? = null,

    @Schema(example = "null", description = "")
    @get:JsonProperty("submittedAt") val submittedAt: Instant? = null,

    @Schema(example = "null", description = "")
    @get:JsonProperty("telephoneNumber") val telephoneNumber: String? = null,

    @Schema(example = "null", description = "")
    @get:JsonProperty("assessment") val assessment: Cas2v2Assessment? = null,

    @Schema(example = "null", description = "")
    @get:JsonProperty("timelineEvents") val timelineEvents: List<Cas2TimelineEvent>? = null,

    @Schema(example = "null", description = "")
    @get:JsonProperty("bailHearingDate") val bailHearingDate: LocalDate? = null
    ) : Application{

}

