package uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas1

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto.Cas1BedDetail
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto.Cas1SpaceCharacteristic
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CharacteristicEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas1.Cas1DomainBedSummary

@Component
class Cas1BedDetailTransformer(
  private val cas1BedSummaryTransformer: Cas1BedSummaryTransformer,
) {
  fun transformToApi(summaryAndCharacteristics: Pair<Cas1DomainBedSummary, List<CharacteristicEntity>>): Cas1BedDetail {
    val summary = summaryAndCharacteristics.first
    val characteristics = summaryAndCharacteristics.second

    val bedSummary = cas1BedSummaryTransformer.transformToApi(summary)
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
