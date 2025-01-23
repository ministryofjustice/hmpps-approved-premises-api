package uk.gov.justice.digital.hmpps.approvedpremisesapi.model.prisoneralertsapi

data class AlertCodeSummary(
  val alertTypeCode: String,
  val alertTypeDescription: String,
  val code: String,
  val description: String,
)
