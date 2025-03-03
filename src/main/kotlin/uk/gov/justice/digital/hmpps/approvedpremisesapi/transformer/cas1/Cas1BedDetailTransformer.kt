package uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas1

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1BedDetail
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1SpaceCharacteristic
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CharacteristicEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainBedSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.BedSummaryTransformer

@Component
class Cas1BedDetailTransformer(
  private val bedSummaryTransformer: BedSummaryTransformer,
) {
  fun transformToApi(summaryAndCharacteristics: Pair<DomainBedSummary, List<CharacteristicEntity>>): Cas1BedDetail {
    val summary = summaryAndCharacteristics.first
    val characteristics = summaryAndCharacteristics.second

    val bedSummary = bedSummaryTransformer.transformToApi(summary)
    val spaceCharacteristics = characteristics.map {
      Cas1SpaceCharacteristic.valueOf(it.propertyName!!)
    }

    return Cas1BedDetail(
      id = bedSummary.id,
      name = bedSummary.name,
      roomName = bedSummary.roomName,
      status = bedSummary.status,
      characteristics = spaceCharacteristics,
    )
  }
}
