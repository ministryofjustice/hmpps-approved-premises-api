package uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.util

import com.google.common.base.CaseFormat
import org.apache.poi.ss.usermodel.WorkbookFactory
import org.jetbrains.kotlinx.dataframe.DataFrame
import org.jetbrains.kotlinx.dataframe.io.writeExcel
import org.springframework.jdbc.core.RowCallbackHandler
import java.io.OutputStream
import java.sql.Types

/**
 * An implementation of [JdbcReportConsumer] that streams the result to an XSLX document.
 *
 * This can be used as a memory-efficient alternative to the [DataFrame] approach that uses
 * data classes, noting that:
 *
 * 1. The Excel Document _is_ still loaded into memory once as we can't stream XLSX directly
 * to an [OutputStream]
 * 2. All column values other than null and Integer are written as a String type, whereas [DataFrame] maps
 * Kotlin data types into corresponding Excel types. This could be improved as required, consulting
 * DataFrame's [DataFrame.writeExcel] function for an example of how data types could be mapped.
 *
 * Given (2), it's recommended that the SQL is written to format any non string types as required
 * (e.g. format date/timestamps as they should appear in the XLSX)
 */
class ExcelJdbcReportConsumer : JdbcReportConsumer, AutoCloseable {
  private val workbook = WorkbookFactory.create(true)
  private val sheet = workbook.createSheet("sheet0")
  private var currentRow = 1
  private var columnCount = 0

  fun write(outputStream: OutputStream) {
    workbook.write(outputStream)
  }

  override fun getHeadersCallbackHandler(): (List<String>) -> Unit = {
      headers ->
    val headerRow = sheet.createRow(0)
    headers.forEachIndexed { index, header ->
      headerRow
        .createCell(index)
        .setCellValue(CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.LOWER_CAMEL, header))
    }

    columnCount = headers.size
  }

  override fun getRowCallbackHandler() = RowCallbackHandler { rs ->
    val row = sheet.createRow(currentRow)

    for (col in 1.rangeTo(columnCount)) {
      when (rs.metaData.getColumnType(col)) {
        Types.INTEGER -> {
          val value = rs.getInt(col)
          if (!rs.wasNull()) {
            row.createCell(col - 1).setCellValue(value.toDouble())
          }
        }
        else -> {
          val value = rs.getString(col)
          if (!rs.wasNull()) {
            row.createCell(col - 1).setCellValue(value)
          }
        }
      }
    }
    currentRow += 1
  }

  override fun close() {
    workbook.close()
  }
}
