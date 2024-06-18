package uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.properties

data class RequestsForPlacementReportProperties(
  val year: Int,
  val month: Int,
  val includePii: Boolean,
)
