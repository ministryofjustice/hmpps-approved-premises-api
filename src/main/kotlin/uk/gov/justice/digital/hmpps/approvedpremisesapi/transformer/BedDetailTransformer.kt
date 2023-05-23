package uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.BedDetail
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.CharacteristicPair
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CharacteristicEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainBedSummary

@Component
class BedDetailTransformer(
  private val bedSummaryTransformer: BedSummaryTransformer
) {
  fun transformToApi(summaryAndCharacteristics: Pair<DomainBedSummary, List<CharacteristicEntity>>): BedDetail {
    val summary = summaryAndCharacteristics.first
    val characteristics = summaryAndCharacteristics.second

    var bedSummary = bedSummaryTransformer.transformToApi(summary)
    var characteristicPairs = characteristics.map {
      CharacteristicPair(
        name = it.name,
        propertyName = it.propertyName,
      )
    }

    return BedDetail(
      id = bedSummary.id,
      name = bedSummary.name,
      roomName = bedSummary.roomName,
      status = bedSummary.status,
      characteristics = characteristicPairs,
    )
  }
}
