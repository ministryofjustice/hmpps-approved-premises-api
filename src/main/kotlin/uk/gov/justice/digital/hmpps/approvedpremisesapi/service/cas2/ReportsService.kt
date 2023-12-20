package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas2

import org.apache.poi.ss.usermodel.WorkbookFactory
import org.jetbrains.kotlinx.dataframe.api.toDataFrame
import org.jetbrains.kotlinx.dataframe.io.writeExcel
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.model.Cas2ExampleMetricsRow
import java.io.OutputStream

@Service
class ReportsService {
  fun createCas2ExampleReport(outputStream: OutputStream) {
    // TODO replace this with a real report
    val exampleData = listOf(Cas2ExampleMetricsRow(id = "123", data = "example"))

    exampleData.toDataFrame()
      .writeExcel(outputStream) {
        WorkbookFactory.create(true)
      }
  }
}
