package uk.gov.justice.digital.hmpps.approvedpremisesapi.service

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
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.validated
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.AuthorisableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.ValidatableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.getMetadata
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.getPageable
import java.time.OffsetDateTime
import java.time.ZoneOffset
import kotlin.Comparator

@Service
class BookingSearchService(
  private val offenderService: OffenderService,
  private val userService: UserService,
  private val bookingRepository: BookingRepository,
) {
  fun findBookings(
    serviceName: ServiceName,
    status: BookingStatus?,
    sortOrder: SortOrder,
    sortField: BookingSearchSortField,
    page: Int?,
  ): Pair<AuthorisableActionResult.Success<ValidatableActionResult<List<BookingSearchResultDto>>>, PaginationMetadata?> {
    val user = userService.getUserForRequest()
    val probationRegionId = when (serviceName) {
      ServiceName.temporaryAccommodation -> user.probationRegion.id
      else -> null
    }
    val findBookings = bookingRepository.findBookings(
      serviceName.value,
      status,
      probationRegionId,
      buildPage(sortOrder, sortField, page),
    )
    val results = updatePersonNameFromOffenderDetail(findBookings, user)
    val sortBookingResult = sortBookingResult(page, sortField, sortOrder, results)
    val success = AuthorisableActionResult.Success(
      validated {
        return@validated success(sortBookingResult)
      },
    )
    return Pair(success, getMetadata(findBookings, page))
  }

  private fun updatePersonNameFromOffenderDetail(
    findBookings: Page<BookingSearchResult>,
    user: UserEntity,
  ) = mapToBookingSearchResults(findBookings)
    .mapNotNull { result ->
      val offenderDetails =
        when (val offenderDetailsResult = offenderService.getOffenderByCrn(result.personCrn, user.deliusUsername)) {
          is AuthorisableActionResult.Success -> offenderDetailsResult.entity
          is AuthorisableActionResult.Unauthorised -> return@mapNotNull null
          is AuthorisableActionResult.NotFound -> null
        }
      result.personName = offenderDetails?.let { "${it.firstName} ${it.surname}" }
      result
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
  ): Pageable? {
    val sortDirection = when (sortOrder) {
      SortOrder.ascending -> SortDirection.asc
      else -> SortDirection.desc
    }
    val sortingField = convertSortFieldToDBField(sortField)
    return getPageable(sortingField, sortDirection, page)
  }

  private fun convertSortFieldToDBField(sortField: BookingSearchSortField) =
    when (sortField) {
      BookingSearchSortField.bookingEndDate -> "departure_date"
      BookingSearchSortField.bookingStartDate -> "arrival_date"
      BookingSearchSortField.bookingCreatedAt -> "created_at"
      BookingSearchSortField.personCrn -> "crn"
      else -> "created_at"
    }

  private fun sortBookingResult(
    page: Int?,
    sortField: BookingSearchSortField,
    sortOrder: SortOrder,
    results: List<BookingSearchResultDto>,
  ): List<BookingSearchResultDto> {
    if ((page == null) || (sortField == BookingSearchSortField.personName)) {
      val comparator = Comparator<BookingSearchResultDto> { a, b ->
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
