package uk.gov.justice.digital.hmpps.approvedpremisesapi.service

import org.springframework.beans.factory.annotation.Value
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.BookingSearchSortField
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.BookingStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SortDirection
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SortOrder
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BookingRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BookingSearchResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.BookingSearchResultDto
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PaginationMetadata
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonSummaryInfoResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.getMetadataWithSize
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.getPageableOrAllPages
import java.time.OffsetDateTime
import java.time.ZoneOffset
import kotlin.Comparator

@Service
class BookingSearchService(
  private val offenderService: OffenderService,
  private val userService: UserService,
  private val bookingRepository: BookingRepository,
  @Value("\${pagination.cas3.booking-search-page-size}") private val cas3BookingSearchPageSize: Int,
  @Value("\${pagination.default-page-size}") private val defaultSearchPageSize: Int,
) {
  fun findBookings(
    serviceName: ServiceName,
    status: BookingStatus?,
    sortOrder: SortOrder,
    sortField: BookingSearchSortField,
    page: Int?,
    crn: String?,
  ): Pair<List<BookingSearchResultDto>, PaginationMetadata?> {
    val user = userService.getUserForRequest()
    val probationRegionId = when (serviceName) {
      ServiceName.temporaryAccommodation -> user.probationRegion.id
      else -> null
    }
    val pageSize = when (serviceName) {
      ServiceName.temporaryAccommodation -> cas3BookingSearchPageSize
      else -> defaultSearchPageSize
    }
    val findBookings = bookingRepository.findBookings(
      serviceName.value,
      status,
      probationRegionId,
      crn,
      buildPage(sortOrder, sortField, page, pageSize),
    )
    var results = removeRestrictedAndUpdatePersonNameFromOffenderDetail(
      mapToBookingSearchResults(findBookings),
      user,
    )
    if (sortField == BookingSearchSortField.personName) {
      results = sortBookingResultByPersonName(results, sortOrder)
    }

    return Pair(results, getMetadataWithSize(findBookings, page, pageSize))
  }

  private fun removeRestrictedAndUpdatePersonNameFromOffenderDetail(
    bookingSearchResultDtos: List<BookingSearchResultDto>,
    user: UserEntity,
  ): List<BookingSearchResultDto> {
    val offenderSummaries = offenderService.getOffenderSummariesByCrns(
      bookingSearchResultDtos.map { it.personCrn }.toSet(),
      user.deliusUsername,
      ignoreLaoRestrictions = false,
      forceApDeliusContextApi = false,
    )

    return bookingSearchResultDtos
      .map { result -> result to offenderSummaries.first { it.crn == result.personCrn } }
      .filter { (_, offenderSummary) -> offenderSummary !is PersonSummaryInfoResult.Success.Restricted }
      .map { (result, offenderSummary) ->
        result
        result.personName = when (offenderSummary) {
          is PersonSummaryInfoResult.Success.Full -> "${offenderSummary.summary.name.forename} ${offenderSummary.summary.name.surname}"
          else -> null
        }
        result
      }
  }

  private fun mapToBookingSearchResults(findBookings: Page<BookingSearchResult>) =
    findBookings.content
      .mapNotNull { rs ->
        BookingSearchResultDto(
          rs.getPersonName(),
          rs.getPersonCrn(),
          rs.getBookingId(),
          rs.getBookingStatus(),
          rs.getBookingStartDate(),
          rs.getBookingEndDate(),
          OffsetDateTime.ofInstant(rs.getBookingCreatedAt().toInstant(), ZoneOffset.UTC),
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
  ): Pageable? {
    val sortDirection = when (sortOrder) {
      SortOrder.ascending -> SortDirection.asc
      else -> SortDirection.desc
    }
    val sortingField = convertSortFieldToDBField(sortField)
    return getPageableOrAllPages(sortingField, sortDirection, page, pageSize)
  }

  private fun convertSortFieldToDBField(sortField: BookingSearchSortField) =
    when (sortField) {
      BookingSearchSortField.bookingEndDate -> "departure_date"
      BookingSearchSortField.bookingStartDate -> "arrival_date"
      BookingSearchSortField.bookingCreatedAt -> "created_at"
      BookingSearchSortField.personCrn -> "crn"
      else -> "created_at"
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
