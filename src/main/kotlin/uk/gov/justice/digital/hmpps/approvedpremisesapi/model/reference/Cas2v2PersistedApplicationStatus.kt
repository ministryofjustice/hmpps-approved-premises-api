package uk.gov.justice.digital.hmpps.approvedpremisesapi.model.reference

import java.util.UUID

data class Cas2v2PersistedApplicationStatus(
  val id: UUID,
  val name: String,
  val label: String,
  val description: String,
  val statusDetails: List<Cas2v2PersistedApplicationStatusDetail>? = emptyList(),
  val isActive: Boolean = true,
) {
  fun findStatusDetailOnStatus(detailName: String) =
    statusDetails?.find { detail -> detail.name == detailName }
}
