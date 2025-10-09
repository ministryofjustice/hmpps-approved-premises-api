package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.reporting.model

data class Cas3BookingGapReportRow(
  val probationRegion: String?,
  val pduName: String?,
  val premisesName: String?,
  val bedName: String?,
  val gap: String?,
  val gapDays: Long?,
  val turnaroundDays: Int?,
)
