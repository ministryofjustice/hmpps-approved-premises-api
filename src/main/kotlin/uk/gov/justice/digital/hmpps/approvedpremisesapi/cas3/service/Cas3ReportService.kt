package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.service

import org.apache.poi.ss.usermodel.WorkbookFactory
import org.jetbrains.kotlinx.dataframe.io.writeExcel
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.reporting.CsvObjectListConsumer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3FutureBookingsReportRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3VoidBedspacesRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3v2BookingRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.reporting.generator.BedUsageReportGenerator
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.reporting.generator.BedUtilisationReportGenerator
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.reporting.generator.BedspaceOccupancyReportGenerator
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.reporting.generator.BedspaceUsageReportGenerator
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.reporting.generator.BookingGapReportGenerator
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.reporting.generator.BookingsReportGenerator
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.reporting.generator.FutureBookingsCsvReportGenerator
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.reporting.generator.FutureBookingsReportGenerator
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.reporting.generator.TransitionalAccommodationReferralReportGenerator
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.reporting.model.BedUtilisationReportData
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.reporting.model.BedspaceOccupancyReportData
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.reporting.model.BookingGapReportData
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.reporting.model.BookingsReportDataAndPersonInfo
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.reporting.model.FutureBookingsReportDataAndPersonInfo
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.reporting.model.PersonInformationReportData
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.reporting.model.TransitionalAccommodationReferralReportDataAndPersonInfo
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.reporting.properties.BedUsageReportProperties
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.reporting.properties.BedUtilisationReportProperties
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.reporting.properties.BedspaceOccupancyReportProperties
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.reporting.properties.BookingGapReportProperties
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.reporting.properties.BookingsReportProperties
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.reporting.properties.FutureBookingsReportProperties
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.reporting.properties.TransitionalAccommodationReferralReportProperties
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.repository.BedUsageRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.repository.BedUtilisationReportRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.repository.BedspaceOccupancyReportRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.repository.BookingsReportRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.repository.Cas3BookingGapReportRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.repository.TransitionalAccommodationReferralReportRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.transformer.Cas3BookingTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BookingRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonSummaryInfoResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.FeatureFlagService
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
  private val cas3BookingTransformer: Cas3BookingTransformer,
  private val workingDayService: WorkingDayService,
  private val featureFlagService: FeatureFlagService,
  private val bookingRepository: BookingRepository,
  private val cas3v2BookingRepository: Cas3v2BookingRepository,
  private val bedUsageRepository: BedUsageRepository,
  private val bedUtilisationReportRepository: BedUtilisationReportRepository,
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
    val referralsInScope = when (featureFlagService.getBooleanFlag("cas3-reports-with-new-bedspace-model-tables-enabled")) {
      true -> transitionalAccommodationReferralReportRowRepository.findAllReferralsV2(
        startDate = properties.startDate,
        endDate = properties.endDate,
        probationRegionId = properties.probationRegionId,
      )
      false -> transitionalAccommodationReferralReportRowRepository.findAllReferrals(
        startDate = properties.startDate,
        endDate = properties.endDate,
        probationRegionId = properties.probationRegionId,
      )
    }

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
    val bookingsInScope = when (featureFlagService.getBooleanFlag("cas3-reports-with-new-bedspace-model-tables-enabled")) {
      true -> bookingsReportRepository.findAllByOverlappingDateV2(
        properties.startDate,
        properties.endDate,
        properties.probationRegionId,
      )
      false -> bookingsReportRepository.findAllByOverlappingDate(
        properties.startDate,
        properties.endDate,
        ServiceName.temporaryAccommodation.value,
        properties.probationRegionId,
      )
    }

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
    when (featureFlagService.getBooleanFlag("cas3-reports-with-new-bedspace-model-tables-enabled")) {
      true -> {
        val bedspacesInScope = bedUsageRepository.findAllBedspacesV2(
          probationRegionId = properties.probationRegionId,
        )
        BedspaceUsageReportGenerator(cas3BookingTransformer, cas3v2BookingRepository, cas3VoidBedspacesRepository, workingDayService)
          .createReport(bedspacesInScope, properties)
          .writeExcel(
            outputStream = outputStream,
            factory = WorkbookFactory.create(true),
          )
      }
      false -> {
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
    }
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

    BedspaceOccupancyReportGenerator(workingDayService)
      .createReport(reportData, properties)
      .writeExcel(
        outputStream = outputStream,
        factory = WorkbookFactory.create(true),
      )
  }

  fun createFutureBookingReport(properties: FutureBookingsReportProperties, outputStream: OutputStream) {
    log.info("Beginning CAS3 Future Booking Report")
    val bookingsInScope = when (featureFlagService.getBooleanFlag("cas3-reports-with-new-bedspace-model-tables-enabled")) {
      true -> cas3FutureBookingsReportRepository.findAllFutureBookingsV2(
        properties.startDate,
        properties.endDate,
        properties.probationRegionId,
      )
      false -> cas3FutureBookingsReportRepository.findAllFutureBookings(
        properties.startDate,
        properties.endDate,
        properties.probationRegionId,
      )
    }

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
    val bookingsInScope = when (featureFlagService.getBooleanFlag("cas3-reports-with-new-bedspace-model-tables-enabled")) {
      true -> cas3FutureBookingsReportRepository.findAllFutureBookingsV2(
        properties.startDate,
        properties.endDate,
        properties.probationRegionId,
      )

      false -> cas3FutureBookingsReportRepository.findAllFutureBookings(
        properties.startDate,
        properties.endDate,
        properties.probationRegionId,
      )
    }

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
    when (featureFlagService.getBooleanFlag("cas3-reports-with-new-bedspace-model-tables-enabled")) {
      true -> {
        log.info("Creating V2 report")
        val bedspaces = cas3BookingGapReportRepository.getBedspacesV2(properties.startDate, properties.endDate)
        BookingGapReportGenerator()
          .createReport(
            listOf(
              BookingGapReportData(
                bedspaces,
                cas3BookingGapReportRepository.getBookings(properties.startDate, properties.endDate),
                cas3BookingGapReportRepository.getBedspaceVoidsV2(properties.startDate, properties.endDate, bedspaces.map { it.id }),
              ),
            ),
            properties,
          ).writeExcel(
            outputStream = outputStream,
            factory = WorkbookFactory.create(true),
          )
      }
      false -> {
        log.info("Creating report")
        val bedspaces = cas3BookingGapReportRepository.getBedspaces(properties.startDate, properties.endDate)
        BookingGapReportGenerator()
          .createReport(
            listOf(
              BookingGapReportData(
                bedspaces,
                cas3BookingGapReportRepository.getBookings(properties.startDate, properties.endDate),
                cas3BookingGapReportRepository.getBedspaceVoids(properties.startDate, properties.endDate, bedspaces.map { it.id }),
              ),
            ),
            properties,
          ).writeExcel(
            outputStream = outputStream,
            factory = WorkbookFactory.create(true),
          )
      }
    }
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
