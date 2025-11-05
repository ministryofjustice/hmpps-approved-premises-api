package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.service.v2

import org.springframework.beans.factory.annotation.Value
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SortDirection
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3v2BookingRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3v2BookingSearchResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.Cas3BookingSearchResultDto
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.Cas3BookingSearchSortField
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.Cas3BookingStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PaginationMetadata
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.OffenderService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas3LaoStrategy
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.getMetadataWithSize
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.getNameFromPersonSummaryInfoResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.getPageableOrAllPages
import java.time.OffsetDateTime
import java.time.ZoneOffset

@Service
class Cas3v2BookingSearchService(
  private val cas3BookingRepository: Cas3v2BookingRepository,
  private val offenderService: OffenderService,
  private val userService: UserService,
  @param:Value("\${pagination.cas3.booking-search-page-size}") private val cas3BookingSearchPageSize: Int,
) {

  fun findBookings(
    status: Cas3BookingStatus?,
    sortDirection: SortDirection,
    sortField: Cas3BookingSearchSortField,
    page: Int?,
    crnOrName: String?,
  ): Pair<List<Cas3BookingSearchResultDto>, PaginationMetadata?> {
    val user = userService.getUserForRequest()
    val findBookings = cas3BookingRepository.findBookings(
      status?.name,
      user.probationRegion.id,
      crnOrName,
      buildPage(sortDirection, sortField, page, cas3BookingSearchPageSize),
    )

    var results = updateRestrictedAndPersonNameFromOffenderDetail(
      mapToBookingSearchResults(findBookings),
      user,
    )

    if (sortField == Cas3BookingSearchSortField.PERSON_NAME) {
      results = sortBookingResultByPersonName(results, sortDirection)
    }

    return Pair(results, getMetadataWithSize(findBookings, page, cas3BookingSearchPageSize))
  }

  private fun sortBookingResultByPersonName(
    results: List<Cas3BookingSearchResultDto>,
    sortDirection: SortDirection,
  ): List<Cas3BookingSearchResultDto> {
    val comparator = compareBy<Cas3BookingSearchResultDto> { it.personName }
    return if (sortDirection == SortDirection.asc) {
      results.sortedWith(comparator)
    } else {
      results.sortedWith(comparator.reversed())
    }
  }

  private fun mapToBookingSearchResults(findBookings: Page<Cas3v2BookingSearchResult>) = findBookings.content
    .mapNotNull { rs ->
      Cas3BookingSearchResultDto(
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
        rs.getBedspaceId(),
        rs.getBedspaceReference(),
      )
    }

  private fun buildPage(
    sortDirection: SortDirection,
    sortField: Cas3BookingSearchSortField,
    page: Int?,
    pageSize: Int,
  ): Pageable {
    val sortingField = convertSortFieldToDBField(sortField)
    return getPageableOrAllPages(sortingField, sortDirection, page, pageSize)
  }

  private fun convertSortFieldToDBField(sortField: Cas3BookingSearchSortField) = when (sortField) {
    Cas3BookingSearchSortField.BOOKING_END_DATE -> listOf("departure_date", "personName")
    Cas3BookingSearchSortField.BOOKING_START_DATE -> listOf("arrival_date", "personName")
    Cas3BookingSearchSortField.BOOKING_CREATED_AT -> listOf("created_at")
    Cas3BookingSearchSortField.PERSON_CRN -> listOf("crn")
    Cas3BookingSearchSortField.PERSON_NAME -> listOf("personName")
  }

  private fun updateRestrictedAndPersonNameFromOffenderDetail(
    bookingSearchResultDtos: List<Cas3BookingSearchResultDto>,
    user: UserEntity,
  ): List<Cas3BookingSearchResultDto> {
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
}
