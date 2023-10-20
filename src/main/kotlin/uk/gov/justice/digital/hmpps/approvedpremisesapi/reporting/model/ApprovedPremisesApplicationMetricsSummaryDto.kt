package uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.model

import java.time.LocalDate

data class ApprovedPremisesApplicationMetricsSummaryDto(
  val createdAt: LocalDate,
  val createdByUserId: String,
)
