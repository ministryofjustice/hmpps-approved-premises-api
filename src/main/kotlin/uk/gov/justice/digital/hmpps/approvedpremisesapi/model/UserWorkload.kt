package uk.gov.justice.digital.hmpps.approvedpremisesapi.model

data class UserWorkload(
  var numAssessmentsPending: Int,
  var numAssessmentsCompleted7Days: Int,
  var numAssessmentsCompleted30Days: Int,
)
