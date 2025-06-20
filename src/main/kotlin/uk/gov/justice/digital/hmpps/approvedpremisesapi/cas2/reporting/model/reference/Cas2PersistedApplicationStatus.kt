package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.reporting.model.reference

import java.util.UUID

data class Cas2PersistedApplicationStatus(
  val id: UUID,
  val name: String,
  val label: String,
  val description: String,
  val statusDetails: List<Cas2PersistedApplicationStatusDetail>? = null,
  val isActive: Boolean = true,
) {
  fun findStatusDetailOnStatus(detailName: String) = statusDetails?.find { detail -> detail.name == detailName }
}
