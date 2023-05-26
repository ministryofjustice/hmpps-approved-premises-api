package uk.gov.justice.digital.hmpps.approvedpremisesapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.BookingSearchSortField
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.BookingStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SortOrder
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.validated
import uk.gov.justice.digital.hmpps.approvedpremisesapi.repository.BookingSearchRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.repository.BookingSearchResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.AuthorisableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.ValidatableActionResult

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
  ): AuthorisableActionResult<ValidatableActionResult<List<BookingSearchResult>>> {
    val user = userService.getUserForRequest()

    val probationRegionId = when (serviceName) {
      ServiceName.temporaryAccommodation -> user.probationRegion.id
      else -> null
    }

    val results = bookingSearchRepository.findBookings(
      serviceName,
      status,
      probationRegionId,
    )

    results.forEach { result ->
      val offenderDetails = when (val offenderDetailsResult = offenderService.getOffenderByCrn(result.personCrn, user.deliusUsername)) {
        is AuthorisableActionResult.Success -> offenderDetailsResult.entity
        is AuthorisableActionResult.Unauthorised -> return AuthorisableActionResult.Unauthorised()
        is AuthorisableActionResult.NotFound -> null
      }

      result.personName = offenderDetails?.let { "${it.firstName} ${it.surname}" }
    }

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

    return AuthorisableActionResult.Success(
      validated {
        return@validated success(results.sortedWith(comparator))
      },
    )
  }
}
