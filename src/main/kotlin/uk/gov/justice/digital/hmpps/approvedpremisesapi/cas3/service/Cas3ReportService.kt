package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.service

import org.apache.poi.xssf.streaming.SXSSFWorkbook
import org.jetbrains.kotlinx.dataframe.io.writeExcel
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.reporting.CsvObjectListConsumer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3BookingRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3FutureBookingsReportRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3VoidBedspacesRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.reporting.generator.BedspaceOccupancyReportGenerator
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.reporting.generator.BedspaceUsageReportGenerator
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.reporting.generator.BookingGapReportGenerator
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.reporting.generator.BookingsReportGenerator
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.reporting.generator.FutureBookingsCsvReportGenerator
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.reporting.generator.FutureBookingsReportGenerator
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.reporting.generator.TransitionalAccommodationReferralReportGenerator
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.reporting.model.BedspaceOccupancyReportData
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.reporting.model.BedspaceUsageReportData
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.reporting.model.BookingGapReportData
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.reporting.model.BookingsReportDataAndPersonInfo
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.reporting.model.FutureBookingsReportDataAndPersonInfo
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.reporting.model.PersonInformationReportData
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.reporting.model.TransitionalAccommodationReferralReportDataAndPersonInfo
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.reporting.properties.BedUsageReportProperties
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.reporting.properties.BedspaceOccupancyReportProperties
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.reporting.properties.BookingGapReportProperties
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.reporting.properties.BookingsReportProperties
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.reporting.properties.FutureBookingsReportProperties
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.reporting.properties.TransitionalAccommodationReferralReportProperties
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.repository.BedUsageRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.repository.BedspaceOccupancyReportRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.repository.BookingsReportRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.repository.Cas3BookingGapReportRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.repository.TransitionalAccommodationReferralReportRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.transformer.Cas3BookingTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonSummaryInfoResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.OffenderService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.WorkingDayService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas3LaoStrategy
import java.io.OutputStream
import kotlin.collections.emptyList

private const val MAX_IN_MEMORY_ROWS = 100
const val MAX_DAYS_STAY = 84

@Service
class Cas3ReportService(
  private val offenderService: OffenderService,
  private val userService: UserService,
  private val transitionalAccommodationReferralReportRowRepository: TransitionalAccommodationReferralReportRepository,
  private val bookingsReportRepository: BookingsReportRepository,
  private val cas3VoidBedspacesRepository: Cas3VoidBedspacesRepository,
  private val cas3BookingTransformer: Cas3BookingTransformer,
  private val workingDayService: WorkingDayService,
  private val cas3BookingRepository: Cas3BookingRepository,
  private val bedUsageRepository: BedUsageRepository,
  private val bedspaceOccupancyReportRepository: BedspaceOccupancyReportRepository,
  private val cas3FutureBookingsReportRepository: Cas3FutureBookingsReportRepository,
  private val cas3BookingGapReportRepository: Cas3BookingGapReportRepository,
  @Value("\${cas3-report.crn-search-limit:500}") private val numberOfCrn: Int,
) {

  var log: Logger = LoggerFactory.getLogger(this::class.java)

  fun createCas3ApplicationReferralsReport(
    properties: TransitionalAccommodationReferralReportProperties,
    outputStream: OutputStream,
  ) {
    log.info("Beginning CAS3 Application Referrals Report")
    val referralsInScope = transitionalAccommodationReferralReportRowRepository.findAllReferralsV2(
      startDate = properties.startDate,
      endDate = properties.endDate,
      probationRegionId = properties.probationRegionId,
    )

    log.info("${referralsInScope.size} referrals found.")

    val crns = referralsInScope.map { it.crn }.sorted().toSet()
    log.info("Getting person info for ${crns.size} CRNs")
    val personInfos = splitAndRetrievePersonInfo(crns)
    log.info("Person info retrieved")
    val reportData = referralsInScope.map {
      val personInfo = personInfos[it.crn] ?: PersonSummaryInfoResult.Unknown(it.crn)
      TransitionalAccommodationReferralReportDataAndPersonInfo(it, personInfo)
    }
    log.info("Creating report")
    val workbook = SXSSFWorkbook(MAX_IN_MEMORY_ROWS)
    TransitionalAccommodationReferralReportGenerator()
      .createReport(reportData, properties)
      .writeExcel(
        outputStream = outputStream,
        factory = workbook,
      )
    workbook.close()
  }

  fun createBookingsReport(properties: BookingsReportProperties, outputStream: OutputStream) {
    log.info("Beginning CAS3 Bookings Report")
    val bookingsInScope = bookingsReportRepository.findAllByOverlappingDateV2(
      properties.startDate,
      properties.endDate,
      properties.probationRegionId,
    )

    val crns = bookingsInScope.map { it.crn }.distinct().sorted()
    log.info("Getting person info for ${crns.size} CRNs")
    val personInfos = splitAndRetrievePersonInfoReportData(crns.toSet())
    log.info("Person info retrieved")
    val reportData = bookingsInScope.map {
      BookingsReportDataAndPersonInfo(it, personInfos[it.crn]!!)
    }

    log.info("Creating report")
    val workbook = SXSSFWorkbook(MAX_IN_MEMORY_ROWS)
    BookingsReportGenerator()
      .createReport(
        reportData,
        BookingsReportProperties(
          ServiceName.temporaryAccommodation,
          properties.probationRegionId,
          properties.startDate,
          properties.endDate,
        ),
      )
      .writeExcel(
        outputStream = outputStream,
        factory = workbook,
      )
    workbook.close()
  }

  fun createBedUsageReport(properties: BedUsageReportProperties, outputStream: OutputStream) {
    log.info("Beginning CAS3 Bed Usage Report")
    val bedspacesInScope = bedUsageRepository.findAllBedspacesV2(
      probationRegionId = properties.probationRegionId,
    )
    val bedspaceIds = bedspacesInScope.map { it.id }
    val bookings =
      if (bedspaceIds.isEmpty()) emptyList() else cas3BookingRepository.findAllByOverlappingDateForBedspaceIds(properties.startDate, properties.endDate, bedspaceIds)
    val voids =
      if (bedspaceIds.isEmpty()) emptyList() else cas3VoidBedspacesRepository.findAllByOverlappingDateForBedspaceIds(properties.startDate, properties.endDate, bedspaceIds)

    // Pre-group to avoid repeatedly scanning the same lists per bedspace.
    val bookingsByBedspaceId = bookings.groupBy { it.bedspace.id }
    val voidsByBedspaceId = voids.groupBy { it.bedspace?.id }

    val reportData = bedspacesInScope.map { bedspace ->
      BedspaceUsageReportData(
        bedspace = bedspace,
        bookings = bookingsByBedspaceId[bedspace.id].orEmpty(),
        voids = voidsByBedspaceId[bedspace.id].orEmpty(),
      )
    }
    val workbook = SXSSFWorkbook(MAX_IN_MEMORY_ROWS)
    BedspaceUsageReportGenerator(cas3BookingTransformer, workingDayService)
      .createReport(reportData, properties)
      .writeExcel(
        outputStream = outputStream,
        factory = workbook,
      )
    workbook.close()
  }

  fun createBedspaceOccupancyReport(properties: BedspaceOccupancyReportProperties, outputStream: OutputStream) {
    log.info("Beginning CAS3 Bedspace Occupancy Report")
    val bedspacesInScope = bedspaceOccupancyReportRepository.findAllBedspaces(
      probationRegionId = properties.probationRegionId,
      startDate = properties.startDate,
      endDate = properties.endDate,
    )
    log.info("${bedspacesInScope.size} bedspaces found.")

    val bedspaceBookingsInScope = bedspaceOccupancyReportRepository.findAllBookingsByOverlappingDate(
      probationRegionId = properties.probationRegionId,
      properties.startDate,
      properties.endDate,
    )
    log.info("${bedspaceBookingsInScope.size} bedspace bookings found.")

    val bedspaceBookingsCancellationInScope =
      bedspaceOccupancyReportRepository.findAllBookingCancellationsByOverlappingDate(
        probationRegionId = properties.probationRegionId,
        properties.startDate,
        properties.endDate,
      )

    log.info("${bedspaceBookingsCancellationInScope.size} bedspace booking cancellation found.")

    val bedspaceBookingsTurnaroundInScope = bedspaceOccupancyReportRepository.findAllBookingTurnaroundByOverlappingDate(
      probationRegionId = properties.probationRegionId,
      properties.startDate,
      properties.endDate,
    )

    log.info("${bedspaceBookingsTurnaroundInScope.size} bedspace booking in turnaround found.")

    val voidBedspaceInScope = bedspaceOccupancyReportRepository.findAllVoidBedspaceByOverlappingDate(
      probationRegionId = properties.probationRegionId,
      properties.startDate,
      properties.endDate,
    )

    log.info("${voidBedspaceInScope.size} void bedspace found.")

    val reportData = bedspacesInScope.map { bedspace ->
      val bedspaceId = bedspace.bedspaceId
      val bedspaceBookings = bedspaceBookingsInScope.filter { it.bedspaceId == bedspaceId }
      val bedspaceBookingsCancellation = bedspaceBookingsCancellationInScope.filter { it.bedspaceId == bedspaceId }
      val bedspaceBookingsTurnaround = bedspaceBookingsTurnaroundInScope.filter { it.bedspaceId == bedspaceId }
      val voidBedspace = voidBedspaceInScope.filter { it.bedspaceId == bedspaceId }

      BedspaceOccupancyReportData(
        bedspace,
        bedspaceBookings,
        bedspaceBookingsCancellation,
        bedspaceBookingsTurnaround,
        voidBedspace,
      )
    }

    log.info("Creating bedspace occupancy report")
    val workbook = SXSSFWorkbook(MAX_IN_MEMORY_ROWS)
    BedspaceOccupancyReportGenerator(workingDayService)
      .createReport(reportData, properties)
      .writeExcel(
        outputStream = outputStream,
        factory = workbook,
      )
    workbook.close()
  }

  fun createFutureBookingReport(properties: FutureBookingsReportProperties, outputStream: OutputStream) {
    log.info("Beginning CAS3 Future Booking Report")
    val bookingsInScope = cas3FutureBookingsReportRepository.findAllFutureBookingsV2(
      properties.startDate,
      properties.endDate,
      properties.probationRegionId,
    )

    val crns = bookingsInScope.map { it.crn }.distinct().toSet()
    val personInfos = splitAndRetrievePersonInfo(crns)
    val reportData = bookingsInScope.map {
      val personInfo = personInfos[it.crn] ?: PersonSummaryInfoResult.Unknown(it.crn)
      FutureBookingsReportDataAndPersonInfo(it, personInfo)
    }

    log.info("Creating report")
    val workbook = SXSSFWorkbook(MAX_IN_MEMORY_ROWS)
    FutureBookingsReportGenerator()
      .createReport(reportData, properties)
      .writeExcel(
        outputStream = outputStream,
        factory = workbook,
      )
    workbook.close()
  }

  fun createFutureBookingCsvReport(properties: FutureBookingsReportProperties, outputStream: OutputStream) {
    log.info("Beginning CAS3 Future Booking CSV Report")
    val bookingsInScope = cas3FutureBookingsReportRepository.findAllFutureBookingsV2(
      properties.startDate,
      properties.endDate,
      properties.probationRegionId,
    )

    val crns = bookingsInScope.map { it.crn }.distinct().toSet()
    val personInfos = splitAndRetrievePersonInfo(crns)
    val reportData = bookingsInScope.map {
      val personInfo = personInfos[it.crn] ?: PersonSummaryInfoResult.Unknown(it.crn)
      FutureBookingsCsvReportGenerator().convert(it, personInfo)
    }

    log.info("Creating report")

    CsvObjectListConsumer(
      outputStream = outputStream,
    ).consume(reportData)
  }

  fun createBookingGapReport(properties: BookingGapReportProperties, outputStream: OutputStream) {
    log.info("Beginning CAS3 Bookings Gap Report")
    val bedspaces = cas3BookingGapReportRepository.getBedspacesV2(properties.startDate, properties.endDate)
    if (bedspaces.isEmpty()) {
      createBookingGapReport(BookingGapReportData(bedspaces, emptyList(), emptyList()), properties, outputStream)
    } else {
      val bookings = cas3BookingGapReportRepository.getBookingsV2(properties.startDate, properties.endDate)

      createBookingGapReport(
        BookingGapReportData(
          bedspaces,
          bookings,
          cas3BookingGapReportRepository.getBedspaceVoidsV2(properties.startDate),
        ),
        properties,
        outputStream,
      )
    }
  }

  private fun createBookingGapReport(
    bookingGapReportData: BookingGapReportData,
    properties: BookingGapReportProperties,
    outputStream: OutputStream,
  ) {
    val workbook = SXSSFWorkbook(MAX_IN_MEMORY_ROWS)
    BookingGapReportGenerator(workingDayService)
      .createReport(
        listOf(
          bookingGapReportData,
        ),
        properties,
      ).writeExcel(
        outputStream = outputStream,
        factory = workbook,
      )
    workbook.close()
  }

  private fun splitAndRetrievePersonInfoReportData(crns: Set<String>): Map<String, PersonInformationReportData> {
    val user = userService.getUserForRequest()

    return offenderService.getPersonSummaryInfoResultsInBatches(
      crns = crns,
      laoStrategy = user.cas3LaoStrategy(),
      batchSize = numberOfCrn,
    ).associate {
      when (it) {
        is PersonSummaryInfoResult.Success.Full -> {
          val personInfo = PersonInformationReportData(
            it.summary.pnc,
            it.summary.name,
            it.summary.dateOfBirth,
            it.summary.gender,
            it.summary.profile?.ethnicity,
          )
          (it.crn to personInfo)
        }

        else -> {
          val personInfo = PersonInformationReportData(null, null, null, null, null)
          (it.crn to personInfo)
        }
      }
    }
  }

  private fun splitAndRetrievePersonInfo(crns: Set<String>): Map<String, PersonSummaryInfoResult> {
    val user = userService.getUserForRequest()

    return offenderService.getPersonSummaryInfoResultsInBatches(
      crns = crns.toSet(),
      laoStrategy = user.cas3LaoStrategy(),
      batchSize = numberOfCrn,
    ).associateBy { it.crn }
  }
}
