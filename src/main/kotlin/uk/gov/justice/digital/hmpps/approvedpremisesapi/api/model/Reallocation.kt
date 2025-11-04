package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

/**
 *
 * @param user
 * @param taskType
 */
data class Reallocation(

  val user: ApprovedPremisesUser,

  val taskType: TaskType,
)
