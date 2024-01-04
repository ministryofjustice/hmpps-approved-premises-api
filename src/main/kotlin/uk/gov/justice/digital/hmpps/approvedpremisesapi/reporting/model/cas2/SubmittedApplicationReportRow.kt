package uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.model.cas2

data class SubmittedApplicationReportRow(
  val eventId: String,
  val applicationId: String,
  val personCrn: String,
  val personNoms: String,
  val referringPrisonCode: String,
  val submittedAt: String,
  val submittedBy: String,
)
