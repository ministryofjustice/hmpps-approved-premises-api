package uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.model.cas2

data class SubmittedApplicationReportRow(
  val eventId: String,
  val applicationId: String,
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
