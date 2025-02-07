package uk.gov.justice.digital.hmpps.approvedpremisesapi.controller
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.BedSearchResults
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.TemporaryAccommodationBedSearchParameters
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.BedsApiDelegate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas3.Cas3BedspaceSearchService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.BedSearchResultTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.extractEntityFromCasResult

@Service
class BedSearchController(
  private val userService: UserService,
  private val cas3BedspaceSearchService: Cas3BedspaceSearchService,
  private val bedSearchResultTransformer: BedSearchResultTransformer,
) : BedsApiDelegate {
  override fun bedsSearchPost(temporaryAccommodationBedSearchParameters: TemporaryAccommodationBedSearchParameters): ResponseEntity<BedSearchResults> {
    val user = userService.getUserForRequest()

    val searchResult = cas3BedspaceSearchService.findBedspaces(
      user = user,
      temporaryAccommodationBedSearchParameters,
    )

    return ResponseEntity.ok(
      bedSearchResultTransformer.transformDomainToApi(extractEntityFromCasResult(searchResult)),
    )
  }
}
