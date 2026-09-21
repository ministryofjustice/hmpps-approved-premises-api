package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.service

import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model.Cas2ApplicationStatusSeeding
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model.Cas2PersistedApplicationStatus
import java.util.UUID

object Cas2PersistedApplicationStatusFinder {

  fun active(): List<Cas2PersistedApplicationStatus> = Cas2ApplicationStatusSeeding.statusList(ServiceName.cas2v2).filter { it.isActive }
  fun forName(name: String) = Cas2ApplicationStatusSeeding.statusList(ServiceName.cas2v2).firstOrNull { it.name == name }
  fun forId(id: UUID) = Cas2ApplicationStatusSeeding.statusList(ServiceName.cas2v2).firstOrNull { it.id == id }
  fun forDetailId(id: UUID, detailId: UUID) = Cas2ApplicationStatusSeeding.statusList(ServiceName.cas2v2)
    .firstOrNull { it.id == id }?.statusDetails?.firstOrNull { it.id == detailId }
}
