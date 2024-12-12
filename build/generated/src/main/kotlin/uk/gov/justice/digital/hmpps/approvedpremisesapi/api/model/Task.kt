package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonValue
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApArea
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApprovedPremisesUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PersonSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ProbationDeliveryUnit
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.TaskStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.TaskType
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 
 * @param taskType 
 * @param id 
 * @param applicationId 
 * @param personSummary 
 * @param personName Superseded by personSummary which provides 'name' as well as 'personType' and 'crn'.
 * @param crn 
 * @param dueDate The Due date of the task - this is deprecated in favour of the `dueAt` field
 * @param dueAt 
 * @param status 
 * @param allocatedToStaffMember 
 * @param apArea 
 * @param probationDeliveryUnit 
 * @param outcomeRecordedAt 
 */

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "taskType", visible = true)
@JsonSubTypes(
      JsonSubTypes.Type(value = AssessmentTask::class, name = "Assessment"),
      JsonSubTypes.Type(value = BookingAppealTask::class, name = "BookingAppeal"),
      JsonSubTypes.Type(value = PlacementApplicationTask::class, name = "PlacementApplication"),
      JsonSubTypes.Type(value = PlacementRequestTask::class, name = "PlacementRequest")
)

interface Task{
                @get:Schema(example = "null", requiredMode = Schema.RequiredMode.REQUIRED, description = "")
        val taskType: TaskType

                @get:Schema(example = "6abb5fa3-e93f-4445-887b-30d081688f44", requiredMode = Schema.RequiredMode.REQUIRED, description = "")
        val id: java.util.UUID

                @get:Schema(example = "6abb5fa3-e93f-4445-887b-30d081688f44", requiredMode = Schema.RequiredMode.REQUIRED, description = "")
        val applicationId: java.util.UUID

                @get:Schema(example = "null", requiredMode = Schema.RequiredMode.REQUIRED, description = "")
        val personSummary: PersonSummary

                @get:Schema(example = "null", requiredMode = Schema.RequiredMode.REQUIRED, description = "Superseded by personSummary which provides 'name' as well as 'personType' and 'crn'.")
        val personName: kotlin.String

                @get:Schema(example = "null", requiredMode = Schema.RequiredMode.REQUIRED, description = "")
        val crn: kotlin.String

                @get:Schema(example = "null", requiredMode = Schema.RequiredMode.REQUIRED, description = "The Due date of the task - this is deprecated in favour of the `dueAt` field")
        val dueDate: java.time.LocalDate

                @get:Schema(example = "null", requiredMode = Schema.RequiredMode.REQUIRED, description = "")
        val dueAt: java.time.Instant

                @get:Schema(example = "null", requiredMode = Schema.RequiredMode.REQUIRED, description = "")
        val status: TaskStatus

                @get:Schema(example = "null", description = "")
        val allocatedToStaffMember: ApprovedPremisesUser? 

                @get:Schema(example = "null", description = "")
        val apArea: ApArea? 

                @get:Schema(example = "null", description = "")
        val probationDeliveryUnit: ProbationDeliveryUnit? 

                @get:Schema(example = "null", description = "")
        val outcomeRecordedAt: java.time.Instant? 


}

