package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2v2.service

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.dto.reference.Cas2HdcApplicationStatusSeeding
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.dto.reference.Cas2HdcPersistedApplicationStatus

@Component
class Cas2v2PersistedApplicationStatusFinder(
  private val statusList: List<Cas2HdcPersistedApplicationStatus> = Cas2HdcApplicationStatusSeeding.statusList(ServiceName.cas2v2),
) {
  fun active(): List<Cas2HdcPersistedApplicationStatus> = statusList.filter { it.isActive }
}
