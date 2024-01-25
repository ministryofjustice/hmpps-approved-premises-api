package uk.gov.justice.digital.hmpps.approvedpremisesapi.model.reference

class Cas2PersistedApplicationStatusFinder(
  private val statusList: List<Cas2PersistedApplicationStatus> = Cas2ApplicationStatusSeeding.statusList(),
) {
  fun all(): List<Cas2PersistedApplicationStatus> {
    return statusList
  }

  fun active(): List<Cas2PersistedApplicationStatus> {
    return statusList.filter { it.isActive }
  }
}
