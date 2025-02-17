package uk.gov.justice.digital.hmpps.approvedpremisesapi.model.reference

import org.springframework.stereotype.Component
import java.util.Stack
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

  fun findDetailsBy(statusId: UUID, fn: (input: Cas2v2PersistedApplicationStatusDetail) -> Boolean): Cas2v2PersistedApplicationStatusDetail? {
    val details = getById(statusId).statusDetails?.flatten() ?: emptyList()
    return details.find { fn(it) }
  }

  // Given a status, looks up the detail matched by `fn` in the tree of details,
  // and returns the path from the root to the matching detail.
  fun findPathForMatchingDetail(statusId: UUID, fn: (input: Cas2v2PersistedApplicationStatusDetail) -> Boolean): String? {
    val details = getById(statusId).statusDetails ?: return null

    val paths = details.firstNotNullOfOrNull { detail ->
      val path = Stack<Cas2v2PersistedApplicationStatusDetail>()
      if (findDetailsWithPathRecursive(detail, path, fn)) path else null
    } ?: return null

    return paths.joinToString(separator = " - ") { (it as Cas2v2PersistedApplicationStatusDetail).label }
  }

  // From the starting detail, searches through the tree of details until it finds a detail that returns
  // true from `matchesTarget`. If the function returns true, then the `path` parameter will be a stack
  // of details from the root (first element) to the target (last element).
  private fun findDetailsWithPathRecursive(
    detail: Cas2v2PersistedApplicationStatusDetail,
    path: Stack<Cas2v2PersistedApplicationStatusDetail>,
    matchesTarget: (input: Cas2v2PersistedApplicationStatusDetail) -> Boolean,
  ): Boolean {
    path.push(detail)

    if (matchesTarget(detail)) {
      return true
    }

    for (child in detail.children!!) {
      if (findDetailsWithPathRecursive(child, path, matchesTarget)) return true
    }

    path.pop()
    return false
  }
}
