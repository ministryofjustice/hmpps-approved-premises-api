package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2v2.reporting.model

@Suppress("LongParameterList")
class ApplicationStatusUpdatesReportRow(
  val eventId: String,
  val applicationId: String,
  val personCrn: String,
  val personNoms: String,
  val newStatus: String,
  val updatedAt: String,
  val updatedBy: String,
  val statusDetails: String,
  val applicationOrigin: String,
)
