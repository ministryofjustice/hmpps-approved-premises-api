package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 *
 * @param task
 * @param users Users to whom this task can be allocated
 */
data class TaskWrapper(

  val task: Task,

  @Schema(example = "null", required = true, description = "Users to whom this task can be allocated")
  val users: kotlin.collections.List<UserWithWorkload>,
)
