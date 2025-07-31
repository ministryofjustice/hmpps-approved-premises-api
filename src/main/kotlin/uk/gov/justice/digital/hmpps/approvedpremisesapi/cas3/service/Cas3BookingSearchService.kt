package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.service

import org.springframework.beans.factory.annotation.Value
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.BookingSearchSortField
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.BookingStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SortDirection
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SortOrder
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas3BookingRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas3BookingSearchResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PaginationMetadata
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.OffenderService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas3LaoStrategy
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.getMetadataWithSize
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.getNameFromPersonSummaryInfoResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.getPageableOrAllPages
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

@Service
class Cas3BookingSearchService(
  private val offenderService: OffenderService,
  private val userService: UserService,
  private val cas3BookingRepository: Cas3BookingRepository,
  @Value("\${pagination.cas3.booking-search-page-size}") private val cas3BookingSearchPageSize: Int,
) {
  fun findBookings(
    status: BookingStatus?,
    sortOrder: SortOrder,
    sortField: BookingSearchSortField,
    page: Int?,
    crnOrName: String?,
  ): Pair<List<BookingSearchResultDto>, PaginationMetadata?> {
    val user = userService.getUserForRequest()
    val findBookings = cas3BookingRepository.findTemporaryAccommodationBookings(
      status?.name,
      user.probationRegion.id,
      crnOrName,
      buildPage(sortOrder, sortField, page, cas3BookingSearchPageSize),
    )

    var results = updateRestrictedAndPersonNameFromOffenderDetail(
      mapToBookingSearchResults(findBookings),
      user,
    )

    if (sortField == BookingSearchSortField.personName) {
      results = sortBookingResultByPersonName(results, sortOrder)
    }

    return Pair(results, getMetadataWithSize(findBookings, page, cas3BookingSearchPageSize))
  }

  private fun updateRestrictedAndPersonNameFromOffenderDetail(
    bookingSearchResultDtos: List<BookingSearchResultDto>,
    user: UserEntity,
  ): List<BookingSearchResultDto> {
    val offenderSummaries = offenderService.getPersonSummaryInfoResults(
      bookingSearchResultDtos.map { it.personCrn }.toSet(),
      user.cas3LaoStrategy(),
    )

    return bookingSearchResultDtos
      .map { result -> result to offenderSummaries.first { it.crn == result.personCrn } }
      .map { (result, offenderSummary) ->
        result.personName = getNameFromPersonSummaryInfoResult(offenderSummary)
        result
      }
  }

  private fun mapToBookingSearchResults(findBookings: Page<Cas3BookingSearchResult>) = findBookings.content
    .mapNotNull { rs ->
      BookingSearchResultDto(
        rs.getPersonName(),
        rs.getPersonCrn(),
        rs.getBookingId(),
        rs.getBookingStatus(),
        rs.getBookingStartDate(),
        rs.getBookingEndDate(),
        OffsetDateTime.ofInstant(rs.getBookingCreatedAt(), ZoneOffset.UTC),
        rs.getPremisesId(),
        rs.getPremisesName(),
        rs.getPremisesAddressLine1(),
        rs.getPremisesAddressLine2(),
        rs.getPremisesTown(),
        rs.getPremisesPostcode(),
        rs.getRoomId(),
        rs.getRoomName(),
        rs.getBedId(),
        rs.getBedName(),
      )
    }

  private fun buildPage(
    sortOrder: SortOrder,
    sortField: BookingSearchSortField,
    page: Int?,
    pageSize: Int,
  ): Pageable {
    val sortDirection = when (sortOrder) {
      SortOrder.ascending -> SortDirection.asc
      else -> SortDirection.desc
    }
    val sortingField = convertSortFieldToDBField(sortField)
    return getPageableOrAllPages(sortingField, sortDirection, page, pageSize)
  }

  private fun convertSortFieldToDBField(sortField: BookingSearchSortField) = when (sortField) {
    BookingSearchSortField.bookingEndDate -> listOf("departure_date", "personName")
    BookingSearchSortField.bookingStartDate -> listOf("arrival_date", "personName")
    BookingSearchSortField.bookingCreatedAt -> listOf("created_at")
    BookingSearchSortField.personCrn -> listOf("crn")
    BookingSearchSortField.personName -> listOf("personName")
    else -> listOf("created_at")
  }

  private fun sortBookingResultByPersonName(
    results: List<BookingSearchResultDto>,
    sortOrder: SortOrder,
  ): List<BookingSearchResultDto> {
    val comparator = Comparator<BookingSearchResultDto> { a, b ->
      val ascendingCompare = compareValues(a.personName, b.personName)
      when (sortOrder) {
        SortOrder.ascending -> ascendingCompare
        SortOrder.descending -> -ascendingCompare
      }
    }
    return results.sortedWith(comparator)
  }
}

data class BookingSearchResultDto(
  var personName: String?,
  val personCrn: String,
  val bookingId: UUID,
  val bookingStatus: String,
  val bookingStartDate: LocalDate,
  val bookingEndDate: LocalDate,
  val bookingCreatedAt: OffsetDateTime,
  val premisesId: UUID,
  val premisesName: String,
  val premisesAddressLine1: String,
  val premisesAddressLine2: String?,
  val premisesTown: String?,
  val premisesPostcode: String,
  val roomId: UUID,
  val roomName: String,
  val bedId: UUID,
  val bedName: String,
)
