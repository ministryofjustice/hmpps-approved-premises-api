package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.reporting.properties

import java.time.LocalDate
import java.util.UUID

data class BedspaceOccupancyReportProperties(
  val probationRegionId: UUID?,
  val startDate: LocalDate,
  val endDate: LocalDate,
)
