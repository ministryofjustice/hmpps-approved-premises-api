package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2v2.service

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.reporting.model.reference.Cas2ApplicationStatusSeeding
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.reporting.model.reference.Cas2PersistedApplicationStatus

@Component
class Cas2v2PersistedApplicationStatusFinder(
  private val statusList: List<Cas2PersistedApplicationStatus> = Cas2ApplicationStatusSeeding.statusList(ServiceName.cas2v2),
) {
  fun active(): List<Cas2PersistedApplicationStatus> = statusList.filter { it.isActive }
}
