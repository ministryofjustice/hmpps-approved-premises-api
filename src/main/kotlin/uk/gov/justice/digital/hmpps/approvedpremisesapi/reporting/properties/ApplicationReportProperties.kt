package uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.properties

import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName

data class ApplicationReportProperties(
  val serviceName: ServiceName,
  val year: Int,
  val month: Int,
  val deliusUsername: String,
  val includePii: Boolean = false,
)
