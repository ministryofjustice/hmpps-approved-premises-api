package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Task
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.UserWithWorkload

/**
 *
 * @param task
 * @param users Users to whom this task can be allocated
 */
data class TaskWrapper(

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("task", required = true) val task: Task,

  @Schema(example = "null", required = true, description = "Users to whom this task can be allocated")
  @get:JsonProperty("users", required = true) val users: kotlin.collections.List<UserWithWorkload>,
)
