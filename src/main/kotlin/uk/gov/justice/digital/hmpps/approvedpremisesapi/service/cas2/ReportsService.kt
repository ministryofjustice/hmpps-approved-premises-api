package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas2

import org.apache.poi.ss.usermodel.WorkbookFactory
import org.jetbrains.kotlinx.dataframe.api.toDataFrame
import org.jetbrains.kotlinx.dataframe.io.writeExcel
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2SubmittedApplicationReportRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.model.Cas2ExampleMetricsRow
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.model.cas2.SubmittedApplicationReportRow
import java.io.OutputStream

@Service
class ReportsService(
  private val submittedApplicationReportRepository: Cas2SubmittedApplicationReportRepository,
) {
  fun createCas2ExampleReport(outputStream: OutputStream) {
    // TODO replace this with a real report
    val exampleData = listOf(Cas2ExampleMetricsRow(id = "123", data = "example"))

    exampleData.toDataFrame()
      .writeExcel(outputStream) {
        WorkbookFactory.create(true)
      }
  }

  fun createSubmittedApplicationsReport(outputStream: OutputStream) {
    val reportData = submittedApplicationReportRepository.generateSubmittedApplicationReportRows().map { row ->
      SubmittedApplicationReportRow(
        eventId = row.getId(),
        applicationId = row.getApplicationId(),
        personCrn = row.getPersonCrn(),
        personNoms = row.getPersonNoms(),
        submittedBy = row.getSubmittedBy(),
        submittedAt = row.getSubmittedAt(),
      )
    }

    reportData.toDataFrame()
      .writeExcel(outputStream) {
        WorkbookFactory.create(true)
      }
  }
}
