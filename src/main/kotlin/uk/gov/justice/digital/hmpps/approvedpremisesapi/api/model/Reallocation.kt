package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

data class Reallocation(

  val user: ApprovedPremisesUser,

  val taskType: TaskType,
)
