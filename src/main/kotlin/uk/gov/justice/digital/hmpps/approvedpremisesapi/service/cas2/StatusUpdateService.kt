package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas2

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas2ApplicationStatusUpdate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.reference.Cas2ApplicationStatusSeeding

@Service("Cas2StatusUpdateService")
class StatusUpdateService {
  fun isValidStatus(statusUpdate: Cas2ApplicationStatusUpdate): Boolean {
    return Cas2ApplicationStatusSeeding.statusList()
      .map { status -> status.name }
      .contains(statusUpdate.newStatus)
  }
}
