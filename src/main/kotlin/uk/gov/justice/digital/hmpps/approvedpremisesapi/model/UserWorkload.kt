package uk.gov.justice.digital.hmpps.approvedpremisesapi.model

data class UserWorkload(
  var numTasksPending: Int,
  var numTasksCompleted7Days: Int,
  var numTasksCompleted30Days: Int,
)
