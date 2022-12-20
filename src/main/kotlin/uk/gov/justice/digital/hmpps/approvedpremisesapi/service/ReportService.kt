package uk.gov.justice.digital.hmpps.approvedpremisesapi.service

import org.apache.poi.ss.usermodel.WorkbookFactory
import org.jetbrains.kotlinx.dataframe.io.writeExcel
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BookingRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.generator.BookingsReportGenerator
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.properties.BookingsReportProperties
import java.io.OutputStream

@Service
class ReportService(
  private val bookingRepository: BookingRepository
) {
  fun createBookingsReport(properties: BookingsReportProperties, outputStream: OutputStream) {
    BookingsReportGenerator()
      .createReport(bookingRepository.findAll(), properties)
      .writeExcel(outputStream) {
        WorkbookFactory.create(true)
      }
  }
}
