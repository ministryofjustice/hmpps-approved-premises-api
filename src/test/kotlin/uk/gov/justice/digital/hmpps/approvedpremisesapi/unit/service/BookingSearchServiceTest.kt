package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.springframework.dao.DataRetrievalFailureException
import org.springframework.data.domain.PageImpl
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
import java.time.OffsetDateTime

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
    every { mockBookingSearchRepository.findBookings(any(), any(), any(), any()) } returns PageImpl(
      listOf(
        BookingSearchResultFactory().produce(),
        BookingSearchResultFactory().produce(),
        BookingSearchResultFactory().produce(),
      ),
    )
    every { mockOffenderService.getOffenderByCrn(any(), any()) } returns AuthorisableActionResult.Success(
      OffenderDetailsSummaryFactory().produce(),
    )

    val (result, metaData) = bookingSearchService.findBookings(
      ServiceName.temporaryAccommodation,
      null,
      SortOrder.ascending,
      BookingSearchSortField.bookingCreatedAt,
      1,
    )

    assertThat(result is AuthorisableActionResult.Success).isTrue
    assertThat(result.entity is ValidatableActionResult.Success).isTrue
    val validationResult = result.entity as ValidatableActionResult.Success
    assertThat(validationResult.entity).hasSize(3)
    assertThat(validationResult.entity).allMatch {
      it.personName != null
    }
    assertThat(metaData).isNotNull()
    verify(exactly = 1) {
      mockBookingSearchRepository.findBookings(any(), any(), any(), any())
    }
    verify(exactly = 3) {
      mockOffenderService.getOffenderByCrn(any(), any())
    }
  }

  @Test
  fun `findBookings filters out bookings where the user is not authorised to get offender details for a particular CRN`() {
    every { mockUserService.getUserForRequest() } returns UserEntityFactory()
      .withYieldedProbationRegion {
        ProbationRegionEntityFactory()
          .withYieldedApArea {
            ApAreaEntityFactory().produce()
          }
          .produce()
      }
      .produce()
    every { mockBookingSearchRepository.findBookings(any(), any(), any(), any()) } returns PageImpl(
      listOf(
        BookingSearchResultFactory()
          .withBookingCreatedAt(OffsetDateTime.now().minusDays(3))
          .produce(),
        BookingSearchResultFactory()
          .withBookingCreatedAt(OffsetDateTime.now().minusDays(2))
          .produce(),
        BookingSearchResultFactory()
          .withBookingCreatedAt(OffsetDateTime.now().minusDays(1))
          .produce(),
      ),
    )
    every { mockOffenderService.getOffenderByCrn(any(), any()) } returnsMany listOf(
      AuthorisableActionResult.Success(
        OffenderDetailsSummaryFactory()
          .withFirstName("Gregor")
          .withLastName("Samsa")
          .produce(),
      ),
      AuthorisableActionResult.Success(
        OffenderDetailsSummaryFactory()
          .withFirstName("Franz")
          .withLastName("Kafka")
          .produce(),
      ),
      AuthorisableActionResult.Unauthorised(),
    )

    val (result, metadata) = bookingSearchService.findBookings(
      ServiceName.temporaryAccommodation,
      null,
      SortOrder.ascending,
      BookingSearchSortField.bookingCreatedAt,
      1,
    )
    assertThat(result is AuthorisableActionResult.Success).isTrue
    assertThat(result.entity is ValidatableActionResult.Success).isTrue
    val validationResult = result.entity as ValidatableActionResult.Success
    assertThat(validationResult.entity).hasSize(2)
    assertThat(validationResult.entity).matches { results ->
      results.map { it.personName }.toSet() == setOf("Gregor Samsa", "Franz Kafka")
    }
    assertThat(metadata).isNotNull()
    verify(exactly = 1) {
      mockBookingSearchRepository.findBookings(any(), any(), any(), any())
    }
    verify(exactly = 3) {
      mockOffenderService.getOffenderByCrn(any(), any())
    }
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
    every { mockBookingSearchRepository.findBookings(any(), any(), any(), any()) } returns PageImpl(
      listOf(
        BookingSearchResultFactory()
          .withBookingCreatedAt(OffsetDateTime.now().minusDays(3))
          .produce(),
        BookingSearchResultFactory()
          .withBookingCreatedAt(OffsetDateTime.now().minusDays(2))
          .produce(),
        BookingSearchResultFactory()
          .withBookingCreatedAt(OffsetDateTime.now().minusDays(1))
          .produce(),
      ),
    )
    every { mockOffenderService.getOffenderByCrn(any(), any()) } returnsMany listOf(
      AuthorisableActionResult.Success(OffenderDetailsSummaryFactory().produce()),
      AuthorisableActionResult.Success(OffenderDetailsSummaryFactory().produce()),
      AuthorisableActionResult.NotFound(),
    )

    val (result, metaData) = bookingSearchService.findBookings(
      ServiceName.temporaryAccommodation,
      null,
      SortOrder.ascending,
      BookingSearchSortField.bookingCreatedAt,
      1,
    )

    assertThat(result is AuthorisableActionResult.Success).isTrue
    assertThat(result.entity is ValidatableActionResult.Success).isTrue
    val validationResult = result.entity as ValidatableActionResult.Success
    assertThat(validationResult.entity).hasSize(3)
    assertThat(validationResult.entity.dropLast(1)).allSatisfy {
      assertThat(it.personName).isNotNull()
    }
    assertThat(validationResult.entity.last().personName).isNull()
    assertThat(metaData).isNotNull()
    verify(exactly = 1) {
      mockBookingSearchRepository.findBookings(any(), any(), any(), any())
    }
    verify(exactly = 3) {
      mockOffenderService.getOffenderByCrn(any(), any())
    }
  }

  @EnumSource(value = SortOrder::class)
  @ParameterizedTest
  fun `findBookings returns results sorted according to person name when page number is not given`(sortOrder: SortOrder) {
    every { mockUserService.getUserForRequest() } returns UserEntityFactory()
      .withYieldedProbationRegion {
        ProbationRegionEntityFactory()
          .withYieldedApArea {
            ApAreaEntityFactory().produce()
          }
          .produce()
      }
      .produce()
    every { mockBookingSearchRepository.findBookings(any(), any(), any(), any()) } returns PageImpl(
      listOf(
        BookingSearchResultFactory().produce(),
        BookingSearchResultFactory().produce(),
        BookingSearchResultFactory().produce(),
      ),
    )
    every { mockOffenderService.getOffenderByCrn(any(), any()) } returns AuthorisableActionResult.Success(
      OffenderDetailsSummaryFactory().produce(),
    )

    val (result, metaData) = bookingSearchService.findBookings(
      ServiceName.temporaryAccommodation,
      null,
      sortOrder,
      BookingSearchSortField.personName,
      null,
    )

    assertThat(result is AuthorisableActionResult.Success).isTrue
    assertThat(result.entity is ValidatableActionResult.Success).isTrue
    val validationResult = result.entity as ValidatableActionResult.Success
    assertThat(validationResult.entity).hasSize(3)
    assertThat(validationResult.entity.map { it.personName }).isSortedAccordingTo { a, b ->
      when (sortOrder) {
        SortOrder.ascending -> compareValues(a, b)
        SortOrder.descending -> compareValues(b, a)
      }
    }
    assertThat(metaData).isNull()
    verify(exactly = 1) {
      mockBookingSearchRepository.findBookings(any(), any(), any(), any())
    }
    verify(exactly = 3) {
      mockOffenderService.getOffenderByCrn(any(), any())
    }
  }

  @EnumSource(value = SortOrder::class)
  @ParameterizedTest
  fun `findBookings returns results sorted according to person name when page number is given`(sortOrder: SortOrder) {
    every { mockUserService.getUserForRequest() } returns UserEntityFactory()
      .withYieldedProbationRegion {
        ProbationRegionEntityFactory()
          .withYieldedApArea {
            ApAreaEntityFactory().produce()
          }
          .produce()
      }
      .produce()
    every { mockBookingSearchRepository.findBookings(any(), any(), any(), any()) } returns PageImpl(
      listOf(
        BookingSearchResultFactory().produce(),
        BookingSearchResultFactory().produce(),
        BookingSearchResultFactory().produce(),
      ),
    )
    every { mockOffenderService.getOffenderByCrn(any(), any()) } returns AuthorisableActionResult.Success(
      OffenderDetailsSummaryFactory().produce(),
    )

    val (result, metaData) = bookingSearchService.findBookings(
      ServiceName.temporaryAccommodation,
      null,
      sortOrder,
      BookingSearchSortField.personName,
      1,
    )

    assertThat(result is AuthorisableActionResult.Success).isTrue
    assertThat(result.entity is ValidatableActionResult.Success).isTrue
    val validationResult = result.entity as ValidatableActionResult.Success
    assertThat(validationResult.entity).hasSize(3)
    assertThat(validationResult.entity.map { it.personName }).isSortedAccordingTo { a, b ->
      when (sortOrder) {
        SortOrder.ascending -> compareValues(a, b)
        SortOrder.descending -> compareValues(b, a)
      }
    }
    assertThat(metaData).isNotNull()
    verify(exactly = 1) {
      mockBookingSearchRepository.findBookings(any(), any(), any(), any())
    }
    verify(exactly = 3) {
      mockOffenderService.getOffenderByCrn(any(), any())
    }
  }

  @EnumSource(value = SortOrder::class)
  @ParameterizedTest
  fun `findBookings returns results sorted according to person CRN`(sortOrder: SortOrder) {
    val bookingSearchResults = listOf(
      BookingSearchResultFactory().produce(),
      BookingSearchResultFactory().produce(),
      BookingSearchResultFactory().produce(),
    )

    every { mockUserService.getUserForRequest() } returns UserEntityFactory()
      .withYieldedProbationRegion {
        ProbationRegionEntityFactory()
          .withYieldedApArea {
            ApAreaEntityFactory().produce()
          }
          .produce()
      }
      .produce()
    every {
      mockBookingSearchRepository.findBookings(
        any(),
        any(),
        any(),
        any(),
      )
    } returns PageImpl(bookingSearchResults)
    every { mockOffenderService.getOffenderByCrn(any(), any()) } returns AuthorisableActionResult.Success(
      OffenderDetailsSummaryFactory().produce(),
    )

    val (result, metaData) = bookingSearchService.findBookings(
      ServiceName.temporaryAccommodation,
      null,
      sortOrder,
      BookingSearchSortField.personCrn,
      1,
    )

    assertThat(result is AuthorisableActionResult.Success).isTrue
    assertThat(result.entity is ValidatableActionResult.Success).isTrue
    val validationResult = result.entity as ValidatableActionResult.Success
    assertThat(validationResult.entity).hasSize(3)
    assertThat(validationResult.entity.map { it.personCrn }).hasSameElementsAs(bookingSearchResults.map { it.personCrn })
    assertThat(metaData).isNotNull()
    verify(exactly = 1) {
      mockBookingSearchRepository.findBookings(any(), any(), any(), any())
    }
    verify(exactly = 3) {
      mockOffenderService.getOffenderByCrn(any(), any())
    }
  }

  @EnumSource(value = SortOrder::class)
  @ParameterizedTest
  fun `findBookings returns results sorted according to person CRN when page number is not provided`(sortOrder: SortOrder) {
    every { mockUserService.getUserForRequest() } returns UserEntityFactory()
      .withYieldedProbationRegion {
        ProbationRegionEntityFactory()
          .withYieldedApArea {
            ApAreaEntityFactory().produce()
          }
          .produce()
      }
      .produce()
    every { mockBookingSearchRepository.findBookings(any(), any(), any(), any()) } returns PageImpl(
      listOf(
        BookingSearchResultFactory().produce(),
        BookingSearchResultFactory().produce(),
        BookingSearchResultFactory().produce(),
      ),
    )
    every { mockOffenderService.getOffenderByCrn(any(), any()) } returns AuthorisableActionResult.Success(
      OffenderDetailsSummaryFactory().produce(),
    )

    val (result, metaData) = bookingSearchService.findBookings(
      ServiceName.temporaryAccommodation,
      null,
      sortOrder,
      BookingSearchSortField.personCrn,
      null,
    )

    assertThat(result is AuthorisableActionResult.Success).isTrue
    assertThat(result.entity is ValidatableActionResult.Success).isTrue
    val validationResult = result.entity as ValidatableActionResult.Success
    assertThat(validationResult.entity).hasSize(3)
    assertThat(validationResult.entity.map { it.personCrn }).isSortedAccordingTo { a, b ->
      when (sortOrder) {
        SortOrder.ascending -> compareValues(a, b)
        SortOrder.descending -> compareValues(b, a)
      }
    }
    assertThat(metaData).isNull()
    verify(exactly = 1) {
      mockBookingSearchRepository.findBookings(any(), any(), any(), any())
    }
    verify(exactly = 3) {
      mockOffenderService.getOffenderByCrn(any(), any())
    }
  }

  @EnumSource(value = SortOrder::class)
  @ParameterizedTest
  fun `findBookings returns results sorted according to booking start date`(sortOrder: SortOrder) {
    every { mockUserService.getUserForRequest() } returns UserEntityFactory()
      .withYieldedProbationRegion {
        ProbationRegionEntityFactory()
          .withYieldedApArea {
            ApAreaEntityFactory().produce()
          }
          .produce()
      }
      .produce()

    every { mockBookingSearchRepository.findBookings(any(), any(), any(), any()) } returns PageImpl(
      listOf(
        BookingSearchResultFactory().produce(),
        BookingSearchResultFactory().produce(),
        BookingSearchResultFactory().produce(),
      ),
    )
    every { mockOffenderService.getOffenderByCrn(any(), any()) } returns AuthorisableActionResult.Success(
      OffenderDetailsSummaryFactory().produce(),
    )

    val (result, metaData) = bookingSearchService.findBookings(
      ServiceName.temporaryAccommodation,
      null,
      sortOrder,
      BookingSearchSortField.bookingStartDate,
      null,
    )

    assertThat(result is AuthorisableActionResult.Success).isTrue
    assertThat(result.entity is ValidatableActionResult.Success).isTrue
    val validationResult = result.entity as ValidatableActionResult.Success
    assertThat(validationResult.entity).hasSize(3)
    assertThat(validationResult.entity.map { it.bookingStartDate }).isSortedAccordingTo { a, b ->
      when (sortOrder) {
        SortOrder.ascending -> compareValues(a, b)
        SortOrder.descending -> compareValues(b, a)
      }
    }
    assertThat(metaData).isNull()
    verify(exactly = 1) {
      mockBookingSearchRepository.findBookings(any(), any(), any(), any())
    }
    verify(exactly = 3) {
      mockOffenderService.getOffenderByCrn(any(), any())
    }
  }

  @EnumSource(value = SortOrder::class)
  @ParameterizedTest
  fun `findBookings returns results sorted according to booking end date`(sortOrder: SortOrder) {
    every { mockUserService.getUserForRequest() } returns UserEntityFactory()
      .withYieldedProbationRegion {
        ProbationRegionEntityFactory()
          .withYieldedApArea {
            ApAreaEntityFactory().produce()
          }
          .produce()
      }
      .produce()

    every { mockBookingSearchRepository.findBookings(any(), any(), any(), any()) } returns PageImpl(
      listOf(
        BookingSearchResultFactory().produce(),
        BookingSearchResultFactory().produce(),
        BookingSearchResultFactory().produce(),
      ),
    )
    every { mockOffenderService.getOffenderByCrn(any(), any()) } returns AuthorisableActionResult.Success(
      OffenderDetailsSummaryFactory().produce(),
    )

    val (result, metaData) = bookingSearchService.findBookings(
      ServiceName.temporaryAccommodation,
      null,
      sortOrder,
      BookingSearchSortField.bookingEndDate,
      null,
    )

    assertThat(result is AuthorisableActionResult.Success).isTrue
    assertThat(result.entity is ValidatableActionResult.Success).isTrue
    val validationResult = result.entity as ValidatableActionResult.Success
    assertThat(validationResult.entity).hasSize(3)
    assertThat(validationResult.entity.map { it.bookingEndDate }).isSortedAccordingTo { a, b ->
      when (sortOrder) {
        SortOrder.ascending -> compareValues(a, b)
        SortOrder.descending -> compareValues(b, a)
      }
    }
    assertThat(metaData).isNull()
    verify(exactly = 1) {
      mockBookingSearchRepository.findBookings(any(), any(), any(), any())
    }
    verify(exactly = 3) {
      mockOffenderService.getOffenderByCrn(any(), any())
    }
  }

  @EnumSource(value = SortOrder::class)
  @ParameterizedTest
  fun `findBookings returns results sorted according to booking creation`(sortOrder: SortOrder) {
    every { mockUserService.getUserForRequest() } returns UserEntityFactory()
      .withYieldedProbationRegion {
        ProbationRegionEntityFactory()
          .withYieldedApArea {
            ApAreaEntityFactory().produce()
          }
          .produce()
      }
      .produce()
    every { mockBookingSearchRepository.findBookings(any(), any(), any(), any()) } returns PageImpl(
      listOf(
        BookingSearchResultFactory().produce(),
        BookingSearchResultFactory().produce(),
        BookingSearchResultFactory().produce(),
      ),
    )
    every { mockOffenderService.getOffenderByCrn(any(), any()) } returns AuthorisableActionResult.Success(
      OffenderDetailsSummaryFactory().produce(),
    )

    val (result, metaData) = bookingSearchService.findBookings(
      ServiceName.temporaryAccommodation,
      null,
      sortOrder,
      BookingSearchSortField.bookingCreatedAt,
      null,
    )

    assertThat(result is AuthorisableActionResult.Success).isTrue
    assertThat(result.entity is ValidatableActionResult.Success).isTrue
    val validationResult = result.entity as ValidatableActionResult.Success
    assertThat(validationResult.entity).hasSize(3)
    assertThat(validationResult.entity.map { it.bookingCreatedAt }).isSortedAccordingTo { a, b ->
      when (sortOrder) {
        SortOrder.ascending -> compareValues(a, b)
        SortOrder.descending -> compareValues(b, a)
      }
    }
    assertThat(metaData).isNull()
    verify(exactly = 1) {
      mockBookingSearchRepository.findBookings(any(), any(), any(), any())
    }
    verify(exactly = 3) {
      mockOffenderService.getOffenderByCrn(any(), any())
    }
  }

  @EnumSource(value = SortOrder::class)
  @ParameterizedTest
  fun `findBookings returns results not sorted according to booking creation when page number is given`(sortOrder: SortOrder) {
    val bookingSearchResults = listOf(
      BookingSearchResultFactory().produce(),
      BookingSearchResultFactory().produce(),
      BookingSearchResultFactory().produce(),
    )

    every { mockUserService.getUserForRequest() } returns UserEntityFactory()
      .withYieldedProbationRegion {
        ProbationRegionEntityFactory()
          .withYieldedApArea {
            ApAreaEntityFactory().produce()
          }
          .produce()
      }
      .produce()
    every {
      mockBookingSearchRepository.findBookings(
        any(),
        any(),
        any(),
        any(),
      )
    } returns PageImpl(bookingSearchResults)
    every { mockOffenderService.getOffenderByCrn(any(), any()) } returns AuthorisableActionResult.Success(
      OffenderDetailsSummaryFactory().produce(),
    )

    val (result, metaData) = bookingSearchService.findBookings(
      ServiceName.temporaryAccommodation,
      null,
      sortOrder,
      BookingSearchSortField.bookingCreatedAt,
      1,
    )

    assertThat(result is AuthorisableActionResult.Success).isTrue
    assertThat(result.entity is ValidatableActionResult.Success).isTrue
    val validationResult = result.entity as ValidatableActionResult.Success
    assertThat(validationResult.entity).hasSize(3)
    val sortedBookingResult = when (sortOrder) {
      SortOrder.ascending -> bookingSearchResults.sortedBy { it.bookingCreatedAt }
      SortOrder.descending -> bookingSearchResults.sortedByDescending { it.bookingCreatedAt }
    }
    assertThat(validationResult.entity).isNotSameAs(sortedBookingResult)
    assertThat(validationResult.entity).containsExactlyInAnyOrderElementsOf(sortedBookingResult)
    assertThat(metaData).isNotNull()
    verify(exactly = 1) {
      mockBookingSearchRepository.findBookings(any(), any(), any(), any())
    }
    verify(exactly = 3) {
      mockOffenderService.getOffenderByCrn(any(), any())
    }
  }

  @EnumSource(value = SortOrder::class)
  @ParameterizedTest
  fun `findBookings returns results sorted when page number is given but sorting criteria by person name`(sortOrder: SortOrder) {
    every { mockUserService.getUserForRequest() } returns UserEntityFactory()
      .withYieldedProbationRegion {
        ProbationRegionEntityFactory()
          .withYieldedApArea {
            ApAreaEntityFactory().produce()
          }
          .produce()
      }
      .produce()
    every { mockBookingSearchRepository.findBookings(any(), any(), any(), any()) } returns PageImpl(
      listOf(
        BookingSearchResultFactory().produce(),
        BookingSearchResultFactory().produce(),
        BookingSearchResultFactory().produce(),
      ),
    )
    every { mockOffenderService.getOffenderByCrn(any(), any()) } returns AuthorisableActionResult.Success(
      OffenderDetailsSummaryFactory().produce(),
    )

    val (result, metaData) = bookingSearchService.findBookings(
      ServiceName.temporaryAccommodation,
      null,
      sortOrder,
      BookingSearchSortField.personName,
      1,
    )

    assertThat(result is AuthorisableActionResult.Success).isTrue
    assertThat(result.entity is ValidatableActionResult.Success).isTrue
    val validationResult = result.entity as ValidatableActionResult.Success
    assertThat(validationResult.entity).hasSize(3)

    assertThat(validationResult.entity.map { it.personName }).isSortedAccordingTo { a, b ->
      when (sortOrder) {
        SortOrder.ascending -> compareValues(a, b)
        SortOrder.descending -> compareValues(b, a)
      }
    }
    assertThat(metaData).isNotNull()
    verify(exactly = 1) {
      mockBookingSearchRepository.findBookings(any(), any(), any(), any())
    }
    verify(exactly = 3) {
      mockOffenderService.getOffenderByCrn(any(), any())
    }
  }

  @Test
  fun `findBookings returns empty results from repository when page number is given`() {
    every { mockUserService.getUserForRequest() } returns UserEntityFactory()
      .withYieldedProbationRegion {
        ProbationRegionEntityFactory()
          .withYieldedApArea {
            ApAreaEntityFactory().produce()
          }
          .produce()
      }
      .produce()
    every { mockBookingSearchRepository.findBookings(any(), any(), any(), any()) } returns PageImpl(emptyList())

    val (result, metaData) = bookingSearchService.findBookings(
      ServiceName.temporaryAccommodation,
      null,
      SortOrder.ascending,
      BookingSearchSortField.bookingCreatedAt,
      1,
    )

    assertThat(result is AuthorisableActionResult.Success).isTrue
    assertThat(result.entity is ValidatableActionResult.Success).isTrue
    val validationResult = result.entity as ValidatableActionResult.Success
    assertThat(validationResult.entity).hasSize(0)
    assertThat(validationResult.entity).allMatch {
      it.personName != null
    }
    assertThat(metaData).isNotNull()
    verify(exactly = 1) {
      mockBookingSearchRepository.findBookings(any(), any(), any(), any())
    }
    verify(exactly = 0) {
      mockOffenderService.getOffenderByCrn(any(), any())
    }
  }

  @Test
  fun `findBookings returns empty results from repository when page number is not given`() {
    every { mockUserService.getUserForRequest() } returns UserEntityFactory()
      .withYieldedProbationRegion {
        ProbationRegionEntityFactory()
          .withYieldedApArea {
            ApAreaEntityFactory().produce()
          }
          .produce()
      }
      .produce()
    every { mockBookingSearchRepository.findBookings(any(), any(), any(), any()) } returns PageImpl(emptyList())

    val (result, metaData) = bookingSearchService.findBookings(
      ServiceName.temporaryAccommodation,
      null,
      SortOrder.ascending,
      BookingSearchSortField.bookingCreatedAt,
      null,
    )

    assertThat(result is AuthorisableActionResult.Success).isTrue
    assertThat(result.entity is ValidatableActionResult.Success).isTrue
    val validationResult = result.entity as ValidatableActionResult.Success
    assertThat(validationResult.entity).hasSize(0)
    assertThat(validationResult.entity).allMatch {
      it.personName != null
    }
    assertThat(metaData).isNull()
    verify(exactly = 1) {
      mockBookingSearchRepository.findBookings(any(), any(), any(), any())
    }
    verify(exactly = 0) {
      mockOffenderService.getOffenderByCrn(any(), any())
    }
  }

  @Test
  fun `throw exception when DB exception happend`() {
    every { mockUserService.getUserForRequest() } returns UserEntityFactory()
      .withYieldedProbationRegion {
        ProbationRegionEntityFactory()
          .withYieldedApArea {
            ApAreaEntityFactory().produce()
          }
          .produce()
      }
      .produce()
    every { mockBookingSearchRepository.findBookings(any(), any(), any(), any()) }.throws(
      DataRetrievalFailureException(
        "some-exception",
      ),
    )

    Assertions.assertThrows(DataRetrievalFailureException::class.java) {
      bookingSearchService.findBookings(
        ServiceName.temporaryAccommodation,
        null,
        SortOrder.ascending,
        BookingSearchSortField.bookingCreatedAt,
        1,
      )
    }
    verify(exactly = 1) {
      mockBookingSearchRepository.findBookings(any(), any(), any(), any())
    }
    verify(exactly = 0) {
      mockOffenderService.getOffenderByCrn(any(), any())
    }
  }
}
