package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.reporting.properties

import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import java.time.LocalDate
import java.util.UUID

data class BedUsageReportProperties(
  val serviceName: ServiceName,
  val probationRegionId: UUID?,
  val startDate: LocalDate,
  val endDate: LocalDate,
)
