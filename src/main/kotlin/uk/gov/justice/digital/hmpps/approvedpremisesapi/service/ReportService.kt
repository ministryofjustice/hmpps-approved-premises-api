package uk.gov.justice.digital.hmpps.approvedpremisesapi.service

import org.apache.poi.ss.usermodel.WorkbookFactory
import org.jetbrains.kotlinx.dataframe.io.writeExcel
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationEntityReportRowRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationTimelinessEntityRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.generator.ApplicationReportGenerator
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.generator.PlacementMetricsReportGenerator
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.generator.ReferralsMetricsReportGenerator
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.model.ReferralsDataDto
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.model.TierCategory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.properties.ApplicationReportProperties
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.properties.PlacementMetricsReportProperties
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.properties.ReferralsMetricsProperties
import java.io.OutputStream

@Service
class ReportService(
  private val workingDayService: WorkingDayService,
  private val applicationEntityReportRowRepository: ApplicationEntityReportRowRepository,
  private val assessmentRepository: AssessmentRepository,
  private val timelinessEntityRepository: ApplicationTimelinessEntityRepository,
) {

  fun createCas1ApplicationReferralsReport(properties: ApplicationReportProperties, outputStream: OutputStream) {
    ApplicationReportGenerator()
      .createReport(applicationEntityReportRowRepository.generateApprovedPremisesReferralReportRowsForCalendarMonth(properties.month, properties.year), properties)
      .writeExcel(outputStream) {
        WorkbookFactory.create(true)
      }
  }

  fun <T : Any> createReferralsMetricsReport(properties: ReferralsMetricsProperties, outputStream: OutputStream, categories: List<T>) {
    val referrals = assessmentRepository.findAllReferralsDataForMonthAndYear(properties.month, properties.year).map {
      ReferralsDataDto(
        it.getTier(),
        it.getIsEsapApplication(),
        it.getIsPipeApplication(),
        it.getDecision(),
        it.getApplicationSubmittedAt()?.toLocalDateTime()?.toLocalDate(),
        it.getAssessmentSubmittedAt()?.toLocalDateTime()?.toLocalDate(),
        it.getRejectionRationale(),
        it.getReleaseType(),
        it.getClarificationNoteCount(),
      )
    }

    ReferralsMetricsReportGenerator<T>(referrals, workingDayService)
      .createReport(categories, properties)
      .writeExcel(outputStream) {
        WorkbookFactory.create(true)
      }
  }

  fun createPlacementMetricsReport(properties: PlacementMetricsReportProperties, outputStream: OutputStream) {
    val timelinessEntities = timelinessEntityRepository.findAllForMonthAndYear(properties.month, properties.year)
    val tiers = TierCategory.entries

    PlacementMetricsReportGenerator(timelinessEntities, workingDayService)
      .createReport(tiers, properties)
      .writeExcel(outputStream) {
        WorkbookFactory.create(true)
      }
  }
}
