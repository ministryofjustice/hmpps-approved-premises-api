package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas3

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

@Service("Cas3ReportService")
class ReportService(
  private val offenderService: OffenderService,
  private val userService: UserService,
  private val transitionalAccommodationReferralReportRowRepository: TransitionalAccommodationReferralReportRepository,
  @Value("\${cas3-report.end-date-override:0}") private val endDateOverride: Int,
) {
  fun createCas3ApplicationReferralsReport(
    properties: TransitionalAccommodationReferralReportProperties,
    outputStream: OutputStream,
  ) {
    val fromDate = LocalDate.of(properties.year, properties.month, 1)
    var toDate = LocalDate.of(properties.year, properties.month, fromDate.month.length(fromDate.isLeapYear))

    if (endDateOverride != 0) {
      toDate = fromDate.plusMonths((endDateOverride).toLong())
    }

    val referralsInScope = transitionalAccommodationReferralReportRowRepository.findAllReferrals(
      fromDate,
      toDate,
      properties.probationRegionId,
    )

    val crns = referralsInScope.map { it.crn }.sorted().toSet()
    val personInfos = offenderService.getOffenderSummariesByCrns(
      crns.toSet(),
      userService.getUserForRequest().deliusUsername,
    ).associateBy { it.crn }

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
}
