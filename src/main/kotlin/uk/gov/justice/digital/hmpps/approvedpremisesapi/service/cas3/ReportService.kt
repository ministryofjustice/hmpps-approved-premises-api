package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas3

import org.apache.commons.collections4.ListUtils
import org.apache.poi.ss.usermodel.WorkbookFactory
import org.jetbrains.kotlinx.dataframe.io.writeExcel
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonSummaryInfoResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.generator.TransitionalAccommodationReferralReportGenerator
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.model.TransitionalAccommodationReferralReportDataAndPersonInfo
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.properties.TransitionalAccommodationReferralReportProperties
import uk.gov.justice.digital.hmpps.approvedpremisesapi.repository.TransitionalAccommodationReferralReportRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.OffenderService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserService
import java.io.OutputStream
import java.time.LocalDate
import java.util.stream.Collectors

@Service("Cas3ReportService")
class ReportService(
  private val offenderService: OffenderService,
  private val userService: UserService,
  private val transitionalAccommodationReferralReportRowRepository: TransitionalAccommodationReferralReportRepository,
  @Value("\${cas3-report.end-date-override:0}") private val endDateOverride: Int,
  @Value("\${cas3-report.crn-search-limit:400}") private val numberOfCrn: Int,
) {
  fun createCas3ApplicationReferralsReport(
    properties: TransitionalAccommodationReferralReportProperties,
    outputStream: OutputStream,
  ) {
    val fromDate = LocalDate.of(properties.year, properties.month, 1)
    val toDate = if (endDateOverride != 0) {
      fromDate.plusMonths(endDateOverride.toLong())
    } else {
      LocalDate.of(properties.year, properties.month, fromDate.month.length(fromDate.isLeapYear))
    }

    val referralsInScope = transitionalAccommodationReferralReportRowRepository.findAllReferrals(
      fromDate,
      toDate,
      properties.probationRegionId,
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

  private fun splitAndRetrievePersonInfo(crns: Set<String>): Map<String, PersonSummaryInfoResult> {
    val deliusUsername = userService.getUserForRequest().deliusUsername

    val crnMap = ListUtils.partition(crns.toList(), numberOfCrn)
      .stream().map { crns ->
        offenderService.getOffenderSummariesByCrns(crns.toSet(), deliusUsername).associateBy { it.crn }
      }.collect(Collectors.toList())

    return crnMap.flatMap { it.toList() }.toMap()
  }
}
