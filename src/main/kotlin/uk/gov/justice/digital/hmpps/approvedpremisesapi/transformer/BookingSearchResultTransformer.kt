package uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.BookingSearchResultBedSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.BookingSearchResultBookingSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.BookingSearchResultPersonSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.BookingSearchResultPremisesSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.BookingSearchResultRoomSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.BookingSearchResults
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.BookingStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.BookingSearchResult as ApiBookingSearchResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.BookingSearchResultDto as DomainBookingSearchResult

@Component
class BookingSearchResultTransformer {
  fun transformDomainToApi(results: List<DomainBookingSearchResult>) = BookingSearchResults(
    resultsCount = results.size,
    results = results.map(::transformResult),
  )

  private fun transformResult(result: DomainBookingSearchResult) = ApiBookingSearchResult(
    person = BookingSearchResultPersonSummary(
      name = result.personName,
      crn = result.personCrn,
    ),
    booking = BookingSearchResultBookingSummary(
      id = result.bookingId,
      status = BookingStatus.values().find { it.value == result.bookingStatus } ?: throw RuntimeException("Unknown booking status ${result.bookingStatus}"),
      startDate = result.bookingStartDate,
      endDate = result.bookingEndDate,
      createdAt = result.bookingCreatedAt.toInstant(),
    ),
    premises = BookingSearchResultPremisesSummary(
      id = result.premisesId,
      name = result.premisesName,
      addressLine1 = result.premisesAddressLine1,
      addressLine2 = result.premisesAddressLine2,
      town = result.premisesTown,
      postcode = result.premisesPostcode,
    ),
    room = BookingSearchResultRoomSummary(
      id = result.roomId,
      name = result.roomName,
    ),
    bed = BookingSearchResultBedSummary(
      id = result.bedId,
      name = result.bedName,
    ),
  )
}
