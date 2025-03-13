package uk.gov.justice.digital.hmpps.approvedpremisesapi.controller.cas3

import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.cas3.BedspacesCas3Delegate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas3BedspaceSearchParameters
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas3BedspaceSearchResults
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas3.Cas3BedspaceSearchService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas3.Cas3BedspaceSearchResultsTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.extractEntityFromCasResult

@Service
class Cas3BedspaceSearchController(
  private val userService: UserService,
  private val cas3BedspaceSearchService: Cas3BedspaceSearchService,
  private val cas3BedspaceSearchResultsTransformer: Cas3BedspaceSearchResultsTransformer,
) : BedspacesCas3Delegate {
  override fun postBedspaceSearch(cas3BedspaceSearchParameters: Cas3BedspaceSearchParameters): ResponseEntity<Cas3BedspaceSearchResults> {
    val user = userService.getUserForRequest()

    val searchResult = cas3BedspaceSearchService.findBedspaces(
      user = user,
      cas3BedspaceSearchParameters,
    )

    return ResponseEntity.ok(cas3BedspaceSearchResultsTransformer.transformDomainToApi(extractEntityFromCasResult(searchResult)))
  }
}
