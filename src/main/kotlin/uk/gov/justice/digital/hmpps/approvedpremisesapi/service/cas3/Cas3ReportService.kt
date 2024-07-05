package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas3

import org.apache.commons.collections4.ListUtils
import org.apache.poi.ss.usermodel.WorkbookFactory
import org.jetbrains.kotlinx.dataframe.io.writeExcel
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BedRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BookingRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.LostBedsRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonSummaryInfoResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.generator.BedUsageReportGenerator
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.generator.BedUtilisationReportGenerator
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.generator.BookingsReportGenerator
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.generator.TransitionalAccommodationReferralReportGenerator
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.model.BedUtilisationReportData
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.model.BookingsReportDataAndPersonInfo
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.model.TransitionalAccommodationReferralReportDataAndPersonInfo
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.properties.BedUsageReportProperties
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.properties.BedUtilisationReportProperties
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.properties.BookingsReportProperties
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.properties.TransitionalAccommodationReferralReportProperties
import uk.gov.justice.digital.hmpps.approvedpremisesapi.repository.BedUtilisationReportRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.repository.BookingsReportRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.repository.TransitionalAccommodationReferralReportRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.OffenderService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.WorkingDayService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.BookingTransformer
import java.io.OutputStream
import java.util.stream.Collectors

@Service
class Cas3ReportService(
  private val offenderService: OffenderService,
  private val userService: UserService,
  private val transitionalAccommodationReferralReportRowRepository: TransitionalAccommodationReferralReportRepository,
  private val bookingsReportRepository: BookingsReportRepository,
  private val lostBedsRepository: LostBedsRepository,
  private val bookingTransformer: BookingTransformer,
  private val workingDayService: WorkingDayService,
  private val bookingRepository: BookingRepository,
  private val bedRepository: BedRepository,
  private val bedUtilisationReportRepository: BedUtilisationReportRepository,
  @Value("\${cas3-report.crn-search-limit:400}") private val numberOfCrn: Int,
) {
  fun createCas3ApplicationReferralsReport(
    properties: TransitionalAccommodationReferralReportProperties,
    outputStream: OutputStream,
  ) {
    val referralsInScope = transitionalAccommodationReferralReportRowRepository.findAllReferrals(
      startDate = properties.startDate,
      endDate = properties.endDate,
      probationRegionId = properties.probationRegionId,
    )

    val crns = referralsInScope.map { it.crn }.sorted().toSet()
    val personInfos = splitAndRetrievePersonInfo(crns)
    val reportData = referralsInScope.map {
      val personInfo = personInfos[it.crn] ?: PersonSummaryInfoResult.Unknown(it.crn)
      TransitionalAccommodationReferralReportDataAndPersonInfo(it, personInfo)
    }

    TransitionalAccommodationReferralReportGenerator()
      .createReport(reportData, properties)
      .writeExcel(outputStream) {
        WorkbookFactory.create(true)
      }
  }

  fun createBookingsReport(properties: BookingsReportProperties, outputStream: OutputStream) {
    val bookingsInScope = bookingsReportRepository.findAllByOverlappingDate(
      properties.startDate,
      properties.endDate,
      ServiceName.temporaryAccommodation.value,
      properties.probationRegionId,
    )

    val crns = bookingsInScope.map { it.crn }.distinct().sorted()
    val personInfos = splitAndRetrievePersonInfo(crns.toSet())

    val reportData = bookingsInScope.map {
      val personInfo = personInfos[it.crn] ?: PersonSummaryInfoResult.Unknown(it.crn)
      BookingsReportDataAndPersonInfo(it, personInfo)
    }

    BookingsReportGenerator()
      .createReport(reportData, BookingsReportProperties(ServiceName.temporaryAccommodation, properties.probationRegionId, properties.startDate, properties.endDate))
      .writeExcel(outputStream) {
        WorkbookFactory.create(true)
      }
  }

  fun createBedUsageReport(properties: BedUsageReportProperties, outputStream: OutputStream) {
    BedUsageReportGenerator(bookingTransformer, bookingRepository, lostBedsRepository, workingDayService)
      .createReport(bedRepository.findAll(), properties)
      .writeExcel(outputStream) {
        WorkbookFactory.create(true)
      }
  }

  fun createBedUtilisationReport(properties: BedUtilisationReportProperties, outputStream: OutputStream) {
    val bedspacesInScope = bedUtilisationReportRepository.findAllBedspaces(
      probationRegionId = properties.probationRegionId,
    )

    val bedspaceBookingsInScope = bedUtilisationReportRepository.findAllBookingsByOverlappingDate(
      probationRegionId = properties.probationRegionId,
      properties.startDate,
      properties.endDate,
    )

    val lostBedspaceInScope = bedUtilisationReportRepository.findAllLostBedByOverlappingDate(
      probationRegionId = properties.probationRegionId,
      properties.startDate,
      properties.endDate,
    )

    val reportData = bedspacesInScope.map {
      val bedId = it.bedId
      val bedspaceBookings = bedspaceBookingsInScope.filter { it.bedId == bedId }
      val lostBedspace = lostBedspaceInScope.filter { it.bedId == bedId }
      BedUtilisationReportData(it, bedspaceBookings, lostBedspace)
    }

    BedUtilisationReportGenerator(workingDayService)
      .createReport(reportData, properties)
      .writeExcel(outputStream) {
        WorkbookFactory.create(true)
      }
  }

  private fun splitAndRetrievePersonInfo(crns: Set<String>): Map<String, PersonSummaryInfoResult> {
    val deliusUsername = userService.getUserForRequest().deliusUsername

    val crnMap = ListUtils.partition(crns.toList(), numberOfCrn)
      .stream().map { crns ->
        offenderService.getOffenderSummariesByCrns(crns.toSet(), deliusUsername).associateBy { it.crn }
      }.collect(Collectors.toList())

    return crnMap.flatMap { it.toList() }.toMap()
  }
}
