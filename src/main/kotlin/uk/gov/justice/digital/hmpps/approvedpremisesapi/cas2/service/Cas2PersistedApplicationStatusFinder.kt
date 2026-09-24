package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.service

import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model.Cas2ApplicationStatusSeeding.statusList
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model.Cas2PersistedApplicationStatus
import java.util.UUID

object Cas2PersistedApplicationStatusFinder {

  fun active(serviceName: ServiceName = ServiceName.cas2v2): List<Cas2PersistedApplicationStatus> = statusList(serviceName).filter { it.isActive }
  fun forName(name: String, serviceName: ServiceName = ServiceName.cas2v2) = statusList(serviceName).firstOrNull { it.name == name }
  fun forDetailId(id: UUID, detailId: UUID, serviceName: ServiceName = ServiceName.cas2v2) = statusList(serviceName)
    .firstOrNull { it.id == id }?.statusDetails?.firstOrNull { it.id == detailId }
  fun getById(id: UUID, serviceName: ServiceName = ServiceName.cas2v2): Cas2PersistedApplicationStatus = statusList(serviceName).find { status -> status.id == id }
    ?: error("Status with id $id not found")
}
