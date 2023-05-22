package uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.BedStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.BedSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainBedSummary

@Component
class BedSummaryTransformer {
  fun transformToApi(summary: DomainBedSummary) = BedSummary(
    id = summary.id,
    name = summary.name,
    roomName = summary.roomName,
    status = getStatus(summary),
  )

  private fun getStatus(summary: DomainBedSummary): BedStatus {
    if (summary.bedBooked) {
      return BedStatus.occupied
    } else if (summary.bedOutOfService) {
      return BedStatus.outOfService
    } else {
      return BedStatus.available
    }
  }
}
