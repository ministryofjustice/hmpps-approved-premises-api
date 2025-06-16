package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2v2.reporting.model

import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApplicationOrigin

class UnsubmittedApplicationsReportRow(
  val applicationId: String,
  val personCrn: String,
  val personNoms: String?,
  val startedAt: String,
  val startedBy: String,
  val applicationOrigin: ApplicationOrigin,
)
