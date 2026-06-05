package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.dto.reference

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import java.util.UUID

@Component
class Cas2HdcPersistedApplicationStatusFinder(
  private val statusList: List<Cas2HdcPersistedApplicationStatus> = Cas2HdcApplicationStatusSeeding.statusList(ServiceName.cas2),
) {
  fun all(): List<Cas2HdcPersistedApplicationStatus> = statusList

  fun active(): List<Cas2HdcPersistedApplicationStatus> = statusList.filter { it.isActive }

  fun getById(id: UUID): Cas2HdcPersistedApplicationStatus = statusList.find { status -> status.id == id }
    ?: error("Status with id $id not found")
}
