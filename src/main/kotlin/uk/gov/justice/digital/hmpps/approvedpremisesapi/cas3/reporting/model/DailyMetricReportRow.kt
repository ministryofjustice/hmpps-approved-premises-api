package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.reporting.model

import java.time.LocalDate

@SuppressWarnings("ConstructorParameterNaming")
@Suppress("MaxLineLength")
data class DailyMetricReportRow(
  val report_date: LocalDate,
  val applications_started: Int,
  val unique_users_starting_applications: Int,
  val applications_submitted: Int,
  val unique_users_submitting_applications: Int,
  val assessments_completed: Int,
  val unique_users_completing_assessments: Int,
  val bookings_made: Int,
  val unique_users_making_bookings: Int,
)
