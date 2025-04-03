package uk.gov.justice.digital.hmpps.approvedpremisesapi.model.reference

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import java.util.UUID

@Component
class Cas2v2PersistedApplicationStatusFinder(
  private val statusList: List<Cas2PersistedApplicationStatus> = Cas2ApplicationStatusSeeding.statusList(listOf(ServiceName.cas2v2)),
) {
  fun all(): List<Cas2PersistedApplicationStatus> = statusList

  fun active(): List<Cas2PersistedApplicationStatus> = statusList.filter { it.isActive }

  fun getById(id: UUID): Cas2PersistedApplicationStatus = statusList.find { status -> status.id == id }
    ?: error("Status with id $id not found")
}
