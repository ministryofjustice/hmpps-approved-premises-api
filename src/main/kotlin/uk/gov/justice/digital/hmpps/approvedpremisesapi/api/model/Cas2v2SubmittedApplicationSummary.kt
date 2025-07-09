package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

/**
 * 
 * @param id 
 * @param createdByUserId 
 * @param crn 
 * @param personName 
 * @param createdAt 
 * @param nomsNumber 
 * @param submittedAt 
 * @param applicationOrigin 
 * @param bailHearingDate 
 */
data class Cas2v2SubmittedApplicationSummary(

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("id", required = true) val id: UUID,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("createdByUserId", required = true) val createdByUserId: UUID,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("crn", required = true) val crn: String,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("personName", required = true) val personName: String,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("createdAt", required = true) val createdAt: Instant,

    @Schema(example = "null", description = "")
    @get:JsonProperty("nomsNumber") val nomsNumber: String? = null,

    @Schema(example = "null", description = "")
    @get:JsonProperty("submittedAt") val submittedAt: Instant? = null,

    @Schema(example = "null", description = "")
    @get:JsonProperty("applicationOrigin") val applicationOrigin: ApplicationOrigin? = ApplicationOrigin.homeDetentionCurfew,

    @Schema(example = "null", description = "")
    @get:JsonProperty("bailHearingDate") val bailHearingDate: LocalDate? = null
    ) {

}

