package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1PlacementMatchingOutcomesReportRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.properties.Cas1PlacementMatchingOutcomesReportProperties
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.util.ExcelJdbcResultSetConsumer
import java.io.OutputStream

@Service
class Cas1ReportService(
  private val cas1PlacementMatchingOutcomesReportRepository: Cas1PlacementMatchingOutcomesReportRepository,
) {

  fun createPlacementMatchingOutcomesReport(properties: Cas1PlacementMatchingOutcomesReportProperties, outputStream: OutputStream) {
    ExcelJdbcResultSetConsumer().use { consumer ->
      cas1PlacementMatchingOutcomesReportRepository.generateReportRowsForExpectedArrivalMonth(
        properties.month,
        properties.year,
        consumer,
      )

      consumer.writeBufferedWorkbook(outputStream)
    }
  }
}
