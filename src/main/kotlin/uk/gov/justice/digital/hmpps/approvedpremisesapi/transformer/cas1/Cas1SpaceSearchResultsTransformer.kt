package uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas1

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1PremisesSearchResultSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1SpaceSearchParameters
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1SpaceSearchResults
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NamedId
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.asApiType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1SpaceSearchResult as ApiSpaceSearchResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1SpaceSearchResult as DomainSpaceSearchResult

@Component
class Cas1SpaceSearchResultsTransformer {
  fun transformDomainToApi(searchParameters: Cas1SpaceSearchParameters, results: List<DomainSpaceSearchResult>) =
    Cas1SpaceSearchResults(
      resultsCount = results.size,
      searchCriteria = searchParameters,
      results = results.map {
        ApiSpaceSearchResult(
          premises = Cas1PremisesSearchResultSummary(
            id = it.candidatePremises.premisesId,
            apCode = it.candidatePremises.apCode,
            deliusQCode = it.candidatePremises.deliusQCode,
            apType = it.candidatePremises.apType.asApiType(),
            name = it.candidatePremises.name,
            addressLine1 = it.candidatePremises.addressLine1,
            addressLine2 = it.candidatePremises.addressLine2,
            town = it.candidatePremises.town,
            postcode = it.candidatePremises.postcode,
            apArea = NamedId(
              id = it.candidatePremises.apAreaId,
              name = it.candidatePremises.apAreaName,
            ),
            totalSpaceCount = it.candidatePremises.totalSpaceCount,
            premisesCharacteristics = listOf(),
          ),
          distanceInMiles = it.candidatePremises.distanceInMiles.toBigDecimal(),
          spacesAvailable = listOf(),
        )
      },
    )
}
