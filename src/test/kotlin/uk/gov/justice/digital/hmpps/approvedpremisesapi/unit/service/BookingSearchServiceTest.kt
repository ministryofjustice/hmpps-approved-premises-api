package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service

import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.BookingSearchSortField
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SortOrder
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApAreaEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.BookingSearchResultFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.OffenderDetailsSummaryFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ProbationRegionEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.UserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.repository.BookingSearchRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.AuthorisableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.ValidatableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.BookingSearchService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.OffenderService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserService

class BookingSearchServiceTest {
  private val mockBookingSearchRepository = mockk<BookingSearchRepository>()
  private val mockOffenderService = mockk<OffenderService>()
  private val mockUserService = mockk<UserService>()

  private val bookingSearchService = BookingSearchService(
    mockBookingSearchRepository,
    mockOffenderService,
    mockUserService,
  )

  @Test
  fun `findBookings returns results from repository when offender details are found for all bookings`() {
    every { mockUserService.getUserForRequest() } returns UserEntityFactory()
      .withYieldedProbationRegion {
        ProbationRegionEntityFactory()
          .withYieldedApArea {
            ApAreaEntityFactory().produce()
          }
          .produce()
      }
      .produce()

    every { mockBookingSearchRepository.findBookings(any(), any()) } returns listOf(
      BookingSearchResultFactory().produce(),
      BookingSearchResultFactory().produce(),
      BookingSearchResultFactory().produce(),
    )

    every { mockOffenderService.getOffenderByCrn(any(), any()) } returns AuthorisableActionResult.Success(OffenderDetailsSummaryFactory().produce())

    val result = bookingSearchService.findBookings(ServiceName.temporaryAccommodation, null, SortOrder.ascending, BookingSearchSortField.bookingCreatedAt)

    assertThat(result is AuthorisableActionResult.Success).isTrue
    result as AuthorisableActionResult.Success
    assertThat(result.entity is ValidatableActionResult.Success).isTrue
    val validationResult = result.entity as ValidatableActionResult.Success
    assertThat(validationResult.entity).hasSize(3)
    assertThat(validationResult.entity).allMatch {
      it.personName != null
    }
  }

  @Test
  fun `findBookings returns Unauthorised when the user is not authorised to get offender details for a particular CRN`() {
    every { mockUserService.getUserForRequest() } returns UserEntityFactory()
      .withYieldedProbationRegion {
        ProbationRegionEntityFactory()
          .withYieldedApArea {
            ApAreaEntityFactory().produce()
          }
          .produce()
      }
      .produce()

    every { mockBookingSearchRepository.findBookings(any(), any()) } returns listOf(
      BookingSearchResultFactory().produce(),
      BookingSearchResultFactory().produce(),
      BookingSearchResultFactory().produce(),
    )

    every { mockOffenderService.getOffenderByCrn(any(), any()) } returnsMany listOf(
      AuthorisableActionResult.Success(OffenderDetailsSummaryFactory().produce()),
      AuthorisableActionResult.Success(OffenderDetailsSummaryFactory().produce()),
      AuthorisableActionResult.Unauthorised(),
    )

    val result = bookingSearchService.findBookings(ServiceName.temporaryAccommodation, null, SortOrder.ascending, BookingSearchSortField.bookingCreatedAt)

    assertThat(result is AuthorisableActionResult.Unauthorised).isTrue
  }

  @Test
  fun `findBookings does not provide a person's name when offender details for a particular CRN could not be found`() {
    every { mockUserService.getUserForRequest() } returns UserEntityFactory()
      .withYieldedProbationRegion {
        ProbationRegionEntityFactory()
          .withYieldedApArea {
            ApAreaEntityFactory().produce()
          }
          .produce()
      }
      .produce()

    every { mockBookingSearchRepository.findBookings(any(), any()) } returns listOf(
      BookingSearchResultFactory().produce(),
      BookingSearchResultFactory().produce(),
      BookingSearchResultFactory().produce(),
    )

    every { mockOffenderService.getOffenderByCrn(any(), any()) } returnsMany listOf(
      AuthorisableActionResult.Success(OffenderDetailsSummaryFactory().produce()),
      AuthorisableActionResult.Success(OffenderDetailsSummaryFactory().produce()),
      AuthorisableActionResult.NotFound(),
    )

    val result = bookingSearchService.findBookings(ServiceName.temporaryAccommodation, null, SortOrder.ascending, BookingSearchSortField.bookingCreatedAt)

    assertThat(result is AuthorisableActionResult.Success).isTrue
    result as AuthorisableActionResult.Success
    assertThat(result.entity is ValidatableActionResult.Success).isTrue
    val validationResult = result.entity as ValidatableActionResult.Success
    assertThat(validationResult.entity).hasSize(3)
    assertThat(validationResult.entity.dropLast(1)).allSatisfy {
      assertThat(it.personName).isNotNull()
    }
    assertThat(validationResult.entity.last().personName).isNull()
  }
}
