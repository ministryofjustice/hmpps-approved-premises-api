package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty

/**
 *
 * @param user
 * @param taskType
 */
data class Reallocation(

  val user: ApprovedPremisesUser,

  val taskType: TaskType,
)
