package uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.BedSearchResultBedSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.BedSearchResultPremisesSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.BedSearchResultRoomSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.BedSearchResults
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.CharacteristicPair
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApprovedPremisesBedSearchResult as ApiApprovedPremisesBedSearchResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.TemporaryAccommodationBedSearchResult as ApiTemporaryAccommodationBedSearchResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.TemporaryAccommodationBedSearchResultOverlap as ApiTemporaryAccommodationBedSearchResultOverlap
import uk.gov.justice.digital.hmpps.approvedpremisesapi.repository.ApprovedPremisesBedSearchResult as DomainApprovedPremisesBedSearchResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.repository.BedSearchResult as DomainBedSearchResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.repository.TemporaryAccommodationBedSearchResult as DomainTemporaryAccommodationBedSearchResult

@Component
class BedSearchResultTransformer {
  fun transformDomainToApi(results: List<DomainBedSearchResult>) = BedSearchResults(
    resultsRoomCount = results.distinctBy { it.roomId }.size,
    resultsPremisesCount = results.distinctBy { it.premisesId }.size,
    resultsBedCount = results.size,
    results = results.map(::transformResult),
  )

  private fun transformResult(result: DomainBedSearchResult) = when (result) {
    is DomainApprovedPremisesBedSearchResult -> ApiApprovedPremisesBedSearchResult(
      distanceMiles = result.distance.toBigDecimal(),
      premises = BedSearchResultPremisesSummary(
        id = result.premisesId,
        name = result.premisesName,
        addressLine1 = result.premisesAddressLine1,
        postcode = result.premisesPostcode,
        characteristics = result.premisesCharacteristics.map {
          CharacteristicPair(
            name = it.name,
            propertyName = it.propertyName,
          )
        },
        addressLine2 = result.premisesAddressLine2,
        town = result.premisesTown,
        bedCount = result.premisesBedCount,
      ),
      room = BedSearchResultRoomSummary(
        id = result.roomId,
        name = result.roomName,
        characteristics = result.roomCharacteristics.map {
          CharacteristicPair(
            name = it.name,
            propertyName = it.propertyName,
          )
        },
      ),
      bed = BedSearchResultBedSummary(
        id = result.bedId,
        name = result.bedName,
      ),
      serviceName = ServiceName.approvedPremises,
    )
    is DomainTemporaryAccommodationBedSearchResult -> ApiTemporaryAccommodationBedSearchResult(
      premises = BedSearchResultPremisesSummary(
        id = result.premisesId,
        name = result.premisesName,
        addressLine1 = result.premisesAddressLine1,
        postcode = result.premisesPostcode,
        characteristics = result.premisesCharacteristics.map {
          CharacteristicPair(
            name = it.name,
            propertyName = it.propertyName,
          )
        },
        addressLine2 = result.premisesAddressLine2,
        town = result.premisesTown,
        probationDeliveryUnitName = result.probationDeliveryUnitName,
        notes = result.premisesNotes,
        bedCount = result.premisesBedCount,
      ),
      room = BedSearchResultRoomSummary(
        id = result.roomId,
        name = result.roomName,
        characteristics = result.roomCharacteristics.map {
          CharacteristicPair(
            name = it.name,
            propertyName = it.propertyName,
          )
        },
      ),
      bed = BedSearchResultBedSummary(
        id = result.bedId,
        name = result.bedName,
      ),
      serviceName = ServiceName.temporaryAccommodation,
      overlaps = result.overlaps.map {
        ApiTemporaryAccommodationBedSearchResultOverlap(
          crn = it.crn,
          days = it.days,
          bookingId = it.bookingId,
          roomId = it.roomId,
        )
      },
    )
  }
}
