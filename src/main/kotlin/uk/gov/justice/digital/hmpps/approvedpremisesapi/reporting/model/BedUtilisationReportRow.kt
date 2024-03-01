package uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.model

data class BedUtilisationReportRow(
  val probationRegion: String?,
  val pdu: String?,
  val localAuthority: String?,
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
  val uniquePropertyRef: String,
  val uniqueBedspaceRef: String,
)
