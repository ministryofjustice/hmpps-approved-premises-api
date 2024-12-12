package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonValue
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApArea
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApprovedPremisesUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PersonSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PlacementApplicationDecision
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PlacementDates
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PlacementType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ProbationDeliveryUnit
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ReleaseTypeOption
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.RiskTierEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Task
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.TaskStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.TaskType
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 
 * @param tier 
 * @param releaseType 
 * @param placementType 
 * @param placementDates 
 * @param outcome 
 */
data class PlacementApplicationTask(

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("tier", required = true) val tier: RiskTierEnvelope,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("releaseType", required = true) val releaseType: ReleaseTypeOption,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("placementType", required = true) val placementType: PlacementType,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("taskType", required = true) override val taskType: TaskType,

    @Schema(example = "6abb5fa3-e93f-4445-887b-30d081688f44", required = true, description = "")
    @get:JsonProperty("id", required = true) override val id: java.util.UUID,

    @Schema(example = "6abb5fa3-e93f-4445-887b-30d081688f44", required = true, description = "")
    @get:JsonProperty("applicationId", required = true) override val applicationId: java.util.UUID,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("personSummary", required = true) override val personSummary: PersonSummary,

    @Schema(example = "null", required = true, description = "Superseded by personSummary which provides 'name' as well as 'personType' and 'crn'.")
    @get:JsonProperty("personName", required = true) override val personName: kotlin.String,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("crn", required = true) override val crn: kotlin.String,

    @Schema(example = "null", required = true, description = "The Due date of the task - this is deprecated in favour of the `dueAt` field")
    @get:JsonProperty("dueDate", required = true) override val dueDate: java.time.LocalDate,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("dueAt", required = true) override val dueAt: java.time.Instant,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("status", required = true) override val status: TaskStatus,

    @Schema(example = "null", description = "")
    @get:JsonProperty("placementDates") val placementDates: kotlin.collections.List<PlacementDates>? = null,

    @Schema(example = "null", description = "")
    @get:JsonProperty("outcome") val outcome: PlacementApplicationDecision? = null,

    @Schema(example = "null", description = "")
    @get:JsonProperty("allocatedToStaffMember") override val allocatedToStaffMember: ApprovedPremisesUser? = null,

    @Schema(example = "null", description = "")
    @get:JsonProperty("apArea") override val apArea: ApArea? = null,

    @Schema(example = "null", description = "")
    @get:JsonProperty("probationDeliveryUnit") override val probationDeliveryUnit: ProbationDeliveryUnit? = null,

    @Schema(example = "null", description = "")
    @get:JsonProperty("outcomeRecordedAt") override val outcomeRecordedAt: java.time.Instant? = null
) : Task{

}

