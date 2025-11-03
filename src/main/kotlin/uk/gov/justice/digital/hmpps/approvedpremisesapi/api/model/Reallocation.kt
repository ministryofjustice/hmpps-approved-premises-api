package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty

/**
 *
 * @param user
 * @param taskType
 */
data class Reallocation(

  @get:JsonProperty("user", required = true) val user: ApprovedPremisesUser,

  @get:JsonProperty("taskType", required = true) val taskType: TaskType,
)
