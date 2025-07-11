package uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas1

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1PremisesSearchResultSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1SpaceCharacteristic
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1SpaceSearchResults
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NamedId
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas1.CandidatePremises
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.asApiType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1SpaceSearchResult as ApiSpaceSearchResult

@Component
class Cas1SpaceSearchResultsTransformer {

  @SuppressWarnings("SwallowedException")
  fun transformDomainToApi(results: List<CandidatePremises>) = Cas1SpaceSearchResults(
    resultsCount = results.size,
    results = results.map { candidatePremises ->
      ApiSpaceSearchResult(
        premises = toPremisesSearchResultSummary(candidatePremises),
        distanceInMiles = candidatePremises.distanceInMiles!!.toBigDecimal(),
      )
    },
  )

  fun toPremisesSearchResultSummary(premises: CandidatePremises) = Cas1PremisesSearchResultSummary(
    id = premises.premisesId,
    apType = premises.apType.asApiType(),
    name = premises.name,
    fullAddress = premises.resolveFullAddress(),
    postcode = premises.postcode,
    apArea = NamedId(
      id = premises.apAreaId,
      name = premises.apAreaName,
    ),
    characteristics = premises.characteristics.map {
      Cas1SpaceCharacteristic.valueOf(it)
    },
  )

  fun CandidatePremises.resolveFullAddress() = ApprovedPremisesEntity.resolveFullAddress(
    fullAddress = fullAddress,
    addressLine1 = addressLine1,
    addressLine2 = addressLine2,
    town = town,
  )
}
