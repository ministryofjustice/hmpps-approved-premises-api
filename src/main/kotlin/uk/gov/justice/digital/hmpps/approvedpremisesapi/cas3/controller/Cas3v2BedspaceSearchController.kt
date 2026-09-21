package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.controller

import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.Cas3v2BedspaceSearchResults
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.Cas3BedspaceSearchParameters
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.service.Cas3BedspaceSearchService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.transformer.Cas3v2BedspaceSearchResultsTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.extractEntityFromCasResult

@Cas3v2Controller
class Cas3v2BedspaceSearchController(
  private val userService: UserService,
  private val cas3v2BedspaceSearchService: Cas3BedspaceSearchService,
  private val cas3v2BedspaceSearchResultsTransformer: Cas3v2BedspaceSearchResultsTransformer,
) {

  @PostMapping(
    "/bedspaces/search",
    consumes = [MediaType.APPLICATION_JSON_VALUE],
  )
  fun postBedspaceSearch(@RequestBody cas3BedspaceSearchParameters: Cas3BedspaceSearchParameters): ResponseEntity<Cas3v2BedspaceSearchResults> {
    val user = userService.getUserForRequest()

    val searchResult = cas3v2BedspaceSearchService.searchBedspaces(
      user = user,
      cas3BedspaceSearchParameters,
    )

    return ResponseEntity.ok(cas3v2BedspaceSearchResultsTransformer.transformDomainToApi(extractEntityFromCasResult(searchResult)))
  }
}
