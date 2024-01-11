package uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.model.cas2

class UnsubmittedApplicationsReportRow(
  val applicationId: String,
  val personCrn: String,
  val personNoms: String,
  val startedAt: String,
  val startedBy: String,
)
