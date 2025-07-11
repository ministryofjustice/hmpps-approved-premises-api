package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model

import com.fasterxml.jackson.annotation.JsonProperty
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Assessment
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.AssessmentDecision
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ClarificationNote
import io.swagger.v3.oas.annotations.media.Schema
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

/**
 * 
 * @param application 
 * @param summaryData Any object
 * @param allocatedToStaffMember 
 * @param status 
 * @param releaseDate 
 * @param accommodationRequiredFromDate 
 */
data class TemporaryAccommodationAssessment(

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("application", required = true) val application: TemporaryAccommodationApplication,

    @Schema(example = "null", required = true, description = "Any object")
    @get:JsonProperty("summaryData", required = true) val summaryData: Any,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("service", required = true) override val service: String,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("id", required = true) override val id: UUID,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("schemaVersion", required = true) override val schemaVersion: UUID,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("outdatedSchema", required = true) override val outdatedSchema: Boolean,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("createdAt", required = true) override val createdAt: Instant,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("clarificationNotes", required = true) override val clarificationNotes: List<ClarificationNote>,

    @Schema(example = "null", description = "")
    @get:JsonProperty("allocatedToStaffMember") val allocatedToStaffMember: TemporaryAccommodationUser? = null,

    @Schema(example = "null", description = "")
    @get:JsonProperty("status") val status: TemporaryAccommodationAssessmentStatus? = null,

    @Schema(example = "null", description = "")
    @get:JsonProperty("releaseDate") val releaseDate: LocalDate? = null,

    @Schema(example = "null", description = "")
    @get:JsonProperty("accommodationRequiredFromDate") val accommodationRequiredFromDate: LocalDate? = null,

    @Schema(example = "null", description = "")
    @get:JsonProperty("allocatedAt") override val allocatedAt: Instant? = null,

    @Schema(example = "null", description = "")
    @get:JsonProperty("submittedAt") override val submittedAt: Instant? = null,

    @Schema(example = "null", description = "")
    @get:JsonProperty("decision") override val decision: AssessmentDecision? = null,

    @Schema(example = "null", description = "")
    @get:JsonProperty("rejectionRationale") override val rejectionRationale: String? = null,

    @Schema(example = "null", description = "Any object")
    @get:JsonProperty("data") override val `data`: Any? = null
    ) : Assessment{

}

