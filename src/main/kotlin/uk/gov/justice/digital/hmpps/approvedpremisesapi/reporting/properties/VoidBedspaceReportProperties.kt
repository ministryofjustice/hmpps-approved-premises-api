package uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.properties

import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import java.util.UUID

data class VoidBedspaceReportProperties(
  val serviceName: ServiceName,
  val probationRegionId: UUID?,
  val year: Int,
  val month: Int,
)
