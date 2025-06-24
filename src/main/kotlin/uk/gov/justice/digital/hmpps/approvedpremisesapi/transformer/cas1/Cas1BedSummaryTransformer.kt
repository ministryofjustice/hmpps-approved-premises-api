package uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas1

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1PremisesBedSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1SpaceCharacteristic
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1PlanningBedSummary

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
}
