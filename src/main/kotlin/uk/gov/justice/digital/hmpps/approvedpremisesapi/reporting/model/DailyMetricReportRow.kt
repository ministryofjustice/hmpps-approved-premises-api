package uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.model

import java.time.LocalDate

// Date	Applications Started	Unique Users starting applications	Applications Submitted	Unique Users submitting applications	Assessments completed	Unique Users completing assessments	Bookings made	Unique Users making bookings

data class DailyMetricReportRow(
  val date: LocalDate,
  val applicationsStarted: Int,
  val uniqueUsersStartingApplications: Int,
  val applicationsSubmitted: Int,
  val uniqueUsersSubmittingApplications: Int,
  val assessmentsCompleted: Int,
  val uniqueUsersCompletingAssessments: Int,
  val bookingsMade: Int,
  val uniqueUsersMakingBookings: Int,
)
