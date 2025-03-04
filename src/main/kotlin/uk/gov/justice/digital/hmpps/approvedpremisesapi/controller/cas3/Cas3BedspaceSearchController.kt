package uk.gov.justice.digital.hmpps.approvedpremisesapi.controller.cas3

import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.cas3.BedspacesCas3Delegate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.BedSearchAttributes
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.BedspaceSearchAttributes
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas3BedspaceSearchParameters
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas3BedspaceSearchResults
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
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
  override fun postBedspaceSearch(cas3BedspaceSearchParameters: Cas3BedspaceSearchParameters): ResponseEntity<Cas3BedspaceSearchResults> {
    val user = userService.getUserForRequest()
    val temporaryAccommodationBedSearchParameters =
      transformToTemporaryAccommodationBedSearchParameters(cas3BedspaceSearchParameters)
    val searchResult = cas3BedspaceSearchService.findBedspaces(
      user = user,
      temporaryAccommodationBedSearchParameters,
    )

    return ResponseEntity.ok(cas3BedspaceSearchResultTransformer.transformDomainToCas3BedspaceSearchResults(extractEntityFromCasResult(searchResult)))
  }

  private fun transformToTemporaryAccommodationBedSearchParameters(cas3BedspaceSearchParameters: Cas3BedspaceSearchParameters) = TemporaryAccommodationBedSearchParameters(
    cas3BedspaceSearchParameters.startDate,
    cas3BedspaceSearchParameters.durationDays,
    cas3BedspaceSearchParameters.probationDeliveryUnits,
    ServiceName.temporaryAccommodation.value,
    cas3BedspaceSearchParameters.premisesFilters,
    cas3BedspaceSearchParameters.bedspaceFilters,
    cas3BedspaceSearchParameters.attributes?.map { transformToBedSearchAttributes(it) },
  )

  private fun transformToBedSearchAttributes(bedspaceSearchAttribute: BedspaceSearchAttributes) = when (bedspaceSearchAttribute) {
    BedspaceSearchAttributes.SHARED_PROPERTY -> BedSearchAttributes.SHARED_PROPERTY
    BedspaceSearchAttributes.SINGLE_OCCUPANCY -> BedSearchAttributes.SINGLE_OCCUPANCY
    BedspaceSearchAttributes.WHEELCHAIR_ACCESSIBLE -> BedSearchAttributes.WHEELCHAIR_ACCESSIBLE
  }
}
