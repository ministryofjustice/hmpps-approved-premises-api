package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model

import java.util.UUID

data class Cas2PersistedApplicationStatus(
  val status: Cas2AssessmentStatus,
  val statusDetails: List<Cas2AssessmentStatusDetail>? = null,
) {
  val name: String
    get() = status.lowerCaseName
  val id: UUID
    get() = status.id
  val description: String
    get() = status.description
  val label: String
    get() = status.label

  fun findStatusDetailOnStatus(detailName: String) = statusDetails?.find { detail -> detail.lowerCaseName == detailName }
}
