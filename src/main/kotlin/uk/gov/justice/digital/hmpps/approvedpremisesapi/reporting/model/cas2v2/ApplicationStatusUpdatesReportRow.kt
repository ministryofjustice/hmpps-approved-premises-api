package uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.model.cas2v2

import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApplicationOrigin

@Suppress("LongParameterList")
class ApplicationStatusUpdatesReportRow(
  val eventId: String,
  val applicationId: String,
  val applicationOrigin: ApplicationOrigin,
  val personCrn: String,
  val personNoms: String,
  val newStatus: String,
  val updatedAt: String,
  val updatedBy: String,
  val statusDetails: String,
)
