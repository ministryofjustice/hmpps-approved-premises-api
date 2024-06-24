package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
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
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.CaseSummaryFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.NameFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.OffenderDetailsSummaryFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ProbationRegionEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.UserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BookingRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonSummaryInfoResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.AuthorisableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.BookingSearchService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.OffenderService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.PaginationConfig
import java.time.OffsetDateTime

class BookingSearchServiceTest {
  private val mockOffenderService = mockk<OffenderService>()
  private val mockUserService = mockk<UserService>()
  private val mockBookingRepository = mockk<BookingRepository>()
  private val cas3BookingSearchPageSize = 50
  private val defaultBookingSearchPageSize = 20

  private val bookingSearchService = BookingSearchService(
    mockOffenderService,
    mockUserService,
    mockBookingRepository,
    cas3BookingSearchPageSize,
    defaultBookingSearchPageSize,
  )

  @BeforeEach
  fun before() {
    PaginationConfig(defaultPageSize = 10).postInit()
  }

  @Test
  fun `findBookings returns results from repository when offender details are found for all bookings`() {
    val crns = setOf("crn1", "crn2", "crn3")

    every { mockUserService.getUserForRequest() } returns UserEntityFactory()
      .withYieldedProbationRegion {
        ProbationRegionEntityFactory()
          .withYieldedApArea {
            ApAreaEntityFactory().produce()
          }
          .produce()
      }
      .produce()

    every { mockBookingRepository.findBookings(any(), any(), any(), any(), any()) } returns PageImpl(
      crns.map { TestBookingSearchResult().withPersonCrn(it) },
    )

    every { mockOffenderService.getOffenderSummariesByCrns(crns, any(), any()) } returns
      crns.map { PersonSummaryInfoResult.Success.Full(it, CaseSummaryFactory().produce()) }

    val (results, metaData) = bookingSearchService.findBookings(
      ServiceName.temporaryAccommodation,
      null,
      SortOrder.ascending,
      BookingSearchSortField.bookingCreatedAt,
      1,
      null,
    )

    assertThat(results).hasSize(3)
    assertThat(results).allMatch {
      it.personName != null
    }
    assertThat(metaData).isNotNull()
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
    every { mockBookingRepository.findBookings(any(), any(), any(), any(), any()) } returns PageImpl(
      listOf(
        TestBookingSearchResult()
          .withPersonCrn("crn1")
          .withBookingCreatedAt(OffsetDateTime.now().minusDays(3)),
        TestBookingSearchResult()
          .withPersonCrn("crn2")
          .withBookingCreatedAt(OffsetDateTime.now().minusDays(2)),
        TestBookingSearchResult()
          .withPersonCrn("crn3")
          .withBookingCreatedAt(OffsetDateTime.now().minusDays(1)),
      ),
    )
    every { mockOffenderService.getOffenderSummariesByCrns(setOf("crn1", "crn2", "crn3"), any(), any()) } returns
      listOf(
        PersonSummaryInfoResult.Success.Full("crn1", CaseSummaryFactory().withName(NameFactory().withForename("Gregor").withSurname("Samsa").produce()).produce()),
        PersonSummaryInfoResult.Success.Full("crn2", CaseSummaryFactory().withName(NameFactory().withForename("Franz").withSurname("Kafka").produce()).produce()),
        PersonSummaryInfoResult.Success.Restricted("crn3", "crn3noms"),
      )

    val (results, metadata) = bookingSearchService.findBookings(
      ServiceName.temporaryAccommodation,
      null,
      SortOrder.ascending,
      BookingSearchSortField.bookingCreatedAt,
      1,
      null,
    )
    assertThat(results).hasSize(2)
    assertThat(results).matches { result ->
      result.map { it.personName }.toSet() == setOf("Gregor Samsa", "Franz Kafka")
    }
    assertThat(metadata).isNotNull()
    verify(exactly = 1) {
      mockBookingRepository.findBookings(any(), any(), any(), any(), any())
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
    every { mockBookingRepository.findBookings(any(), any(), any(), any(), any()) } returns PageImpl(
      listOf(
        TestBookingSearchResult()
          .withPersonCrn("crn1")
          .withBookingCreatedAt(OffsetDateTime.now().minusDays(3)),
        TestBookingSearchResult()
          .withPersonCrn("crn2")
          .withBookingCreatedAt(OffsetDateTime.now().minusDays(2)),
        TestBookingSearchResult()
          .withPersonCrn("crn3")
          .withBookingCreatedAt(OffsetDateTime.now().minusDays(1)),
      ),
    )
    every { mockOffenderService.getOffenderSummariesByCrns(setOf("crn1", "crn2", "crn3"), any(), any()) } returns
      listOf(
        PersonSummaryInfoResult.Success.Full("crn1", CaseSummaryFactory().produce()),
        PersonSummaryInfoResult.Success.Full("crn2", CaseSummaryFactory().produce()),
        PersonSummaryInfoResult.NotFound("crn3"),
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
      null,
    )

    assertThat(results).hasSize(3)
    assertThat(results.dropLast(1)).allSatisfy {
      assertThat(it.personName).isNotNull()
    }
    assertThat(results.last().personName).isNull()
    assertThat(metaData).isNotNull()
    verify(exactly = 1) {
      mockBookingRepository.findBookings(any(), any(), any(), any(), any())
    }
  }

  @EnumSource(value = SortOrder::class)
  @ParameterizedTest
  fun `findBookings returns sorted results by person name and database default sort when page number is given`(sortOrder: SortOrder) {
    val pageSort = when (sortOrder) {
      SortOrder.ascending -> Sort.by("created_at").ascending()
      SortOrder.descending -> Sort.by("created_at").descending()
    }
    val pageable = PageRequest.of(0, cas3BookingSearchPageSize, pageSort)
    every { mockUserService.getUserForRequest() } returns UserEntityFactory()
      .withYieldedProbationRegion {
        ProbationRegionEntityFactory()
          .withYieldedApArea {
            ApAreaEntityFactory().produce()
          }
          .produce()
      }
      .produce()
    every { mockBookingRepository.findBookings(any(), any(), any(), any(), pageable) } returns PageImpl(
      listOf(
        TestBookingSearchResult().withPersonCrn("crn1"),
        TestBookingSearchResult().withPersonCrn("crn2"),
        TestBookingSearchResult().withPersonCrn("crn3"),
      ),
    )
    every { mockOffenderService.getOffenderSummariesByCrns(setOf("crn1", "crn2", "crn3"), any(), any()) } returns
      listOf(
        PersonSummaryInfoResult.Success.Full("crn1", CaseSummaryFactory().produce()),
        PersonSummaryInfoResult.Success.Full("crn2", CaseSummaryFactory().produce()),
        PersonSummaryInfoResult.Success.Full("crn3", CaseSummaryFactory().produce()),
      )

    val (results, metaData) = bookingSearchService.findBookings(
      ServiceName.temporaryAccommodation,
      null,
      sortOrder,
      BookingSearchSortField.personName,
      1,
      null,
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
      mockBookingRepository.findBookings(any(), any(), any(), any(), pageable)
    }
  }

  @EnumSource(value = SortOrder::class)
  @ParameterizedTest
  fun `findBookings returns sorted approved premises booking results by person name and database default sort when page number is given`(sortOrder: SortOrder) {
    val pageSort = when (sortOrder) {
      SortOrder.ascending -> Sort.by("created_at").ascending()
      SortOrder.descending -> Sort.by("created_at").descending()
    }
    val pageable = PageRequest.of(0, defaultBookingSearchPageSize, pageSort)
    every { mockUserService.getUserForRequest() } returns UserEntityFactory()
      .withYieldedProbationRegion {
        ProbationRegionEntityFactory()
          .withYieldedApArea {
            ApAreaEntityFactory().produce()
          }
          .produce()
      }
      .produce()
    every { mockBookingRepository.findBookings(any(), any(), any(), any(), pageable) } returns PageImpl(
      listOf(
        TestBookingSearchResult().withPersonCrn("crn1"),
        TestBookingSearchResult().withPersonCrn("crn2"),
        TestBookingSearchResult().withPersonCrn("crn3"),
      ),
    )
    every { mockOffenderService.getOffenderSummariesByCrns(setOf("crn1", "crn2", "crn3"), any(), any()) } returns
      listOf(
        PersonSummaryInfoResult.Success.Full("crn1", CaseSummaryFactory().produce()),
        PersonSummaryInfoResult.Success.Full("crn2", CaseSummaryFactory().produce()),
        PersonSummaryInfoResult.Success.Full("crn3", CaseSummaryFactory().produce()),
      )

    val (results, metaData) = bookingSearchService.findBookings(
      ServiceName.approvedPremises,
      null,
      sortOrder,
      BookingSearchSortField.personName,
      1,
      null,
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
      mockBookingRepository.findBookings(any(), any(), any(), any(), pageable)
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
    every { mockBookingRepository.findBookings(any(), any(), any(), any(), pageable) } returns PageImpl(
      listOf(
        TestBookingSearchResult().withPersonCrn("crn1"),
        TestBookingSearchResult().withPersonCrn("crn2"),
        TestBookingSearchResult().withPersonCrn("crn3"),
      ),
    )
    every { mockOffenderService.getOffenderSummariesByCrns(setOf("crn1", "crn2", "crn3"), any(), any()) } returns
      listOf(
        PersonSummaryInfoResult.Success.Full("crn1", CaseSummaryFactory().produce()),
        PersonSummaryInfoResult.Success.Full("crn2", CaseSummaryFactory().produce()),
        PersonSummaryInfoResult.Success.Full("crn3", CaseSummaryFactory().produce()),
      )

    val (results, metaData) = bookingSearchService.findBookings(
      ServiceName.temporaryAccommodation,
      null,
      sortOrder,
      BookingSearchSortField.personName,
      null,
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
      mockBookingRepository.findBookings(any(), any(), any(), any(), pageable)
    }
  }

  @EnumSource(value = SortOrder::class)
  @ParameterizedTest
  fun `findBookings returns results and database sorted by crn when page number is given`(sortOrder: SortOrder) {
    val pageSort = when (sortOrder) {
      SortOrder.ascending -> Sort.by("crn").ascending()
      SortOrder.descending -> Sort.by("crn").descending()
    }
    val pageable = PageRequest.of(1, cas3BookingSearchPageSize, pageSort)
    every { mockUserService.getUserForRequest() } returns UserEntityFactory()
      .withYieldedProbationRegion {
        ProbationRegionEntityFactory()
          .withYieldedApArea {
            ApAreaEntityFactory().produce()
          }
          .produce()
      }
      .produce()
    every { mockBookingRepository.findBookings(any(), any(), any(), any(), pageable) } returns PageImpl(
      listOf(
        TestBookingSearchResult().withPersonCrn("crn1"),
        TestBookingSearchResult().withPersonCrn("crn2"),
        TestBookingSearchResult().withPersonCrn("crn3"),
      ),
    )
    every { mockOffenderService.getOffenderSummariesByCrns(setOf("crn1", "crn2", "crn3"), any(), any()) } returns
      listOf(
        PersonSummaryInfoResult.Success.Full("crn1", CaseSummaryFactory().produce()),
        PersonSummaryInfoResult.Success.Full("crn2", CaseSummaryFactory().produce()),
        PersonSummaryInfoResult.Success.Full("crn3", CaseSummaryFactory().produce()),
      )

    val (results, metaData) = bookingSearchService.findBookings(
      ServiceName.temporaryAccommodation,
      null,
      sortOrder,
      BookingSearchSortField.personCrn,
      2,
      null,
    )

    assertThat(results).hasSize(3)
    assertThat(metaData).isNotNull()
    verify(exactly = 1) {
      mockBookingRepository.findBookings(any(), any(), any(), any(), pageable)
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
    every { mockBookingRepository.findBookings(any(), any(), any(), any(), any()) } returns PageImpl(emptyList())
    every { mockOffenderService.getOffenderSummariesByCrns(emptySet(), any(), any()) } returns emptyList()

    val (results, metaData) = bookingSearchService.findBookings(
      ServiceName.temporaryAccommodation,
      null,
      SortOrder.ascending,
      BookingSearchSortField.bookingCreatedAt,
      1,
      null,
    )

    assertThat(results).hasSize(0)
    assertThat(metaData).isNotNull()
    verify(exactly = 1) {
      mockBookingRepository.findBookings(any(), any(), any(), any(), any())
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
    every { mockBookingRepository.findBookings(any(), any(), any(), any(), any()) } returns PageImpl(emptyList())
    every { mockOffenderService.getOffenderSummariesByCrns(emptySet(), any(), any()) } returns emptyList()

    val (results, metaData) = bookingSearchService.findBookings(
      ServiceName.temporaryAccommodation,
      null,
      SortOrder.ascending,
      BookingSearchSortField.bookingCreatedAt,
      null,
      "S448160",
    )

    assertThat(results).hasSize(0)
    assertThat(metaData).isNull()
    verify(exactly = 1) {
      mockBookingRepository.findBookings(any(), any(), any(), any(), any())
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
    every { mockBookingRepository.findBookings(any(), any(), any(), any(), any()) }.throws(
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
        null,
      )
    }
    verify(exactly = 1) {
      mockBookingRepository.findBookings(any(), any(), any(), any(), any())
    }
    verify(exactly = 0) {
      mockOffenderService.getOffenderByCrn(any(), any())
    }
  }
}
