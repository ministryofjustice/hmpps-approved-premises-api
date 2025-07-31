package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.controller

import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.Cas3BedspaceSearchParameters
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.Cas3BedspaceSearchResults
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.service.Cas3BedspaceSearchService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.transformer.Cas3BedspaceSearchResultsTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.extractEntityFromCasResult

@RestController
@RequestMapping(
  "\${api.base-path:}/cas3",
  produces = [MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_PROBLEM_JSON_VALUE],
)
class Cas3BedspaceSearchController(
  private val userService: UserService,
  private val cas3BedspaceSearchService: Cas3BedspaceSearchService,
  private val cas3BedspaceSearchResultsTransformer: Cas3BedspaceSearchResultsTransformer,
) {

  @PostMapping(
    "/bedspaces/search",
    consumes = [MediaType.APPLICATION_JSON_VALUE],
  )
  fun postBedspaceSearch(@RequestBody cas3BedspaceSearchParameters: Cas3BedspaceSearchParameters): ResponseEntity<Cas3BedspaceSearchResults> {
    val user = userService.getUserForRequest()

    val searchResult = cas3BedspaceSearchService.findBedspaces(
      user = user,
      cas3BedspaceSearchParameters,
    )

    return ResponseEntity.ok(cas3BedspaceSearchResultsTransformer.transformDomainToApi(extractEntityFromCasResult(searchResult)))
  }
}
