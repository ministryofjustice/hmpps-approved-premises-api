package uk.gov.justice.digital.hmpps.approvedpremisesapi.controller
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.BedsApiDelegate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.BedSearchParameters
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.BedSearchResults
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.TemporaryAccommodationBedSearchParameters
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
  override fun bedsSearchPost(bedSearchParameters: BedSearchParameters): ResponseEntity<BedSearchResults> {
    val user = userService.getUserForRequest()

    val searchResult = when (bedSearchParameters) {
      is TemporaryAccommodationBedSearchParameters -> cas3BedspaceSearchService.findBedspaces(
        user = user,
        probationDeliveryUnits = bedSearchParameters.probationDeliveryUnits,
        startDate = bedSearchParameters.startDate,
        durationInDays = bedSearchParameters.durationDays,
        propertyBedAttributes = bedSearchParameters.attributes,
      )
      else -> throw RuntimeException("Unsupported BedSearchParameters type: ${bedSearchParameters::class.qualifiedName}")
    }

    return ResponseEntity.ok(
      bedSearchResultTransformer.transformDomainToApi(extractEntityFromCasResult(searchResult)),
    )
  }
}
