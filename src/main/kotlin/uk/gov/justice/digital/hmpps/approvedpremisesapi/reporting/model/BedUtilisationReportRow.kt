package uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.model

data class BedUtilisationReportRow(
  val pdu: String?,
  val propertyRef: String,
  val addressLine1: String,
  val bedspaceRef: String,
  val bookedDaysActiveAndClosed: Int,
  val confirmedDays: Int,
  val provisionalDays: Int,
  val scheduledTurnaroundDays: Int,
  val effectiveTurnaroundDays: Int,
  val voidDays: Int,
  val totalBookedDays: Int,
  val totalDaysInTheMonth: Int,
  val occupancyRate: Double,
)
