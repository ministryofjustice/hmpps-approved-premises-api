package uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas1

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1PremiseCharacteristic
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1PremisesSearchResultSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1SpaceSearchParameters
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1SpaceSearchResults
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NamedId
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas1.CandidatePremises
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.asApiType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1SpaceSearchResult as ApiSpaceSearchResult

@Component
class Cas1SpaceSearchResultsTransformer {

  private val log = LoggerFactory.getLogger(this::class.java)

  @SuppressWarnings("SwallowedException")
  fun transformDomainToApi(searchParameters: Cas1SpaceSearchParameters, results: List<CandidatePremises>) =
    Cas1SpaceSearchResults(
      resultsCount = results.size,
      searchCriteria = searchParameters,
      results = results.map { candidatePremises ->
        ApiSpaceSearchResult(
          premises = Cas1PremisesSearchResultSummary(
            id = candidatePremises.premisesId,
            apType = candidatePremises.apType.asApiType(),
            name = candidatePremises.name,
            addressLine1 = candidatePremises.addressLine1,
            addressLine2 = candidatePremises.addressLine2,
            town = candidatePremises.town,
            postcode = candidatePremises.postcode,
            apArea = NamedId(
              id = candidatePremises.apAreaId,
              name = candidatePremises.apAreaName,
            ),
            premisesCharacteristics = emptyList(),
            characteristics = candidatePremises.characteristics.mapNotNull {
              try {
                Cas1PremiseCharacteristic.valueOf(it)
              } catch (e: IllegalArgumentException) {
                log.warn("Couldn't find an enum entry for propertyName $it")
                null
              }
            },
          ),
          distanceInMiles = candidatePremises.distanceInMiles.toBigDecimal(),
          spacesAvailable = listOf(),
        )
      },
    )
}
