package uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.model.cas2v2

import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApplicationOrigin

class UnsubmittedApplicationsReportRow(
  val applicationId: String,
  val applicationOrigin: ApplicationOrigin,
  val personCrn: String,
  val personNoms: String,
  val startedAt: String,
  val startedBy: String,
)
