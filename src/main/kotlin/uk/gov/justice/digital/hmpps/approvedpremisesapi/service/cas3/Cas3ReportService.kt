package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas3

import org.apache.poi.ss.usermodel.WorkbookFactory
import org.jetbrains.kotlinx.dataframe.io.writeExcel
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BookingRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas3.Cas3BookingGapReportRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas3.Cas3FutureBookingsReportRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas3.Cas3VoidBedspacesRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonSummaryInfoResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.generator.BedUsageReportGenerator
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.generator.BedUtilisationReportGenerator
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.generator.BookingsReportGenerator
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.generator.FutureBookingsCsvReportGenerator
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.generator.FutureBookingsReportGenerator
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.generator.TransitionalAccommodationReferralReportGenerator
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.model.BedUtilisationReportData
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.model.BookingsReportDataAndPersonInfo
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.model.FutureBookingsReportDataAndPersonInfo
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.model.PersonInformationReportData
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.model.TransitionalAccommodationReferralReportDataAndPersonInfo
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.properties.BedUsageReportProperties
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.properties.BedUtilisationReportProperties
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.properties.BookingGapReportProperties
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.properties.BookingsReportProperties
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.properties.FutureBookingsReportProperties
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.properties.TransitionalAccommodationReferralReportProperties
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.util.CsvJdbcResultSetConsumer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.util.CsvObjectListConsumer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.repository.BedUsageRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.repository.BedUtilisationReportRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.repository.BookingsReportRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.repository.TransitionalAccommodationReferralReportRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.OffenderService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.WorkingDayService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas3LaoStrategy
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.BookingTransformer
import java.io.OutputStream

@Service
class Cas3ReportService(
  private val offenderService: OffenderService,
  private val userService: UserService,
  private val transitionalAccommodationReferralReportRowRepository: TransitionalAccommodationReferralReportRepository,
  private val bookingsReportRepository: BookingsReportRepository,
  private val cas3VoidBedspacesRepository: Cas3VoidBedspacesRepository,
  private val bookingTransformer: BookingTransformer,
  private val workingDayService: WorkingDayService,
  private val bookingRepository: BookingRepository,
  private val bedUsageRepository: BedUsageRepository,
  private val bedUtilisationReportRepository: BedUtilisationReportRepository,
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
    val referralsInScope = transitionalAccommodationReferralReportRowRepository.findAllReferrals(
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

    TransitionalAccommodationReferralReportGenerator()
      .createReport(reportData, properties)
      .writeExcel(
        outputStream = outputStream,
        factory = WorkbookFactory.create(true),
      )
  }

  fun createBookingsReport(properties: BookingsReportProperties, outputStream: OutputStream) {
    log.info("Beginning CAS3 Bookings Report")
    val bookingsInScope = bookingsReportRepository.findAllByOverlappingDate(
      properties.startDate,
      properties.endDate,
      ServiceName.temporaryAccommodation.value,
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
        factory = WorkbookFactory.create(true),
      )
  }

  fun createBedUsageReport(properties: BedUsageReportProperties, outputStream: OutputStream) {
    log.info("Beginning CAS3 Bed Usage Report")
    val bedspacesInScope = bedUsageRepository.findAllBedspaces(
      probationRegionId = properties.probationRegionId,
    )

    log.info("Creating report")
    BedUsageReportGenerator(bookingTransformer, bookingRepository, cas3VoidBedspacesRepository, workingDayService)
      .createReport(bedspacesInScope, properties)
      .writeExcel(
        outputStream = outputStream,
        factory = WorkbookFactory.create(true),
      )
  }

  fun createBedUtilisationReport(properties: BedUtilisationReportProperties, outputStream: OutputStream) {
    log.info("Beginning CAS3 Bed Utilisation Report")
    val bedspacesInScope = bedUtilisationReportRepository.findAllBedspaces(
      probationRegionId = properties.probationRegionId,
      startDate = properties.startDate,
      endDate = properties.endDate,
    )
    log.info("${bedspacesInScope.size} bedspaces found.")

    val bedspaceBookingsInScope = bedUtilisationReportRepository.findAllBookingsByOverlappingDate(
      probationRegionId = properties.probationRegionId,
      properties.startDate,
      properties.endDate,
    )
    log.info("${bedspaceBookingsInScope.size} bedspace bookings found.")

    val bedspaceBookingsCancellationInScope =
      bedUtilisationReportRepository.findAllBookingCancellationsByOverlappingDate(
        probationRegionId = properties.probationRegionId,
        properties.startDate,
        properties.endDate,
      )

    log.info("${bedspaceBookingsCancellationInScope.size} bedspace booking cancellation found.")

    val bedspaceBookingsTurnaroundInScope = bedUtilisationReportRepository.findAllBookingTurnaroundByOverlappingDate(
      probationRegionId = properties.probationRegionId,
      properties.startDate,
      properties.endDate,
    )

    log.info("${bedspaceBookingsTurnaroundInScope.size} bedspace booking in turnaround found.")

    val voidBedspaceInScope = bedUtilisationReportRepository.findAllVoidBedspaceByOverlappingDate(
      probationRegionId = properties.probationRegionId,
      properties.startDate,
      properties.endDate,
    )

    log.info("${voidBedspaceInScope.size} void bedspace found.")

    val reportData = bedspacesInScope.map { bedspace ->
      val bedId = bedspace.bedId
      val bedspaceBookings = bedspaceBookingsInScope.filter { it.bedId == bedId }
      val bedspaceBookingsCancellation = bedspaceBookingsCancellationInScope.filter { it.bedId == bedId }
      val bedspaceBookingsTurnaround = bedspaceBookingsTurnaroundInScope.filter { it.bedId == bedId }
      val voidBedspace = voidBedspaceInScope.filter { it.bedId == bedId }

      BedUtilisationReportData(
        bedspace,
        bedspaceBookings,
        bedspaceBookingsCancellation,
        bedspaceBookingsTurnaround,
        voidBedspace,
      )
    }

    log.info("Creating report")

    BedUtilisationReportGenerator(workingDayService)
      .createReport(reportData, properties)
      .writeExcel(
        outputStream = outputStream,
        factory = WorkbookFactory.create(true),
      )
  }

  fun createBookingGapReport(properties: BookingGapReportProperties, outputStream: OutputStream) {
    log.info("Beginning CAS3 Booking Gap Report")

    CsvJdbcResultSetConsumer(
      outputStream = outputStream,
    ).use { consumer ->
      cas3BookingGapReportRepository.generateBookingGapReport(
        properties.startDate,
        properties.endDate,
        consumer,
      )
    }
  }

  fun createFutureBookingReport(properties: FutureBookingsReportProperties, outputStream: OutputStream) {
    log.info("Beginning CAS3 Future Booking Report")
    val bookingsInScope = cas3FutureBookingsReportRepository.findAllFutureBookings(
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

    FutureBookingsReportGenerator()
      .createReport(reportData, properties)
      .writeExcel(
        outputStream = outputStream,
        factory = WorkbookFactory.create(true),
      )
  }

  fun createFutureBookingCsvReport(properties: FutureBookingsReportProperties, outputStream: OutputStream) {
    log.info("Beginning CAS3 Future Booking CSV Report")
    val bookingsInScope = cas3FutureBookingsReportRepository.findAllFutureBookings(
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
