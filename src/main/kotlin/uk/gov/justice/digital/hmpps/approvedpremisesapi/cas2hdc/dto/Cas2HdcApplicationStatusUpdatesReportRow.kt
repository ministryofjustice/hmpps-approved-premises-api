package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.dto

@Suppress("LongParameterList")
class Cas2HdcApplicationStatusUpdatesReportRow(
  val eventId: String,
  val applicationId: String,
  val personCrn: String,
  val personNoms: String,
  val newStatus: String,
  val updatedAt: String,
  val updatedBy: String,
  val statusDetails: String,
  val numberOfLocationTransfers: String,
  val numberOfPomTransfers: String,
)
