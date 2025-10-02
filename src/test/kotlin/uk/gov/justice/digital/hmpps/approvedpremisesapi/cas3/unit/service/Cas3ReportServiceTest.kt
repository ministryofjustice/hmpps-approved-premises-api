package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.unit.service

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3BookingGapReportRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3FutureBookingsReportRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3VoidBedspacesRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3v2BookingRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.reporting.generator.TransitionalAccommodationReferralReportGenerator
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.reporting.model.TransitionalAccommodationReferralReportDataAndPersonInfo
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.reporting.properties.BookingsReportProperties
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.reporting.properties.TransitionalAccommodationReferralReportProperties
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.repository.BedUsageRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.repository.BedUtilisationReportRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.repository.BedspaceOccupancyReportRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.repository.BookingsReportData
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.repository.BookingsReportRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.repository.TransitionalAccommodationReferralReportData
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.repository.TransitionalAccommodationReferralReportRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.service.Cas3ReportService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.transformer.Cas3BookingTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.CaseSummaryFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.UserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BookingRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonSummaryInfoResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.FeatureFlagService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.OffenderService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.WorkingDayService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.BookingTransformer
import java.io.ByteArrayOutputStream
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

class Cas3ReportServiceTest {
  private val mockOffenderService = mockk<OffenderService>()
  private val mockUserService = mockk<UserService>()
  private val mockTransitionalAccommodationReferralReportRowRepository =
    mockk<TransitionalAccommodationReferralReportRepository>()
  private val mockBookingsReportRepository = mockk<BookingsReportRepository>()
  private val mockLostBedsRepository = mockk<Cas3VoidBedspacesRepository>()
  private val mockBookingTransformer = mockk<BookingTransformer>()
  private val mockCas3BookingTransformer = mockk<Cas3BookingTransformer>()
  private val mockWorkingDayService = mockk<WorkingDayService>()
  private val mockFeatureFlagService = mockk<FeatureFlagService>()
  private val mockBookingRepository = mockk<BookingRepository>()
  private val mockCas3v2BookingRepository = mockk<Cas3v2BookingRepository>()
  private val mockBedUsageRepository = mockk<BedUsageRepository>()
  private val mockBedUtilisationReportRepository = mockk<BedUtilisationReportRepository>()
  private val mockBedspaceOccupancyReportRepository = mockk<BedspaceOccupancyReportRepository>()
  private val mockCas3BookingGapReportRepository = mockk<Cas3BookingGapReportRepository>()
  private val mockCas3FutureBookingsReportRepository = mockk<Cas3FutureBookingsReportRepository>()

  private val cas3ReportService = Cas3ReportService(
    mockOffenderService,
    mockUserService,
    mockTransitionalAccommodationReferralReportRowRepository,
    mockBookingsReportRepository,
    mockLostBedsRepository,
    mockBookingTransformer,
    mockCas3BookingTransformer,
    mockWorkingDayService,
    mockFeatureFlagService,
    mockBookingRepository,
    mockCas3v2BookingRepository,
    mockBedUsageRepository,
    mockBedUtilisationReportRepository,
    mockBedspaceOccupancyReportRepository,
    mockCas3FutureBookingsReportRepository,
    mockCas3BookingGapReportRepository,
    2,
  )

  @ParameterizedTest
  @ValueSource(booleans = [true, false])
  fun `createCas3ApplicationReferralsReport successfully generate report with required information`(reportsWithNewBedspaceModelTablesEnabled: Boolean) {
    val startDate = LocalDate.of(2024, 1, 1)
    val endDate = LocalDate.of(2024, 1, 31)
    val probationRegionId = UUID.randomUUID()
    val testTransitionalAccommodationReferralReportData = createDBReferralReportData()
    val properties = TransitionalAccommodationReferralReportProperties(probationRegionId, startDate, endDate)

    every { mockFeatureFlagService.getBooleanFlag(eq("cas3-reports-with-new-bedspace-model-tables-enabled")) } returns reportsWithNewBedspaceModelTablesEnabled
    if (reportsWithNewBedspaceModelTablesEnabled) {
      every {
        mockTransitionalAccommodationReferralReportRowRepository.findAllReferralsV2(
          any(),
          any(),
          any(),
        )
      } returns listOf(testTransitionalAccommodationReferralReportData)
    } else {
      every {
        mockTransitionalAccommodationReferralReportRowRepository.findAllReferrals(
          any(),
          any(),
          any(),
        )
      } returns listOf(testTransitionalAccommodationReferralReportData)
    }

    every { mockUserService.getUserForRequest() } returns UserEntityFactory().withUnitTestControlProbationRegion().produce()
    every { mockOffenderService.getPersonSummaryInfoResultsInBatches(any<Set<String>>(), any(), batchSize = 2) } returns listOf(
      PersonSummaryInfoResult.Success.Full("", CaseSummaryFactory().produce()),
    )

    cas3ReportService.createCas3ApplicationReferralsReport(properties, ByteArrayOutputStream())

    if (reportsWithNewBedspaceModelTablesEnabled) {
      verify {
        mockTransitionalAccommodationReferralReportRowRepository.findAllReferralsV2(
          startDate,
          endDate,
          probationRegionId,
        )
      }
    } else {
      verify {
        mockTransitionalAccommodationReferralReportRowRepository.findAllReferrals(
          startDate,
          endDate,
          probationRegionId,
        )
      }
    }
    verify { mockUserService.getUserForRequest() }
    verify(exactly = 1) { mockOffenderService.getPersonSummaryInfoResultsInBatches(any<Set<String>>(), any(), batchSize = 2) }
  }

  @ParameterizedTest
  @ValueSource(booleans = [true, false])
  fun `createCas3ApplicationReferralsReport successfully generate report for 3 months`(reportsWithNewBedspaceModelTablesEnabled: Boolean) {
    val startDate = LocalDate.of(2024, 1, 1)
    val endDate = LocalDate.of(2024, 4, 1)
    val probationRegionId = UUID.randomUUID()
    val testTransitionalAccommodationReferralReportData = createDBReferralReportData()
    val properties = TransitionalAccommodationReferralReportProperties(probationRegionId, startDate, endDate)

    every { mockFeatureFlagService.getBooleanFlag(eq("cas3-reports-with-new-bedspace-model-tables-enabled")) } returns reportsWithNewBedspaceModelTablesEnabled

    if (reportsWithNewBedspaceModelTablesEnabled) {
      every {
        mockTransitionalAccommodationReferralReportRowRepository.findAllReferralsV2(
          any(),
          any(),
          any(),
        )
      } returns listOf(testTransitionalAccommodationReferralReportData)
    } else {
      every {
        mockTransitionalAccommodationReferralReportRowRepository.findAllReferrals(
          any(),
          any(),
          any(),
        )
      } returns listOf(testTransitionalAccommodationReferralReportData)
    }
    every { mockUserService.getUserForRequest() } returns UserEntityFactory().withUnitTestControlProbationRegion().produce()
    every { mockOffenderService.getPersonSummaryInfoResultsInBatches(any<Set<String>>(), any(), batchSize = 3) } returns listOf(
      PersonSummaryInfoResult.Success.Full("", CaseSummaryFactory().produce()),
    )

    val cas3ReportServiceWithThreeMonths = Cas3ReportService(
      mockOffenderService,
      mockUserService,
      mockTransitionalAccommodationReferralReportRowRepository,
      mockBookingsReportRepository,
      mockLostBedsRepository,
      mockBookingTransformer,
      mockCas3BookingTransformer,
      mockWorkingDayService,
      mockFeatureFlagService,
      mockBookingRepository,
      mockCas3v2BookingRepository,
      mockBedUsageRepository,
      mockBedUtilisationReportRepository,
      mockBedspaceOccupancyReportRepository,
      mockCas3FutureBookingsReportRepository,
      mockCas3BookingGapReportRepository,
      3,
    )

    cas3ReportServiceWithThreeMonths.createCas3ApplicationReferralsReport(properties, ByteArrayOutputStream())

    if (reportsWithNewBedspaceModelTablesEnabled) {
      verify {
        mockTransitionalAccommodationReferralReportRowRepository.findAllReferralsV2(
          startDate,
          endDate,
          probationRegionId,
        )
      }
    } else {
      verify {
        mockTransitionalAccommodationReferralReportRowRepository.findAllReferrals(
          startDate,
          endDate,
          probationRegionId,
        )
      }
    }
    verify { mockUserService.getUserForRequest() }
    verify(exactly = 1) { mockOffenderService.getPersonSummaryInfoResultsInBatches(any<Set<String>>(), any(), batchSize = 3) }
  }

  @ParameterizedTest
  @ValueSource(booleans = [true, false])
  fun `createCas3ApplicationReferralsReport successfully generate empty report`(reportsWithNewBedspaceModelTablesEnabled: Boolean) {
    val startDate = LocalDate.of(2024, 1, 1)
    val endDate = LocalDate.of(2024, 1, 31)
    val probationRegionId = UUID.randomUUID()
    val properties = TransitionalAccommodationReferralReportProperties(probationRegionId, startDate, endDate)

    every { mockFeatureFlagService.getBooleanFlag(eq("cas3-reports-with-new-bedspace-model-tables-enabled")) } returns reportsWithNewBedspaceModelTablesEnabled

    if (reportsWithNewBedspaceModelTablesEnabled) {
      every {
        mockTransitionalAccommodationReferralReportRowRepository.findAllReferralsV2(
          any(),
          any(),
          any(),
        )
      } returns emptyList()
    } else {
      every {
        mockTransitionalAccommodationReferralReportRowRepository.findAllReferrals(
          any(),
          any(),
          any(),
        )
      } returns emptyList()
    }
    every { mockUserService.getUserForRequest() } returns UserEntityFactory().withUnitTestControlProbationRegion().produce()
    every { mockOffenderService.getPersonSummaryInfoResultsInBatches(any<Set<String>>(), any(), batchSize = 2) } returns emptyList()

    cas3ReportService.createCas3ApplicationReferralsReport(properties, ByteArrayOutputStream())

    if (reportsWithNewBedspaceModelTablesEnabled) {
      verify {
        mockTransitionalAccommodationReferralReportRowRepository.findAllReferralsV2(
          startDate,
          endDate,
          probationRegionId,
        )
      }
    } else {
      verify {
        mockTransitionalAccommodationReferralReportRowRepository.findAllReferrals(
          startDate,
          endDate,
          probationRegionId,
        )
      }
    }
    verify(exactly = 1) { mockUserService.getUserForRequest() }
    verify(exactly = 0) { mockOffenderService.getPersonSummaryInfoResults(any<Set<String>>(), any()) }
  }

  @ParameterizedTest
  @ValueSource(booleans = [true, false])
  fun `createCas3ApplicationReferralsReport includes previousReferralProbationRegionName and previousReferralPduName when present`(reportsWithNewBedspaceModelTablesEnabled: Boolean) {
    val startDate = LocalDate.of(2024, 1, 1)
    val endDate = LocalDate.of(2024, 1, 31)
    val probationRegionId = UUID.randomUUID()
    val testTransitionalAccommodationReferralReportData = createDBReferralReportDataWithPreviousReferralInfo(
      "crn-with-previous",
      "Previous Region Name",
      "Previous PDU Name",
    )
    val properties = TransitionalAccommodationReferralReportProperties(probationRegionId, startDate, endDate)

    every { mockFeatureFlagService.getBooleanFlag(eq("cas3-reports-with-new-bedspace-model-tables-enabled")) } returns reportsWithNewBedspaceModelTablesEnabled
    if (reportsWithNewBedspaceModelTablesEnabled) {
      every {
        mockTransitionalAccommodationReferralReportRowRepository.findAllReferralsV2(
          any(),
          any(),
          any(),
        )
      } returns listOf(testTransitionalAccommodationReferralReportData)
    } else {
      every {
        mockTransitionalAccommodationReferralReportRowRepository.findAllReferrals(
          any(),
          any(),
          any(),
        )
      } returns listOf(testTransitionalAccommodationReferralReportData)
    }
    every { mockUserService.getUserForRequest() } returns UserEntityFactory().withUnitTestControlProbationRegion().produce()
    every { mockOffenderService.getPersonSummaryInfoResultsInBatches(any<Set<String>>(), any(), batchSize = 2) } returns listOf(
      PersonSummaryInfoResult.Success.Full("crn-with-previous", CaseSummaryFactory().produce()),
    )

    val outputStream = ByteArrayOutputStream()
    cas3ReportService.createCas3ApplicationReferralsReport(properties, outputStream)

    val reportDataAndPersonInfo = TransitionalAccommodationReferralReportDataAndPersonInfo(
      testTransitionalAccommodationReferralReportData,
      PersonSummaryInfoResult.Success.Full("crn-with-previous", CaseSummaryFactory().produce()),
    )
    val reportGenerator = TransitionalAccommodationReferralReportGenerator()
    val dataFrame = reportGenerator.createReport(listOf(reportDataAndPersonInfo), properties)

    val columnNames = dataFrame.columnNames()
    Assertions.assertThat(columnNames).contains("destinationRegion")
    Assertions.assertThat(columnNames).contains("destinationPdu")
    Assertions.assertThat(columnNames).contains("outOfRegion")

    Assertions.assertThat(dataFrame.rowsCount()).isEqualTo(1)
    val row = dataFrame[0]
    Assertions.assertThat(row["probationRegion"]).isEqualTo("Previous Region Name")
    Assertions.assertThat(row["pdu"]).isEqualTo("Previous PDU Name")
    Assertions.assertThat(row["destinationRegion"]).isEqualTo("region")
    Assertions.assertThat(row["destinationPdu"]).isEqualTo("pduName")
    Assertions.assertThat(row["outOfRegion"]).isEqualTo("Yes")

    if (reportsWithNewBedspaceModelTablesEnabled) {
      verify {
        mockTransitionalAccommodationReferralReportRowRepository.findAllReferralsV2(
          startDate,
          endDate,
          probationRegionId,
        )
      }
    } else {
      verify {
        mockTransitionalAccommodationReferralReportRowRepository.findAllReferrals(
          startDate,
          endDate,
          probationRegionId,
        )
      }
    }
    verify { mockUserService.getUserForRequest() }
    verify(exactly = 1) { mockOffenderService.getPersonSummaryInfoResultsInBatches(any<Set<String>>(), any(), batchSize = 2) }
  }

  @ParameterizedTest
  @ValueSource(booleans = [true, false])
  fun `createCas3ApplicationReferralsReport handles null previousReferralProbationRegionName and previousReferralPduName`(reportsWithNewBedspaceModelTablesEnabled: Boolean) {
    val startDate = LocalDate.of(2024, 1, 1)
    val endDate = LocalDate.of(2024, 1, 31)
    val probationRegionId = UUID.randomUUID()
    val testTransitionalAccommodationReferralReportData = createDBReferralReportDataWithPreviousReferralInfo(
      "crn-without-previous",
      null,
      null,
    )
    val properties = TransitionalAccommodationReferralReportProperties(probationRegionId, startDate, endDate)

    every { mockFeatureFlagService.getBooleanFlag(eq("cas3-reports-with-new-bedspace-model-tables-enabled")) } returns reportsWithNewBedspaceModelTablesEnabled
    if (reportsWithNewBedspaceModelTablesEnabled) {
      every {
        mockTransitionalAccommodationReferralReportRowRepository.findAllReferralsV2(
          any(),
          any(),
          any(),
        )
      } returns listOf(testTransitionalAccommodationReferralReportData)
    } else {
      every {
        mockTransitionalAccommodationReferralReportRowRepository.findAllReferrals(
          any(),
          any(),
          any(),
        )
      } returns listOf(testTransitionalAccommodationReferralReportData)
    }
    every { mockUserService.getUserForRequest() } returns UserEntityFactory().withUnitTestControlProbationRegion().produce()
    every { mockOffenderService.getPersonSummaryInfoResultsInBatches(any<Set<String>>(), any(), batchSize = 2) } returns listOf(
      PersonSummaryInfoResult.Success.Full("crn-without-previous", CaseSummaryFactory().produce()),
    )

    val outputStream = ByteArrayOutputStream()
    cas3ReportService.createCas3ApplicationReferralsReport(properties, outputStream)

    val reportDataAndPersonInfo = TransitionalAccommodationReferralReportDataAndPersonInfo(
      testTransitionalAccommodationReferralReportData,
      PersonSummaryInfoResult.Success.Full("crn-without-previous", CaseSummaryFactory().produce()),
    )
    val reportGenerator = TransitionalAccommodationReferralReportGenerator()
    val dataFrame = reportGenerator.createReport(listOf(reportDataAndPersonInfo), properties)

    val columnNames = dataFrame.columnNames()
    Assertions.assertThat(columnNames).contains("destinationRegion")
    Assertions.assertThat(columnNames).contains("destinationPdu")
    Assertions.assertThat(columnNames).contains("outOfRegion")

    Assertions.assertThat(dataFrame.rowsCount()).isEqualTo(1)
    val row = dataFrame[0]
    Assertions.assertThat(row["probationRegion"]).isEqualTo("region")
    Assertions.assertThat(row["pdu"]).isEqualTo("pduName")
    Assertions.assertThat(row["destinationRegion"]).isNull()
    Assertions.assertThat(row["destinationPdu"]).isNull()
    Assertions.assertThat(row["outOfRegion"]).isEqualTo("No")

    if (reportsWithNewBedspaceModelTablesEnabled) {
      verify {
        mockTransitionalAccommodationReferralReportRowRepository.findAllReferralsV2(
          startDate,
          endDate,
          probationRegionId,
        )
      }
    } else {
      mockTransitionalAccommodationReferralReportRowRepository.findAllReferrals(
        startDate,
        endDate,
        probationRegionId,
      )
    }
    verify { mockUserService.getUserForRequest() }
    verify(exactly = 1) { mockOffenderService.getPersonSummaryInfoResultsInBatches(any<Set<String>>(), any(), batchSize = 2) }
  }

  @ParameterizedTest
  @ValueSource(booleans = [true, false])
  fun `createCas3ApplicationReferralsReport successfully generate report fail when offender service call fails`(reportsWithNewBedspaceModelTablesEnabled: Boolean) {
    val startDate = LocalDate.of(2024, 1, 1)
    val endDate = LocalDate.of(2024, 1, 31)
    val probationRegionId = UUID.randomUUID()
    val properties = TransitionalAccommodationReferralReportProperties(probationRegionId, startDate, endDate)

    every { mockFeatureFlagService.getBooleanFlag(eq("cas3-reports-with-new-bedspace-model-tables-enabled")) } returns reportsWithNewBedspaceModelTablesEnabled
    if (reportsWithNewBedspaceModelTablesEnabled) {
      every {
        mockTransitionalAccommodationReferralReportRowRepository.findAllReferralsV2(
          any(),
          any(),
          any(),
        )
      } returns listOf(
        createDBReferralReportData("crn1"),
        createDBReferralReportData("crn2"),
        createDBReferralReportData("crn3"),
      )
    } else {
      every {
        mockTransitionalAccommodationReferralReportRowRepository.findAllReferrals(
          any(),
          any(),
          any(),
        )
      } returns listOf(
        createDBReferralReportData("crn1"),
        createDBReferralReportData("crn2"),
        createDBReferralReportData("crn3"),
      )
    }
    every { mockUserService.getUserForRequest() } returns UserEntityFactory().withUnitTestControlProbationRegion().produce()
    every { mockOffenderService.getPersonSummaryInfoResultsInBatches(setOf("crn1", "crn2", "crn3"), any(), batchSize = 2) } throws RuntimeException("some exception")

    Assertions.assertThatExceptionOfType(RuntimeException::class.java)
      .isThrownBy {
        cas3ReportService.createCas3ApplicationReferralsReport(properties, ByteArrayOutputStream())
      }

    if (reportsWithNewBedspaceModelTablesEnabled) {
      verify {
        mockTransitionalAccommodationReferralReportRowRepository.findAllReferralsV2(
          startDate,
          endDate,
          probationRegionId,
        )
      }
    } else {
      verify {
        mockTransitionalAccommodationReferralReportRowRepository.findAllReferrals(
          startDate,
          endDate,
          probationRegionId,
        )
      }
    }
    verify { mockUserService.getUserForRequest() }
    verify(exactly = 1) { mockOffenderService.getPersonSummaryInfoResultsInBatches(any<Set<String>>(), any(), batchSize = 2) }
  }

  @ParameterizedTest
  @ValueSource(booleans = [true, false])
  fun `createBookingsReport successfully generate report with required information`(reportsWithNewBedspaceModelTablesEnabled: Boolean) {
    val crn = "P131431"
    val startDate = LocalDate.of(2024, 1, 1)
    val endDate = LocalDate.of(2024, 1, 31)
    val probationRegionId = UUID.randomUUID()
    val bookingsReportData = createBookingReportData(crn)
    val properties = BookingsReportProperties(ServiceName.temporaryAccommodation, probationRegionId, startDate, endDate)

    every { mockFeatureFlagService.getBooleanFlag(eq("cas3-reports-with-new-bedspace-model-tables-enabled")) } returns reportsWithNewBedspaceModelTablesEnabled
    if (reportsWithNewBedspaceModelTablesEnabled) {
      every {
        mockBookingsReportRepository.findAllByOverlappingDateV2(
          startDate,
          endDate,
          probationRegionId,
        )
      } returns listOf(bookingsReportData)
    } else {
      every {
        mockBookingsReportRepository.findAllByOverlappingDate(
          startDate,
          endDate,
          ServiceName.temporaryAccommodation.value,
          probationRegionId,
        )
      } returns listOf(bookingsReportData)
    }
    every { mockUserService.getUserForRequest() } returns UserEntityFactory().withUnitTestControlProbationRegion().produce()
    every { mockOffenderService.getPersonSummaryInfoResultsInBatches(any<Set<String>>(), any(), batchSize = 2) } returns listOf(
      PersonSummaryInfoResult.Success.Full(crn, CaseSummaryFactory().produce()),
    )

    cas3ReportService.createBookingsReport(properties, ByteArrayOutputStream())

    if (reportsWithNewBedspaceModelTablesEnabled) {
      verify {
        mockBookingsReportRepository.findAllByOverlappingDateV2(
          startDate,
          endDate,
          probationRegionId,
        )
      }
    } else {
      verify {
        mockBookingsReportRepository.findAllByOverlappingDate(
          startDate,
          endDate,
          ServiceName.temporaryAccommodation.value,
          probationRegionId,
        )
      }
    }
    verify { mockUserService.getUserForRequest() }
    verify(exactly = 1) { mockOffenderService.getPersonSummaryInfoResultsInBatches(any<Set<String>>(), any(), batchSize = 2) }
  }

  @ParameterizedTest
  @ValueSource(booleans = [true, false])
  fun `createBookingsReport successfully generate report with required information, multiple crns`(reportsWithNewBedspaceModelTablesEnabled: Boolean) {
    val crns = listOf("P131431", "P131432", "P131433")
    val startDate = LocalDate.of(2024, 1, 1)
    val endDate = LocalDate.of(2024, 1, 31)
    val probationRegionId = UUID.randomUUID()
    val properties = BookingsReportProperties(ServiceName.temporaryAccommodation, probationRegionId, startDate, endDate)

    every { mockFeatureFlagService.getBooleanFlag(eq("cas3-reports-with-new-bedspace-model-tables-enabled")) } returns reportsWithNewBedspaceModelTablesEnabled
    if (reportsWithNewBedspaceModelTablesEnabled) {
      every {
        mockBookingsReportRepository.findAllByOverlappingDateV2(
          startDate,
          endDate,
          probationRegionId,
        )
      } returns listOf(
        createBookingReportData(crns[0]),
        createBookingReportData(crns[1]),
        createBookingReportData(crns[2]),
      )
    } else {
      every {
        mockBookingsReportRepository.findAllByOverlappingDate(
          startDate,
          endDate,
          ServiceName.temporaryAccommodation.value,
          probationRegionId,
        )
      } returns listOf(
        createBookingReportData(crns[0]),
        createBookingReportData(crns[1]),
        createBookingReportData(crns[2]),
      )
    }
    every { mockUserService.getUserForRequest() } returns UserEntityFactory().withUnitTestControlProbationRegion().produce()
    every { mockOffenderService.getPersonSummaryInfoResultsInBatches(any<Set<String>>(), any(), batchSize = 2) } returns listOf(
      PersonSummaryInfoResult.Success.Full(crns[0], CaseSummaryFactory().produce()),
      PersonSummaryInfoResult.Success.Full(crns[1], CaseSummaryFactory().produce()),
      PersonSummaryInfoResult.Success.Full(crns[2], CaseSummaryFactory().produce()),
    )

    cas3ReportService.createBookingsReport(properties, ByteArrayOutputStream())

    if (reportsWithNewBedspaceModelTablesEnabled) {
      verify {
        mockBookingsReportRepository.findAllByOverlappingDateV2(
          startDate,
          endDate,
          probationRegionId,
        )
      }
    } else {
      verify {
        mockBookingsReportRepository.findAllByOverlappingDate(
          startDate,
          endDate,
          ServiceName.temporaryAccommodation.value,
          probationRegionId,
        )
      }
    }
    verify { mockUserService.getUserForRequest() }
    verify(exactly = 1) { mockOffenderService.getPersonSummaryInfoResultsInBatches(any<Set<String>>(), any(), batchSize = 2) }
  }

  private fun createDBReferralReportData(): TestTransitionalAccommodationReferralReportData = createDBReferralReportData("crn")
  private fun createDBReferralReportData(crn: String) = TestTransitionalAccommodationReferralReportData(
    UUID.randomUUID().toString(), UUID.randomUUID().toString(), UUID.randomUUID().toString(),
    Instant.now(), crn, Instant.now(), "riskOfSeriousHarm",
    registeredSexOffender = false,
    historyOfSexualOffence = false,
    concerningSexualBehaviour = true,
    needForAccessibleProperty = true,
    historyOfArsonOffence = false,
    concerningArsonBehaviour = true,
    dutyToReferMade = true,
    dateDutyToReferMade = LocalDate.now(),
    probationRegionName = "region",
    dutyToReferLocalAuthorityAreaName = "name",
    dutyToReferOutcome = "outcome",
    assessmentDecision = null,
    referralRejectionReason = null,
    referralRejectionReasonDetail = null,
    assessmentSubmittedDate = Instant.now(),
    referralEligibleForCas3 = true,
    referralEligibilityReason = "reason",
    accommodationRequiredDate = Instant.now(),
    prisonNameOnCreation = null,
    personReleaseDate = null,
    town = null,
    postCode = null,
    pduName = null,
    prisonReleaseTypes = null,
    updatedReleaseDate = null,
    updatedAccommodationRequiredFromDate = null,
    previousReferralProbationRegionName = null,
    previousReferralPduName = null,
  )

  private fun createDBReferralReportDataWithPreviousReferralInfo(
    crn: String,
    previousReferralProbationRegionName: String?,
    previousReferralPduName: String?,
  ) = TestTransitionalAccommodationReferralReportData(
    UUID.randomUUID().toString(), UUID.randomUUID().toString(), UUID.randomUUID().toString(),
    Instant.now(), crn, Instant.now(), "riskOfSeriousHarm",
    registeredSexOffender = false,
    historyOfSexualOffence = false,
    concerningSexualBehaviour = true,
    needForAccessibleProperty = true,
    historyOfArsonOffence = false,
    concerningArsonBehaviour = true,
    dutyToReferMade = true,
    dateDutyToReferMade = LocalDate.now(),
    probationRegionName = "region",
    dutyToReferLocalAuthorityAreaName = "name",
    dutyToReferOutcome = "outcome",
    assessmentDecision = null,
    referralRejectionReason = null,
    referralRejectionReasonDetail = null,
    assessmentSubmittedDate = Instant.now(),
    referralEligibleForCas3 = true,
    referralEligibilityReason = "reason",
    accommodationRequiredDate = Instant.now(),
    prisonNameOnCreation = null,
    personReleaseDate = null,
    town = null,
    postCode = null,
    pduName = "pduName",
    prisonReleaseTypes = null,
    updatedReleaseDate = null,
    updatedAccommodationRequiredFromDate = null,
    previousReferralProbationRegionName = previousReferralProbationRegionName,
    previousReferralPduName = previousReferralPduName,
  )

  private fun createBookingReportData(crn: String) = TestBookingsReportData(
    bookingId = UUID.randomUUID().toString(),
    referralId = null,
    referralDate = null,
    riskOfSeriousHarm = null,
    registeredSexOffender = null,
    historyOfSexualOffence = null,
    concerningSexualBehaviour = null,
    needForAccessibleProperty = null,
    historyOfArsonOffence = null,
    concerningArsonBehaviour = null,
    dutyToReferMade = null,
    dateDutyToReferMade = null,
    referralEligibleForCas3 = null,
    referralEligibilityReason = null,
    probationRegionName = "some-region",
    localAuthorityAreaName = null,
    crn = crn,
    confirmationId = null,
    cancellationId = null,
    cancellationReason = null,
    startDate = null,
    endDate = null,
    actualEndDate = null,
    accommodationOutcome = null,
    dutyToReferLocalAuthorityAreaName = null,
    pdu = null,
    town = null,
    postCode = null,
  )

  @Suppress("LongParameterList")
  class TestTransitionalAccommodationReferralReportData(
    override val assessmentId: String,
    override val referralId: String,
    override val bookingId: String?,
    override val referralCreatedDate: Instant,
    override val crn: String,
    override val referralSubmittedDate: Instant?,
    override val riskOfSeriousHarm: String?,
    override val registeredSexOffender: Boolean?,
    override val historyOfSexualOffence: Boolean?,
    override val concerningSexualBehaviour: Boolean?,
    override val needForAccessibleProperty: Boolean?,
    override val historyOfArsonOffence: Boolean?,
    override val concerningArsonBehaviour: Boolean?,
    override val dutyToReferMade: Boolean?,
    override val dateDutyToReferMade: LocalDate?,
    override val probationRegionName: String,
    override val dutyToReferLocalAuthorityAreaName: String?,
    override val dutyToReferOutcome: String?,
    override val assessmentDecision: String?,
    override val referralRejectionReason: String?,
    override val referralRejectionReasonDetail: String?,
    override val assessmentSubmittedDate: Instant?,
    override val referralEligibleForCas3: Boolean?,
    override val referralEligibilityReason: String?,
    override val accommodationRequiredDate: Instant?,
    override val prisonNameOnCreation: String?,
    override val personReleaseDate: LocalDate?,
    override val town: String?,
    override val postCode: String?,
    override val pduName: String?,
    override val prisonReleaseTypes: String?,
    override val updatedReleaseDate: LocalDate?,
    override val updatedAccommodationRequiredFromDate: LocalDate?,
    override val previousReferralProbationRegionName: String?,
    override val previousReferralPduName: String?,
  ) : TransitionalAccommodationReferralReportData

  @Suppress("LongParameterList")
  class TestBookingsReportData(
    override val bookingId: String,
    override val referralId: String?,
    override val referralDate: Instant?,
    override val riskOfSeriousHarm: String?,
    override val registeredSexOffender: Boolean?,
    override val historyOfSexualOffence: Boolean?,
    override val concerningSexualBehaviour: Boolean?,
    override val needForAccessibleProperty: Boolean?,
    override val historyOfArsonOffence: Boolean?,
    override val concerningArsonBehaviour: Boolean?,
    override val dutyToReferMade: Boolean?,
    override val dateDutyToReferMade: LocalDate?,
    override val referralEligibleForCas3: Boolean?,
    override val referralEligibilityReason: String?,
    override val probationRegionName: String,
    override val localAuthorityAreaName: String?,
    override val crn: String,
    override val confirmationId: String?,
    override val cancellationId: String?,
    override val cancellationReason: String?,
    override val startDate: LocalDate?,
    override val endDate: LocalDate?,
    override val actualEndDate: Instant?,
    override val accommodationOutcome: String?,
    override val dutyToReferLocalAuthorityAreaName: String?,
    override val pdu: String?,
    override val town: String?,
    override val postCode: String?,
  ) : BookingsReportData
}
