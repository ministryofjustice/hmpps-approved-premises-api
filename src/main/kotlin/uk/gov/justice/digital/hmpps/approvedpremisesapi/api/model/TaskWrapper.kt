package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

data class TaskWrapper(

  @get:JsonProperty("task", required = true) val task: Task,

  @field:Schema(example = "null", required = true, description = "Users to whom this task can be allocated")
  @get:JsonProperty("users", required = true) val users: kotlin.collections.List<UserWithWorkload>,
)
