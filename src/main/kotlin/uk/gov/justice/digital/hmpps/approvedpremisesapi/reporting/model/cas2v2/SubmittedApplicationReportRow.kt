package uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.model.cas2v2

import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApplicationOrigin

data class SubmittedApplicationReportRow(
  val eventId: String,
  val applicationId: String,
  val applicationOrigin: ApplicationOrigin,
  val personCrn: String,
  val personNoms: String,
  val referringPrisonCode: String,
  val preferredAreas: String?,
  val hdcEligibilityDate: String?,
  val conditionalReleaseDate: String?,
  val submittedAt: String,
  val submittedBy: String,
  val startedAt: String,
)
