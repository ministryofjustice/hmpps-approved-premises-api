package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

data class AssessmentTask(

  @get:JsonProperty("createdFromAppeal", required = true) val createdFromAppeal: kotlin.Boolean,

  @get:JsonProperty("taskType", required = true) override val taskType: TaskType,

  @field:Schema(example = "6abb5fa3-e93f-4445-887b-30d081688f44", required = true, description = "")
  @get:JsonProperty("id", required = true) override val id: java.util.UUID,

  @field:Schema(example = "6abb5fa3-e93f-4445-887b-30d081688f44", required = true, description = "")
  @get:JsonProperty("applicationId", required = true) override val applicationId: java.util.UUID,

  @get:JsonProperty("personSummary", required = true) override val personSummary: PersonSummary,

  @field:Schema(example = "null", required = true, description = "Superseded by personSummary which provides 'name' as well as 'personType' and 'crn'.")
  @get:JsonProperty("personName", required = true) override val personName: kotlin.String,

  @get:JsonProperty("crn", required = true) override val crn: kotlin.String,

  @field:Schema(example = "null", required = true, description = "The Due date of the task - this is deprecated in favour of the `dueAt` field")
  @get:JsonProperty("dueDate", required = true) override val dueDate: java.time.LocalDate,

  @get:JsonProperty("dueAt", required = true) override val dueAt: java.time.Instant,

  @get:JsonProperty("status", required = true) override val status: TaskStatus,

  @get:JsonProperty("apType", required = true) override val apType: ApType,

  @get:JsonProperty("outcome") val outcome: AssessmentDecision? = null,

  @get:JsonProperty("expectedArrivalDate") override val expectedArrivalDate: java.time.LocalDate? = null,

  @get:JsonProperty("allocatedToStaffMember") override val allocatedToStaffMember: ApprovedPremisesUser? = null,

  @get:JsonProperty("apArea") override val apArea: ApArea? = null,

  @get:JsonProperty("probationDeliveryUnit") override val probationDeliveryUnit: ProbationDeliveryUnit? = null,

  @get:JsonProperty("outcomeRecordedAt") override val outcomeRecordedAt: java.time.Instant? = null,
) : Task
