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
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.BookingSearchSortField
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SortOrder
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApAreaEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.OffenderDetailsSummaryFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ProbationRegionEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.UserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BookingRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.AuthorisableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.BookingSearchService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.OffenderService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserService
import java.time.OffsetDateTime

class BookingSearchServiceTest {
  private val mockOffenderService = mockk<OffenderService>()
  private val mockUserService = mockk<UserService>()
  private val mockBookingRepository = mockk<BookingRepository>()

  private val bookingSearchService = BookingSearchService(
    mockOffenderService,
    mockUserService,
    mockBookingRepository,
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
    every { mockBookingRepository.findBookings(any(), any(), any(), any()) } returns PageImpl(
      listOf(
        TestBookingSearchResult(),
        TestBookingSearchResult(),
        TestBookingSearchResult(),
      ),
    )
    every { mockOffenderService.getOffenderByCrn(any(), any()) } returns AuthorisableActionResult.Success(
      OffenderDetailsSummaryFactory().produce(),
    )

    val (results, metaData) = bookingSearchService.findBookings(
      ServiceName.temporaryAccommodation,
      null,
      SortOrder.ascending,
      BookingSearchSortField.bookingCreatedAt,
      1,
    )

    assertThat(results).hasSize(3)
    assertThat(results).allMatch {
      it.personName != null
    }
    assertThat(metaData).isNotNull()
    verify(exactly = 1) {
      mockBookingRepository.findBookings(any(), any(), any(), any())
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
    every { mockBookingRepository.findBookings(any(), any(), any(), any()) } returns PageImpl(
      listOf(
        TestBookingSearchResult()
          .withBookingCreatedAt(OffsetDateTime.now().minusDays(3)),
        TestBookingSearchResult()
          .withBookingCreatedAt(OffsetDateTime.now().minusDays(2)),
        TestBookingSearchResult()
          .withBookingCreatedAt(OffsetDateTime.now().minusDays(1)),
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

    val (results, metadata) = bookingSearchService.findBookings(
      ServiceName.temporaryAccommodation,
      null,
      SortOrder.ascending,
      BookingSearchSortField.bookingCreatedAt,
      1,
    )
    assertThat(results).hasSize(2)
    assertThat(results).matches { result ->
      result.map { it.personName }.toSet() == setOf("Gregor Samsa", "Franz Kafka")
    }
    assertThat(metadata).isNotNull()
    verify(exactly = 1) {
      mockBookingRepository.findBookings(any(), any(), any(), any())
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
    every { mockBookingRepository.findBookings(any(), any(), any(), any()) } returns PageImpl(
      listOf(
        TestBookingSearchResult()
          .withBookingCreatedAt(OffsetDateTime.now().minusDays(3)),
        TestBookingSearchResult()
          .withBookingCreatedAt(OffsetDateTime.now().minusDays(2)),
        TestBookingSearchResult()
          .withBookingCreatedAt(OffsetDateTime.now().minusDays(1)),
      ),
    )
    every { mockOffenderService.getOffenderByCrn(any(), any()) } returnsMany listOf(
      AuthorisableActionResult.Success(OffenderDetailsSummaryFactory().produce()),
      AuthorisableActionResult.Success(OffenderDetailsSummaryFactory().produce()),
      AuthorisableActionResult.NotFound(),
    )

    val (results, metaData) = bookingSearchService.findBookings(
      ServiceName.temporaryAccommodation,
      null,
      SortOrder.ascending,
      BookingSearchSortField.bookingCreatedAt,
      1,
    )

    assertThat(results).hasSize(3)
    assertThat(results.dropLast(1)).allSatisfy {
      assertThat(it.personName).isNotNull()
    }
    assertThat(results.last().personName).isNull()
    assertThat(metaData).isNotNull()
    verify(exactly = 1) {
      mockBookingRepository.findBookings(any(), any(), any(), any())
    }
    verify(exactly = 3) {
      mockOffenderService.getOffenderByCrn(any(), any())
    }
  }

  @EnumSource(value = SortOrder::class)
  @ParameterizedTest
  fun `findBookings returns sorted results by person name and database default sort when page number is given`(sortOrder: SortOrder) {
    val pageSort = when (sortOrder) {
      SortOrder.ascending -> Sort.by("created_at").ascending()
      SortOrder.descending -> Sort.by("created_at").descending()
    }
    val pageable = PageRequest.of(0, 10, pageSort)
    every { mockUserService.getUserForRequest() } returns UserEntityFactory()
      .withYieldedProbationRegion {
        ProbationRegionEntityFactory()
          .withYieldedApArea {
            ApAreaEntityFactory().produce()
          }
          .produce()
      }
      .produce()
    every { mockBookingRepository.findBookings(any(), any(), any(), pageable) } returns PageImpl(
      listOf(
        TestBookingSearchResult(),
        TestBookingSearchResult(),
        TestBookingSearchResult(),
      ),
    )
    every { mockOffenderService.getOffenderByCrn(any(), any()) } returns AuthorisableActionResult.Success(
      OffenderDetailsSummaryFactory().produce(),
    )

    val (results, metaData) = bookingSearchService.findBookings(
      ServiceName.temporaryAccommodation,
      null,
      sortOrder,
      BookingSearchSortField.personName,
      1,
    )

    assertThat(results).hasSize(3)
    assertThat(results.map { it.personName }).isSortedAccordingTo { a, b ->
      when (sortOrder) {
        SortOrder.ascending -> compareValues(a, b)
        SortOrder.descending -> compareValues(b, a)
      }
    }
    assertThat(metaData).isNotNull()
    verify(exactly = 1) {
      mockBookingRepository.findBookings(any(), any(), any(), pageable)
    }
    verify(exactly = 3) {
      mockOffenderService.getOffenderByCrn(any(), any())
    }
  }

  @EnumSource(value = SortOrder::class)
  @ParameterizedTest
  fun `findBookings returns sorted results by person name and database default sort when page number is not given`(sortOrder: SortOrder) {
    val pageSort = when (sortOrder) {
      SortOrder.ascending -> Sort.by("created_at").ascending()
      SortOrder.descending -> Sort.by("created_at").descending()
    }
    val pageable = PageRequest.of(0, Int.MAX_VALUE, pageSort)
    every { mockUserService.getUserForRequest() } returns UserEntityFactory()
      .withYieldedProbationRegion {
        ProbationRegionEntityFactory()
          .withYieldedApArea {
            ApAreaEntityFactory().produce()
          }
          .produce()
      }
      .produce()
    every { mockBookingRepository.findBookings(any(), any(), any(), pageable) } returns PageImpl(
      listOf(
        TestBookingSearchResult(),
        TestBookingSearchResult(),
        TestBookingSearchResult(),
      ),
    )
    every { mockOffenderService.getOffenderByCrn(any(), any()) } returns AuthorisableActionResult.Success(
      OffenderDetailsSummaryFactory().produce(),
    )

    val (results, metaData) = bookingSearchService.findBookings(
      ServiceName.temporaryAccommodation,
      null,
      sortOrder,
      BookingSearchSortField.personName,
      null,
    )

    assertThat(results).hasSize(3)
    assertThat(results.map { it.personName }).isSortedAccordingTo { a, b ->
      when (sortOrder) {
        SortOrder.ascending -> compareValues(a, b)
        SortOrder.descending -> compareValues(b, a)
      }
    }
    assertThat(metaData).isNull()
    verify(exactly = 1) {
      mockBookingRepository.findBookings(any(), any(), any(), pageable)
    }
    verify(exactly = 3) {
      mockOffenderService.getOffenderByCrn(any(), any())
    }
  }

  @EnumSource(value = SortOrder::class)
  @ParameterizedTest
  fun `findBookings returns results and database sorted by crn when page number is given`(sortOrder: SortOrder) {
    val pageSort = when (sortOrder) {
      SortOrder.ascending -> Sort.by("crn").ascending()
      SortOrder.descending -> Sort.by("crn").descending()
    }
    val pageable = PageRequest.of(1, 10, pageSort)
    every { mockUserService.getUserForRequest() } returns UserEntityFactory()
      .withYieldedProbationRegion {
        ProbationRegionEntityFactory()
          .withYieldedApArea {
            ApAreaEntityFactory().produce()
          }
          .produce()
      }
      .produce()
    every { mockBookingRepository.findBookings(any(), any(), any(), pageable) } returns PageImpl(
      listOf(
        TestBookingSearchResult(),
        TestBookingSearchResult(),
        TestBookingSearchResult(),
      ),
    )
    every { mockOffenderService.getOffenderByCrn(any(), any()) } returns AuthorisableActionResult.Success(
      OffenderDetailsSummaryFactory().produce(),
    )

    val (results, metaData) = bookingSearchService.findBookings(
      ServiceName.temporaryAccommodation,
      null,
      sortOrder,
      BookingSearchSortField.personCrn,
      2,
    )

    assertThat(results).hasSize(3)
    assertThat(metaData).isNotNull()
    verify(exactly = 1) {
      mockBookingRepository.findBookings(any(), any(), any(), pageable)
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
    every { mockBookingRepository.findBookings(any(), any(), any(), any()) } returns PageImpl(emptyList())

    val (results, metaData) = bookingSearchService.findBookings(
      ServiceName.temporaryAccommodation,
      null,
      SortOrder.ascending,
      BookingSearchSortField.bookingCreatedAt,
      1,
    )

    assertThat(results).hasSize(0)
    assertThat(metaData).isNotNull()
    verify(exactly = 1) {
      mockBookingRepository.findBookings(any(), any(), any(), any())
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
    every { mockBookingRepository.findBookings(any(), any(), any(), any()) } returns PageImpl(emptyList())

    val (results, metaData) = bookingSearchService.findBookings(
      ServiceName.temporaryAccommodation,
      null,
      SortOrder.ascending,
      BookingSearchSortField.bookingCreatedAt,
      null,
    )

    assertThat(results).hasSize(0)
    assertThat(metaData).isNull()
    verify(exactly = 1) {
      mockBookingRepository.findBookings(any(), any(), any(), any())
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
    every { mockBookingRepository.findBookings(any(), any(), any(), any()) }.throws(
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
      mockBookingRepository.findBookings(any(), any(), any(), any())
    }
    verify(exactly = 0) {
      mockOffenderService.getOffenderByCrn(any(), any())
    }
  }
}
