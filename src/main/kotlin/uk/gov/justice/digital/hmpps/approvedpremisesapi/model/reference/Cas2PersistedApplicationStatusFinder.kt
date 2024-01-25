package uk.gov.justice.digital.hmpps.approvedpremisesapi.model.reference

import java.util.UUID

class Cas2PersistedApplicationStatusFinder(
  private val statusList: List<Cas2PersistedApplicationStatus> = Cas2ApplicationStatusSeeding.statusList(),
) {
  fun all(): List<Cas2PersistedApplicationStatus> {
    return statusList
  }

  fun active(): List<Cas2PersistedApplicationStatus> {
    return statusList.filter { it.isActive }
  }

  fun getById(id: UUID): Cas2PersistedApplicationStatus {
    return statusList.find { status -> status.id == id }
      ?: error("Status with id $id not found")
  }
}
