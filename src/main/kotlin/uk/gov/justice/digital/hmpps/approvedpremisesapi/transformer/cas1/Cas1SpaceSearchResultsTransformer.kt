package uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas1

import org.slf4j.LoggerFactory
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

  private val log = LoggerFactory.getLogger(this::class.java)

  @SuppressWarnings("SwallowedException")
  fun transformDomainToApi(results: List<CandidatePremises>) = Cas1SpaceSearchResults(
    resultsCount = results.size,
    results = results.map { candidatePremises ->
      ApiSpaceSearchResult(
        premises = Cas1PremisesSearchResultSummary(
          id = candidatePremises.premisesId,
          apType = candidatePremises.apType.asApiType(),
          name = candidatePremises.name,
          fullAddress = candidatePremises.resolveFullAddress(),
          postcode = candidatePremises.postcode,
          apArea = NamedId(
            id = candidatePremises.apAreaId,
            name = candidatePremises.apAreaName,
          ),
          characteristics = candidatePremises.characteristics.mapNotNull {
            try {
              Cas1SpaceCharacteristic.valueOf(it)
            } catch (e: IllegalArgumentException) {
              log.warn("Couldn't find a Cas1SpaceCharacteristic enum entry for propertyName $it")
              null
            }
          },
        ),
        distanceInMiles = candidatePremises.distanceInMiles.toBigDecimal(),
      )
    },
  )

  fun CandidatePremises.resolveFullAddress() = ApprovedPremisesEntity.resolveFullAddress(
    fullAddress = fullAddress,
    addressLine1 = addressLine1,
    addressLine2 = addressLine2,
    town = town,
  )
}
