package uk.gov.justice.digital.hmpps.approvedpremisesapi.controller.cas3

import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.cas3.BedspacesCas3Delegate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas3BedspaceSearchResults
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.TemporaryAccommodationBedSearchParameters
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas3.Cas3BedspaceSearchService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas3.Cas3BedspaceSearchResultTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.extractEntityFromCasResult

@Service
class Cas3BedspaceSearchController(
  private val userService: UserService,
  private val cas3BedspaceSearchService: Cas3BedspaceSearchService,
  private val cas3BedspaceSearchResultTransformer: Cas3BedspaceSearchResultTransformer,
) : BedspacesCas3Delegate {
  override fun bedspaceSearchPost(temporaryAccommodationBedSearchParameters: TemporaryAccommodationBedSearchParameters): ResponseEntity<Cas3BedspaceSearchResults> {
    val user = userService.getUserForRequest()

    val searchResult = cas3BedspaceSearchService.findBedspaces(
      user = user,
      temporaryAccommodationBedSearchParameters,
    )

    return ResponseEntity.ok(
      cas3BedspaceSearchResultTransformer.transformDomainToCas3BedspaceSearchResults(extractEntityFromCasResult(searchResult)),
    )
  }
}
