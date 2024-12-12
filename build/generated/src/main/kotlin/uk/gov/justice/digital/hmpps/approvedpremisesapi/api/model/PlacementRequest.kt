package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonValue
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApprovedPremisesUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.AssessmentDecision
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.BookingSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Gender
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Person
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PersonRisks
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PlacementCriteria
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PlacementRequestRequestType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PlacementRequestStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ReleaseTypeOption
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.WithdrawPlacementRequestReason
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 
 * @param gender 
 * @param type 
 * @param location 
 * @param radius 
 * @param essentialCriteria 
 * @param desirableCriteria 
 * @param expectedArrival 
 * @param duration 
 * @param id 
 * @param person 
 * @param risks 
 * @param applicationId 
 * @param assessmentId 
 * @param releaseType 
 * @param status 
 * @param assessmentDecision 
 * @param assessmentDate 
 * @param applicationDate 
 * @param assessor 
 * @param isParole 
 * @param isWithdrawn 
 * @param notes 
 * @param booking 
 * @param requestType 
 * @param withdrawalReason 
 */
data class PlacementRequest(

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("gender", required = true) val gender: Gender,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("type", required = true) val type: ApType,

    @Schema(example = "B74", required = true, description = "")
    @get:JsonProperty("location", required = true) val location: kotlin.String,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("radius", required = true) val radius: kotlin.Int,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("essentialCriteria", required = true) val essentialCriteria: kotlin.collections.List<PlacementCriteria>,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("desirableCriteria", required = true) val desirableCriteria: kotlin.collections.List<PlacementCriteria>,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("expectedArrival", required = true) val expectedArrival: java.time.LocalDate,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("duration", required = true) val duration: kotlin.Int,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("id", required = true) val id: java.util.UUID,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("person", required = true) val person: Person,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("risks", required = true) val risks: PersonRisks,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("applicationId", required = true) val applicationId: java.util.UUID,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("assessmentId", required = true) val assessmentId: java.util.UUID,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("releaseType", required = true) val releaseType: ReleaseTypeOption,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("status", required = true) val status: PlacementRequestStatus,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("assessmentDecision", required = true) val assessmentDecision: AssessmentDecision,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("assessmentDate", required = true) val assessmentDate: java.time.Instant,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("applicationDate", required = true) val applicationDate: java.time.Instant,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("assessor", required = true) val assessor: ApprovedPremisesUser,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("isParole", required = true) val isParole: kotlin.Boolean,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("isWithdrawn", required = true) val isWithdrawn: kotlin.Boolean,

    @Schema(example = "null", description = "")
    @get:JsonProperty("notes") val notes: kotlin.String? = null,

    @Schema(example = "null", description = "")
    @get:JsonProperty("booking") val booking: BookingSummary? = null,

    @Schema(example = "null", description = "")
    @get:JsonProperty("requestType") val requestType: PlacementRequestRequestType? = null,

    @Schema(example = "null", description = "")
    @get:JsonProperty("withdrawalReason") val withdrawalReason: WithdrawPlacementRequestReason? = null
) {

}

