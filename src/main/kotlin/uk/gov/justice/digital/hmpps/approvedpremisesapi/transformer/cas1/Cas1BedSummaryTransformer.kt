package uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas1

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.BedStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.BedSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto.Cas1PremisesBedSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto.Cas1SpaceCharacteristic
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas1.Cas1DomainBedSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas1.Cas1PlanningBedSummary

@Component
class Cas1BedSummaryTransformer {
  fun transformJpaToApi(summary: Cas1PlanningBedSummary) = Cas1PremisesBedSummary(
    id = summary.bedId,
    roomName = summary.roomName,
    bedName = summary.bedName,
    characteristics = summary.characteristicsPropertyNames.map {
      Cas1SpaceCharacteristic.valueOf(it)
    },
  )

  fun transformToApi(summary: Cas1DomainBedSummary) = BedSummary(
    id = summary.id,
    name = summary.name,
    roomName = summary.roomName,
    status = getStatus(summary),
  )

  private fun getStatus(summary: Cas1DomainBedSummary): BedStatus {
    if (summary.bedBooked) {
      return BedStatus.occupied
    } else if (summary.bedOutOfService) {
      return BedStatus.outOfService
    } else {
      return BedStatus.available
    }
  }
}
