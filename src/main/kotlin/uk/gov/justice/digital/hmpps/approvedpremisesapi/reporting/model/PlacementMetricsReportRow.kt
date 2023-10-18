package uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.model

data class PlacementMetricsReportRow(
  val tier: TierCategory,
  val uniqueRequests: Int,
  val successfulRequests: Int,
  val percentageOfSuccessfulPlacements: String,
  val averageTimeliness: String,
  val averagePlacementMatchingTimeliness: String,
  val timelinessSameDayAdmission: Int,
  val timeliness1To2WorkingDays: Int,
  val timeliness3To5WorkingDays: Int,
  val timeliness8To15CalendarDays: Int,
  val timeliness16To30CalendarDays: Int,
  val timeliness31To60CalendarDays: Int,
  val timeliness61To90CalendarDays: Int,
  val timeliness91To120CalendarDays: Int,
  val timeliness121To150CalendarDays: Int,
  val timeliness151To180CalendarDays: Int,
  val timeliness181To275CalendarDays: Int,
  val timeliness276To365CalendarDays: Int,
  val timeliness366PlusCalendarDays: Int,
)
