package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.unit.service.v2

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
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SortDirection
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3v2BookingRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.Cas3BookingSearchSortField
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.service.v2.Cas3v2BookingSearchService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.unit.service.TestCas3BookingSearchResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApAreaEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.CaseSummaryFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.NameFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ProbationRegionEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.UserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonSummaryInfoResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.OffenderService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.PaginationConfig
import java.time.OffsetDateTime

class Cas3v2BookingSearchServiceTest {
  private val mockCas3v2BookingRepository = mockk<Cas3v2BookingRepository>()
  private val mockOffenderService = mockk<OffenderService>()
  private val mockUserService = mockk<UserService>()
  private val cas3BookingSearchPageSize = 50

  private val cas3BookingSearchService = Cas3v2BookingSearchService(
    mockCas3v2BookingRepository,
    mockOffenderService,
    mockUserService,
    cas3BookingSearchPageSize,
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

    every { mockCas3v2BookingRepository.findBookings(any(), any(), any(), any()) } returns PageImpl(
      crns.map { TestCas3BookingSearchResult().withPersonCrn(it) },
    )

    every { mockOffenderService.getPersonSummaryInfoResults(crns, any()) } returns
      crns.map { PersonSummaryInfoResult.Success.Full(it, CaseSummaryFactory().produce()) }

    val (results, metaData) = cas3BookingSearchService.findBookings(
      null,
      SortDirection.asc,
      Cas3BookingSearchSortField.BOOKING_CREATED_AT,
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
  fun `findBookings finds out bookings where the user is not authorised to get offender details for a particular CRN and gives them the name Limited Access Offender`() {
    every { mockUserService.getUserForRequest() } returns UserEntityFactory()
      .withYieldedProbationRegion {
        ProbationRegionEntityFactory()
          .withYieldedApArea {
            ApAreaEntityFactory().produce()
          }
          .produce()
      }
      .produce()
    every { mockCas3v2BookingRepository.findBookings(any(), any(), any(), any()) } returns PageImpl(
      listOf(
        TestCas3BookingSearchResult()
          .withPersonCrn("crn1")
          .withBookingCreatedAt(OffsetDateTime.now().minusDays(3)),
        TestCas3BookingSearchResult()
          .withPersonCrn("crn2")
          .withBookingCreatedAt(OffsetDateTime.now().minusDays(2)),
        TestCas3BookingSearchResult()
          .withPersonCrn("crn3")
          .withBookingCreatedAt(OffsetDateTime.now().minusDays(1)),
      ),
    )
    every { mockOffenderService.getPersonSummaryInfoResults(setOf("crn1", "crn2", "crn3"), any()) } returns
      listOf(
        PersonSummaryInfoResult.Success.Full("crn1", CaseSummaryFactory().withName(NameFactory().withForename("Gregor").withSurname("Samsa").produce()).produce()),
        PersonSummaryInfoResult.Success.Full("crn2", CaseSummaryFactory().withName(NameFactory().withForename("Franz").withSurname("Kafka").produce()).produce()),
        PersonSummaryInfoResult.Success.Restricted("crn3", "crn3noms"),
      )

    val (results, metadata) = cas3BookingSearchService.findBookings(
      null,
      SortDirection.asc,
      Cas3BookingSearchSortField.BOOKING_CREATED_AT,
      1,
      null,
    )
    assertThat(results).hasSize(3)
    assertThat(results).matches { result ->
      result.map { it.personName }.toSet() == setOf("Gregor Samsa", "Franz Kafka", "Limited Access Offender")
    }
    assertThat(metadata).isNotNull()
    verify(exactly = 1) {
      mockCas3v2BookingRepository.findBookings(any(), any(), any(), any())
    }
  }

  @Test
  fun `findBookings provides Unknown for a person's name when offender details for a particular CRN could not be found`() {
    every { mockUserService.getUserForRequest() } returns UserEntityFactory()
      .withYieldedProbationRegion {
        ProbationRegionEntityFactory()
          .withYieldedApArea {
            ApAreaEntityFactory().produce()
          }
          .produce()
      }
      .produce()
    every { mockCas3v2BookingRepository.findBookings(any(), any(), any(), any()) } returns PageImpl(
      listOf(
        TestCas3BookingSearchResult()
          .withPersonCrn("crn1")
          .withBookingCreatedAt(OffsetDateTime.now().minusDays(3)),
        TestCas3BookingSearchResult()
          .withPersonCrn("crn2")
          .withBookingCreatedAt(OffsetDateTime.now().minusDays(2)),
        TestCas3BookingSearchResult()
          .withPersonCrn("crn3")
          .withBookingCreatedAt(OffsetDateTime.now().minusDays(1)),
      ),
    )
    every { mockOffenderService.getPersonSummaryInfoResults(setOf("crn1", "crn2", "crn3"), any()) } returns
      listOf(
        PersonSummaryInfoResult.Success.Full("crn1", CaseSummaryFactory().produce()),
        PersonSummaryInfoResult.Success.Full("crn2", CaseSummaryFactory().produce()),
        PersonSummaryInfoResult.NotFound("crn3"),
      )

    val (results, metaData) = cas3BookingSearchService.findBookings(
      null,
      SortDirection.asc,
      Cas3BookingSearchSortField.BOOKING_CREATED_AT,
      1,
      null,
    )

    assertThat(results).hasSize(3)
    assertThat(results).allSatisfy {
      assertThat(it.personName).isNotNull()
    }
    assertThat(results.last().personName).isEqualTo("Unknown")
    assertThat(metaData).isNotNull()
    verify(exactly = 1) {
      mockCas3v2BookingRepository.findBookings(any(), any(), any(), any())
    }
  }

  @EnumSource(value = SortDirection::class)
  @ParameterizedTest
  fun `findBookings returns sorted results by person name and database default sort when page number is given`(sortDirection: SortDirection) {
    val pageSort = when (sortDirection) {
      SortDirection.asc -> Sort.by("personName").ascending()
      SortDirection.desc -> Sort.by("personName").descending()
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
    every { mockCas3v2BookingRepository.findBookings(any(), any(), any(), pageable) } returns PageImpl(
      listOf(
        TestCas3BookingSearchResult().withPersonCrn("crn1"),
        TestCas3BookingSearchResult().withPersonCrn("crn2"),
        TestCas3BookingSearchResult().withPersonCrn("crn3"),
      ),
    )
    every { mockOffenderService.getPersonSummaryInfoResults(setOf("crn1", "crn2", "crn3"), any()) } returns
      listOf(
        PersonSummaryInfoResult.Success.Full("crn1", CaseSummaryFactory().produce()),
        PersonSummaryInfoResult.Success.Full("crn2", CaseSummaryFactory().produce()),
        PersonSummaryInfoResult.Success.Full("crn3", CaseSummaryFactory().produce()),
      )

    val (results, metaData) = cas3BookingSearchService.findBookings(
      null,
      sortDirection,
      Cas3BookingSearchSortField.PERSON_NAME,
      1,
      null,
    )

    assertThat(results).hasSize(3)
    assertThat(results.map { it.personName }).isSortedAccordingTo { a, b ->
      when (sortDirection) {
        SortDirection.asc -> compareValues(a, b)
        SortDirection.desc -> compareValues(b, a)
      }
    }
    assertThat(metaData).isNotNull()
    verify(exactly = 1) {
      mockCas3v2BookingRepository.findBookings(any(), any(), any(), pageable)
    }
  }

  @EnumSource(value = SortDirection::class)
  @ParameterizedTest
  fun `findBookings returns sorted results by person name and database default sort when page number is not given`(sortDirection: SortDirection) {
    val pageSort = when (sortDirection) {
      SortDirection.asc -> Sort.by("personName").ascending()
      SortDirection.desc -> Sort.by("personName").descending()
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
    every { mockCas3v2BookingRepository.findBookings(any(), any(), any(), pageable) } returns PageImpl(
      listOf(
        TestCas3BookingSearchResult().withPersonCrn("crn1"),
        TestCas3BookingSearchResult().withPersonCrn("crn2"),
        TestCas3BookingSearchResult().withPersonCrn("crn3"),
      ),
    )
    every { mockOffenderService.getPersonSummaryInfoResults(setOf("crn1", "crn2", "crn3"), any()) } returns
      listOf(
        PersonSummaryInfoResult.Success.Full("crn1", CaseSummaryFactory().produce()),
        PersonSummaryInfoResult.Success.Full("crn2", CaseSummaryFactory().produce()),
        PersonSummaryInfoResult.Success.Full("crn3", CaseSummaryFactory().produce()),
      )

    val (results, metaData) = cas3BookingSearchService.findBookings(
      null,
      sortDirection,
      Cas3BookingSearchSortField.PERSON_NAME,
      null,
      null,
    )

    assertThat(results).hasSize(3)
    assertThat(results.map { it.personName }).isSortedAccordingTo { a, b ->
      when (sortDirection) {
        SortDirection.asc -> compareValues(a, b)
        SortDirection.desc -> compareValues(b, a)
      }
    }
    assertThat(metaData).isNull()
    verify(exactly = 1) {
      mockCas3v2BookingRepository.findBookings(any(), any(), any(), pageable)
    }
  }

  @EnumSource(value = SortDirection::class)
  @ParameterizedTest
  fun `findBookings returns results and database sorted by crn when page number is given`(sortDirection: SortDirection) {
    val pageSort = when (sortDirection) {
      SortDirection.asc -> Sort.by("crn").ascending()
      SortDirection.desc -> Sort.by("crn").descending()
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
    every { mockCas3v2BookingRepository.findBookings(any(), any(), any(), pageable) } returns PageImpl(
      listOf(
        TestCas3BookingSearchResult().withPersonCrn("crn1"),
        TestCas3BookingSearchResult().withPersonCrn("crn2"),
        TestCas3BookingSearchResult().withPersonCrn("crn3"),
      ),
    )
    every { mockOffenderService.getPersonSummaryInfoResults(setOf("crn1", "crn2", "crn3"), any()) } returns
      listOf(
        PersonSummaryInfoResult.Success.Full("crn1", CaseSummaryFactory().produce()),
        PersonSummaryInfoResult.Success.Full("crn2", CaseSummaryFactory().produce()),
        PersonSummaryInfoResult.Success.Full("crn3", CaseSummaryFactory().produce()),
      )

    val (results, metaData) = cas3BookingSearchService.findBookings(
      null,
      sortDirection,
      Cas3BookingSearchSortField.PERSON_CRN,
      2,
      null,
    )

    assertThat(results).hasSize(3)
    assertThat(metaData).isNotNull()
    verify(exactly = 1) {
      mockCas3v2BookingRepository.findBookings(any(), any(), any(), pageable)
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
    every { mockCas3v2BookingRepository.findBookings(any(), any(), any(), any()) } returns PageImpl(emptyList())
    every { mockOffenderService.getPersonSummaryInfoResults(emptySet(), any()) } returns emptyList()

    val (results, metaData) = cas3BookingSearchService.findBookings(
      null,
      SortDirection.asc,
      Cas3BookingSearchSortField.BOOKING_CREATED_AT,
      1,
      null,
    )

    assertThat(results).hasSize(0)
    assertThat(metaData).isNotNull()
    verify(exactly = 1) {
      mockCas3v2BookingRepository.findBookings(any(), any(), any(), any())
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
    every { mockCas3v2BookingRepository.findBookings(any(), any(), any(), any()) } returns PageImpl(emptyList())
    every { mockOffenderService.getPersonSummaryInfoResults(emptySet(), any()) } returns emptyList()

    val (results, metaData) = cas3BookingSearchService.findBookings(
      null,
      SortDirection.asc,
      Cas3BookingSearchSortField.BOOKING_CREATED_AT,
      null,
      "S448160",
    )

    assertThat(results).hasSize(0)
    assertThat(metaData).isNull()
    verify(exactly = 1) {
      mockCas3v2BookingRepository.findBookings(any(), any(), any(), any())
    }
  }

  @Test
  fun `throw exception when DB exception happened`() {
    every { mockUserService.getUserForRequest() } returns UserEntityFactory()
      .withYieldedProbationRegion {
        ProbationRegionEntityFactory()
          .withYieldedApArea {
            ApAreaEntityFactory().produce()
          }
          .produce()
      }
      .produce()
    every { mockCas3v2BookingRepository.findBookings(any(), any(), any(), any()) }.throws(
      DataRetrievalFailureException(
        "some-exception",
      ),
    )

    Assertions.assertThrows(DataRetrievalFailureException::class.java) {
      cas3BookingSearchService.findBookings(
        null,
        SortDirection.asc,
        Cas3BookingSearchSortField.BOOKING_CREATED_AT,
        1,
        null,
      )
    }
    verify(exactly = 1) {
      mockCas3v2BookingRepository.findBookings(any(), any(), any(), any())
    }
  }
}
