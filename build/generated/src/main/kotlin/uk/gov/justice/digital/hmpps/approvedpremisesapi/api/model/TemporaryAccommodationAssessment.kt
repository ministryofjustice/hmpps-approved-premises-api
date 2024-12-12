package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonValue
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Assessment
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.AssessmentDecision
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ClarificationNote
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ReferralHistoryNote
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.TemporaryAccommodationApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.TemporaryAccommodationAssessmentStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.TemporaryAccommodationUser
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 
 * @param application 
 * @param summaryData Any object that conforms to the current JSON schema for an application
 * @param allocatedToStaffMember 
 * @param status 
 * @param releaseDate 
 * @param accommodationRequiredFromDate 
 */
data class TemporaryAccommodationAssessment(

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("application", required = true) val application: TemporaryAccommodationApplication,

    @Schema(example = "null", required = true, description = "Any object that conforms to the current JSON schema for an application")
    @get:JsonProperty("summaryData", required = true) val summaryData: kotlin.Any,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("service", required = true) override val service: kotlin.String,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("id", required = true) override val id: java.util.UUID,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("schemaVersion", required = true) override val schemaVersion: java.util.UUID,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("outdatedSchema", required = true) override val outdatedSchema: kotlin.Boolean,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("createdAt", required = true) override val createdAt: java.time.Instant,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("clarificationNotes", required = true) override val clarificationNotes: kotlin.collections.List<ClarificationNote>,

    @Schema(example = "null", description = "")
    @get:JsonProperty("allocatedToStaffMember") val allocatedToStaffMember: TemporaryAccommodationUser? = null,

    @Schema(example = "null", description = "")
    @get:JsonProperty("status") val status: TemporaryAccommodationAssessmentStatus? = null,

    @Schema(example = "null", description = "")
    @get:JsonProperty("releaseDate") val releaseDate: java.time.LocalDate? = null,

    @Schema(example = "null", description = "")
    @get:JsonProperty("accommodationRequiredFromDate") val accommodationRequiredFromDate: java.time.LocalDate? = null,

    @Schema(example = "null", description = "")
    @get:JsonProperty("allocatedAt") override val allocatedAt: java.time.Instant? = null,

    @Schema(example = "null", description = "")
    @get:JsonProperty("submittedAt") override val submittedAt: java.time.Instant? = null,

    @Schema(example = "null", description = "")
    @get:JsonProperty("decision") override val decision: AssessmentDecision? = null,

    @Schema(example = "null", description = "")
    @get:JsonProperty("rejectionRationale") override val rejectionRationale: kotlin.String? = null,

    @Schema(example = "null", description = "Any object that conforms to the current JSON schema for an application")
    @get:JsonProperty("data") override val `data`: kotlin.Any? = null,

    @Schema(example = "null", description = "")
    @get:JsonProperty("referralHistoryNotes") override val referralHistoryNotes: kotlin.collections.List<ReferralHistoryNote>? = null
) : Assessment{

}

