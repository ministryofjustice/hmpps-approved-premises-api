package uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.model

import java.time.LocalDate

data class ApplicationTimelinessDto(
  val id: String,
  val tier: String?,
  val applicationSubmittedAt: LocalDate?,
  val bookingMadeAt: LocalDate?,
  val overallTimeliness: Int?,
  val placementMatchingTimeliness: Int?,
  var overallTimelinessInWorkingDays: Int?,
)
