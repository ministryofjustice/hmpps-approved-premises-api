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

    val results = bookingSearchRepository.findBookings(
      serviceName,
      status,
    )

    results.forEach { result ->
      val offenderDetails = when (val offenderDetailsResult = offenderService.getOffenderByCrn(result.personCrn, user.deliusUsername)) {
        is AuthorisableActionResult.Success -> offenderDetailsResult.entity
        is AuthorisableActionResult.Unauthorised -> return AuthorisableActionResult.Unauthorised()
        is AuthorisableActionResult.NotFound -> null
      }

      result.personName = offenderDetails?.let { "${it.firstName} ${it.surname}" }
    }

    return AuthorisableActionResult.Success(
      validated {
        return@validated success(results)
      }
    )
  }
}
