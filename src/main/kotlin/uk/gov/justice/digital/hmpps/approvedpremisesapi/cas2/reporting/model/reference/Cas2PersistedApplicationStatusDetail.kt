package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.reporting.model.reference

import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import java.util.UUID

data class Cas2PersistedApplicationStatusDetail(
  val id: UUID,
  val name: String,
  val label: String,
  val isActive: Boolean = true,
  val applicableToServices: List<ServiceName> = listOf(ServiceName.cas2, ServiceName.cas2v2),
)
