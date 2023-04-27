package uk.gov.justice.digital.hmpps.approvedpremisesapi.service

import org.apache.poi.ss.usermodel.WorkbookFactory
import org.jetbrains.kotlinx.dataframe.io.writeExcel
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BookingRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.generator.BookingsReportGenerator
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.properties.BookingsReportProperties
import java.io.OutputStream
import java.time.LocalDate

@Service
class ReportService(
  private val bookingRepository: BookingRepository
) {
  fun createBookingsReport(properties: BookingsReportProperties, outputStream: OutputStream) {
    val bookingsInScope = if (properties.year != null && properties.month != null) {
      val startOfMonth = LocalDate.of(properties.year, properties.month, 1)
      val endOfMonth = LocalDate.of(properties.year, properties.month, startOfMonth.month.length(startOfMonth.isLeapYear))
      bookingRepository.findAllByOverlappingDate(startOfMonth, endOfMonth)
    } else {
      bookingRepository.findAll()
    }

    BookingsReportGenerator()
      .createReport(bookingsInScope, properties)
      .writeExcel(outputStream) {
        WorkbookFactory.create(true)
      }
  }
}
