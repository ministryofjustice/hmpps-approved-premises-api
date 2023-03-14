package uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.BedSearchResult as ApiBedSearchResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BedSearchResult as DomainBedSearchResult

@Component
class BedSearchTransformer {
  fun domainToApi(domain: List<DomainBedSearchResult>): List<ApiBedSearchResult> {
    return domain.map {
      ApiBedSearchResult(
        premisesId = it.premisesId,
        premisesName = it.premisesName,
        premisesCharacteristicPropertyNames = it.premisesCharacteristicPropertyNames,
        bedId = it.bedId,
        bedName = it.bedName,
        roomCharacteristicPropertyNames = it.roomCharacteristicPropertyNames,
        distanceMiles = it.distance.toBigDecimal()
      )
    }
  }
}
