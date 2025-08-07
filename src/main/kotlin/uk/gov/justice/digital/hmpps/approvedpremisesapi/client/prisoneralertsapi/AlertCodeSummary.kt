package uk.gov.justice.digital.hmpps.approvedpremisesapi.client.prisoneralertsapi

data class AlertCodeSummary(
  val alertTypeCode: String,
  val alertTypeDescription: String,
  val code: String,
  val description: String,
)
