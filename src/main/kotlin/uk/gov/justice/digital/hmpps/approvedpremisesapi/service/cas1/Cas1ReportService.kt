package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1

import org.apache.poi.ss.usermodel.WorkbookFactory
import org.jetbrains.kotlinx.dataframe.io.writeExcel
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementMatchingOutcomesEntityReportRowRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.generator.Cas1PlacementMatchingOutcomesReportGenerator
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.properties.Cas1PlacementMatchingOutcomesReportProperties
import java.io.OutputStream

@Service
class Cas1ReportService(
  private val placementMatchingOutcomesEntityReportRowRepository: PlacementMatchingOutcomesEntityReportRowRepository,
) {

  fun createPlacementMatchingOutcomesReport(properties: Cas1PlacementMatchingOutcomesReportProperties, outputStream: OutputStream) {
    Cas1PlacementMatchingOutcomesReportGenerator()
      .createReport(placementMatchingOutcomesEntityReportRowRepository.generateReportRowsForExpectedArrivalMonth(properties.month, properties.year), properties)
      .writeExcel(outputStream) {
        WorkbookFactory.create(true)
      }
  }
}
