package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.transformer

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.Cas3BedspaceSearchResultBedspaceSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.Cas3BedspaceSearchResultPremisesSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.Cas3CharacteristicPair
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.Cas3v2BedspaceSearchResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.Cas3v2BedspaceSearchResultOverlap
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.Cas3v2BedspaceSearchResults
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.repository.Cas3v2CandidateBedspace

@Component
class Cas3v2BedspaceSearchResultsTransformer {
  fun transformDomainToApi(results: List<Cas3v2CandidateBedspace>) = Cas3v2BedspaceSearchResults(
    resultsBedspaceCount = results.distinctBy { it.bedspaceId }.size,
    resultsPremisesCount = results.distinctBy { it.premisesId }.size,
    results = results.map(::transformResult),
  )

  private fun transformResult(result: Cas3v2CandidateBedspace) = Cas3v2BedspaceSearchResult(
    premises = Cas3BedspaceSearchResultPremisesSummary(
      id = result.premisesId,
      name = result.premisesName,
      addressLine1 = result.premisesAddressLine1,
      postcode = result.premisesPostcode,
      characteristics = result.premisesCharacteristics.map {
        Cas3CharacteristicPair(
          name = it.name,
          description = it.description,
        )
      },
      addressLine2 = result.premisesAddressLine2,
      town = result.premisesTown,
      probationDeliveryUnitName = result.probationDeliveryUnitName,
      notes = result.premisesNotes,
      bedspaceCount = result.premisesBedspaceCount,
      bookedBedspaceCount = result.bookedBedspaceCount,
    ),
    bedspace = Cas3BedspaceSearchResultBedspaceSummary(
      id = result.bedspaceId,
      reference = result.bedspaceReference,
      characteristics = result.bedspaceCharacteristics.map {
        Cas3CharacteristicPair(
          name = it.name,
          description = it.description,
        )
      },
    ),
    overlaps = result.overlaps.map {
      Cas3v2BedspaceSearchResultOverlap(
        name = it.name,
        crn = it.crn,
        personType = it.personType,
        days = it.days,
        bookingId = it.bookingId,
        bedspaceId = it.bedspaceId,
        sex = it.sex,
        assessmentId = it.assessmentId,
        isSexualRisk = it.isSexualRisk,
      )
    },
  )
}
