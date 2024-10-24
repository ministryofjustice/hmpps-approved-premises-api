package uk.gov.justice.digital.hmpps.approvedpremisesapi.controller.cas1

import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.cas1.SpaceSearchesCas1Delegate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1SpaceSearchParameters
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1SpaceSearchResults
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1SpaceSearchService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas1.Cas1SpaceSearchResultsTransformer

@Service
class Cas1SpaceSearchController(
  private val spaceSearchService: Cas1SpaceSearchService,
  private val spaceSearchResultTransformer: Cas1SpaceSearchResultsTransformer,
) : SpaceSearchesCas1Delegate {
  override fun spacesSearchPost(cas1SpaceSearchParameters: Cas1SpaceSearchParameters): ResponseEntity<Cas1SpaceSearchResults> {
    val results = spaceSearchService.findSpaces(cas1SpaceSearchParameters)

    return ResponseEntity.ok(spaceSearchResultTransformer.transformDomainToApi(cas1SpaceSearchParameters, results))
  }
}
