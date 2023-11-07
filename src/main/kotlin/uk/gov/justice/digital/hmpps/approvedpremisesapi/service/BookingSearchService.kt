package uk.gov.justice.digital.hmpps.approvedpremisesapi.service

import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.BookingSearchSortField
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.BookingStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SortDirection
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SortOrder
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PaginationMetadata
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.validated
import uk.gov.justice.digital.hmpps.approvedpremisesapi.repository.BookingSearchRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.repository.BookingSearchResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.AuthorisableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.ValidatableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.getMetadata
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.getPageable

@Service
class BookingSearchService(
  private val bookingSearchRepository: BookingSearchRepository,
  private val offenderService: OffenderService,
  private val userService: UserService,
) {
  fun findBookings(
    serviceName: ServiceName,
    status: BookingStatus?,
    sortOrder: SortOrder,
    sortField: BookingSearchSortField,
    page: Int?,
  ): Pair<AuthorisableActionResult.Success<ValidatableActionResult<List<BookingSearchResult>>>, PaginationMetadata?> {
    val user = userService.getUserForRequest()

    val probationRegionId = when (serviceName) {
      ServiceName.temporaryAccommodation -> user.probationRegion.id
      else -> null
    }
    val findBookings = bookingSearchRepository.findBookings(
      serviceName,
      status,
      probationRegionId,
      buildPage(sortOrder, sortField, page),
    )
    val results = findBookings.content
      .mapNotNull { result ->
        val offenderDetails = when (val offenderDetailsResult = offenderService.getOffenderByCrn(result.personCrn, user.deliusUsername)) {
          is AuthorisableActionResult.Success -> offenderDetailsResult.entity
          is AuthorisableActionResult.Unauthorised -> return@mapNotNull null
          is AuthorisableActionResult.NotFound -> null
        }
        result.personName = offenderDetails?.let { "${it.firstName} ${it.surname}" }
        result
      }

    val sortBookingResult = sortBookingResult(page, sortField, sortOrder, results)
    val success = AuthorisableActionResult.Success(
      validated {
        return@validated success(sortBookingResult)
      },
    )
    return Pair(success, getMetadata(findBookings, page))
  }

  private fun buildPage(
    sortOrder: SortOrder,
    sortField: BookingSearchSortField,
    page: Int?,
  ): Pageable? {
    val sortDirection = when (sortOrder) {
      SortOrder.ascending -> SortDirection.asc
      else -> SortDirection.desc
    }

    return getPageable(sortField.value, sortDirection, page)
  }

  private fun sortBookingResult(
    page: Int?,
    sortField: BookingSearchSortField,
    sortOrder: SortOrder,
    results: List<BookingSearchResult>,
  ): List<BookingSearchResult> {
    if ((page == null) || (sortField == BookingSearchSortField.personName)) {
      val comparator = Comparator<BookingSearchResult> { a, b ->
        val ascendingCompare = when (sortField) {
          BookingSearchSortField.personName -> compareValues(a.personName, b.personName)
          BookingSearchSortField.personCrn -> compareValues(a.personCrn, b.personCrn)
          BookingSearchSortField.bookingStartDate -> compareValues(a.bookingStartDate, b.bookingStartDate)
          BookingSearchSortField.bookingEndDate -> compareValues(a.bookingEndDate, b.bookingEndDate)
          BookingSearchSortField.bookingCreatedAt -> compareValues(a.bookingCreatedAt, b.bookingCreatedAt)
        }

        when (sortOrder) {
          SortOrder.ascending -> ascendingCompare
          SortOrder.descending -> -ascendingCompare
        }
      }
      return results.sortedWith(comparator)
    }
    return results
  }
}
