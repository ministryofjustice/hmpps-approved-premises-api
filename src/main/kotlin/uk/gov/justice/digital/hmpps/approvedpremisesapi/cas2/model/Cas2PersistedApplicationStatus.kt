package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model

import java.util.UUID

data class Cas2PersistedApplicationStatus(
  val id: UUID,
  val status: Cas2AssessmentStatus,
  val label: String,
  val description: String,
  val statusDetails: List<Cas2PersistedApplicationStatusDetail>? = null,
  val isActive: Boolean = true,
) {
  val name: String
    get() = status.value

  fun findStatusDetailOnStatus(detailName: String) = statusDetails?.find { detail -> detail.name == detailName }
}
