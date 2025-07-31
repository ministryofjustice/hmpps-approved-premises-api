package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.transformer

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.Cas3BookingSearchResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.Cas3BookingSearchResultBedspaceSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.Cas3BookingSearchResultBookingSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.Cas3BookingSearchResultDto
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.Cas3BookingSearchResultPersonSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.Cas3BookingSearchResultPremisesSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.Cas3BookingSearchResults
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.Cas3BookingStatus

@Component
class Cas3BookingSearchResultTransformer {
  fun transformDomainToApi(results: List<Cas3BookingSearchResultDto>) = Cas3BookingSearchResults(
    resultsCount = results.size,
    results = results.map(::transformResult),
  )

  private fun transformResult(result: Cas3BookingSearchResultDto) = Cas3BookingSearchResult(
    person = Cas3BookingSearchResultPersonSummary(
      name = result.personName,
      crn = result.personCrn,
    ),
    booking = Cas3BookingSearchResultBookingSummary(
      id = result.bookingId,
      status = Cas3BookingStatus.entries.find { it.value == result.bookingStatus } ?: throw IllegalArgumentException("Unknown booking status ${result.bookingStatus}"),
      startDate = result.bookingStartDate,
      endDate = result.bookingEndDate,
      createdAt = result.bookingCreatedAt.toInstant(),
    ),
    premises = Cas3BookingSearchResultPremisesSummary(
      id = result.premisesId,
      name = result.premisesName,
      addressLine1 = result.premisesAddressLine1,
      addressLine2 = result.premisesAddressLine2,
      town = result.premisesTown,
      postcode = result.premisesPostcode,
    ),
    bedspace = Cas3BookingSearchResultBedspaceSummary(
      id = result.bedspaceId,
      reference = result.bedspaceReference,
    ),
  )
}
