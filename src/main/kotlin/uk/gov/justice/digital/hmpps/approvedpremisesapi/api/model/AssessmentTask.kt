package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 *
 * @param createdFromAppeal
 * @param outcome
 */
data class AssessmentTask(

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("createdFromAppeal", required = true) val createdFromAppeal: kotlin.Boolean,

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
  @get:JsonProperty("outcome") val outcome: AssessmentDecision? = null,

  @Schema(example = "null", description = "")
  @get:JsonProperty("allocatedToStaffMember") override val allocatedToStaffMember: ApprovedPremisesUser? = null,

  @Schema(example = "null", description = "")
  @get:JsonProperty("apArea") override val apArea: ApArea? = null,

  @Schema(example = "null", description = "")
  @get:JsonProperty("probationDeliveryUnit") override val probationDeliveryUnit: ProbationDeliveryUnit? = null,

  @Schema(example = "null", description = "")
  @get:JsonProperty("outcomeRecordedAt") override val outcomeRecordedAt: java.time.Instant? = null,
) : Task
