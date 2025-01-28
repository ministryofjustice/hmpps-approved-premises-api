package uk.gov.justice.digital.hmpps.approvedpremisesapi.model.reference

import org.springframework.stereotype.Component
import java.util.UUID

@Component
class Cas2v2PersistedApplicationStatusFinder(
  private val statusList: List<Cas2v2PersistedApplicationStatus> = Cas2v2ApplicationStatusSeeding.statusList(),
) {
  fun all(): List<Cas2v2PersistedApplicationStatus> {
    return statusList
  }

  fun active(): List<Cas2v2PersistedApplicationStatus> {
    return statusList.filter { it.isActive }
  }

  fun getById(id: UUID): Cas2v2PersistedApplicationStatus {
    return statusList.find { status -> status.id == id }
      ?: error("Status with id $id not found")
  }

  fun findStatusByName(name: String): Cas2v2PersistedApplicationStatus? {
    return statusList.first { status -> status.name == name }
  }

  fun findDetailsByName(name: String): Cas2v2PersistedApplicationStatusDetail? {
    val details = statusList.mapNotNull { it.statusDetails }.map { it.flatten() }.flatten()
    return details.find { detail -> detail.name == name }
  }
}

fun List<Cas2v2PersistedApplicationStatusDetail>.flatten(): List<Cas2v2PersistedApplicationStatusDetail> {
  return this + map { it.children?.flatten() ?: emptyList() }.flatten()
}
