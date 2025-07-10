package uk.gov.justice.digital.hmpps.approvedpremisesapi.controller.cas1

import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.cas1.SpaceSearchesCas1Delegate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1SpaceSearchParameters
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1SpaceSearchResults
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserPermission
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserAccessService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1PremisesSearchService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas1.Cas1SpaceSearchResultsTransformer

@Service
class Cas1SpaceSearchController(
  private val spaceSearchService: Cas1PremisesSearchService,
  private val spaceSearchResultTransformer: Cas1SpaceSearchResultsTransformer,
  private val userAccessService: UserAccessService,
) : SpaceSearchesCas1Delegate {
  override fun spaceSearch(cas1SpaceSearchParameters: Cas1SpaceSearchParameters): ResponseEntity<Cas1SpaceSearchResults> {
    userAccessService.ensureCurrentUserHasPermission(UserPermission.CAS1_SPACE_BOOKING_CREATE)

    val results = spaceSearchService.findPremises(
      Cas1PremisesSearchService.Cas1PremisesSearchCriteria(
        cas1SpaceSearchParameters.applicationId,
        cas1SpaceSearchParameters.targetPostcodeDistrict,
        cas1SpaceSearchParameters.spaceCharacteristics ?: emptyList(),
      ),
    )

    return ResponseEntity.ok(spaceSearchResultTransformer.transformDomainToApi(results))
  }
}
