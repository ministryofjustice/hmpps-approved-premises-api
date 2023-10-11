package uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.properties

import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName

data class DailyMetricReportProperties(
  val serviceName: ServiceName,
  val year: Int,
  val month: Int,
)
