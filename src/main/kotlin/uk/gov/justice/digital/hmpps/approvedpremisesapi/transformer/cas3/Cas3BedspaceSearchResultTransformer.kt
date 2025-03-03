package uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas3

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.BedSearchResultBedSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.BedSearchResultPremisesSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.BedSearchResultRoomSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.BedSearchResults
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas3BedspaceSearchResults
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.CharacteristicPair
import uk.gov.justice.digital.hmpps.approvedpremisesapi.repository.Cas3BedspaceSearchResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas3BedspaceSearchResult as ApiCas3BedspaceSearchResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas3BedspaceSearchResultOverlap as ApiCas3BedspaceSearchResultOverlap
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.TemporaryAccommodationBedSearchResult as ApiTemporaryAccommodationBedSearchResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.TemporaryAccommodationBedSearchResultOverlap as ApiTemporaryAccommodationBedSearchResultOverlap

@Component
class Cas3BedspaceSearchResultTransformer {
  fun transformDomainToApi(results: List<Cas3BedspaceSearchResult>) = BedSearchResults(
    resultsRoomCount = results.distinctBy { it.roomId }.size,
    resultsPremisesCount = results.distinctBy { it.premisesId }.size,
    resultsBedCount = results.size,
    results = results.map(::transformResult),
  )

  fun transformDomainToCas3BedspaceSearchResults(results: List<Cas3BedspaceSearchResult>) = Cas3BedspaceSearchResults(
    resultsRoomCount = results.distinctBy { it.roomId }.size,
    resultsPremisesCount = results.distinctBy { it.premisesId }.size,
    resultsBedCount = results.size,
    results = results.map(::transformCas3BedspaceSearchResult),
  )

  private fun transformResult(result: Cas3BedspaceSearchResult) = ApiTemporaryAccommodationBedSearchResult(
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
      bookedBedCount = result.bookedBedCount,
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
    overlaps = result.overlaps.map {
      ApiTemporaryAccommodationBedSearchResultOverlap(
        name = it.name,
        crn = it.crn,
        personType = it.personType,
        days = it.days,
        bookingId = it.bookingId,
        roomId = it.roomId,
        sex = it.sex,
        assessmentId = it.assessmentId,
        isSexualRisk = it.isSexualRisk,
      )
    },
  )

  private fun transformCas3BedspaceSearchResult(result: Cas3BedspaceSearchResult) = ApiCas3BedspaceSearchResult(
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
      bookedBedCount = result.bookedBedCount,
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
    overlaps = result.overlaps.map {
      ApiCas3BedspaceSearchResultOverlap(
        name = it.name,
        crn = it.crn,
        personType = it.personType,
        days = it.days,
        bookingId = it.bookingId,
        roomId = it.roomId,
        sex = it.sex,
        assessmentId = it.assessmentId,
        isSexualRisk = it.isSexualRisk,
      )
    },
  )
}
