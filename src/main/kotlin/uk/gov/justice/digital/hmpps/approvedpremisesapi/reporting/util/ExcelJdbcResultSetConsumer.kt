package uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.util

import com.google.common.base.CaseFormat
import org.apache.poi.ss.usermodel.WorkbookFactory
import org.jetbrains.kotlinx.dataframe.DataFrame
import org.jetbrains.kotlinx.dataframe.io.writeExcel
import java.io.OutputStream
import java.sql.ResultSet
import java.sql.Types

/**
 * An implementation of [JdbcResultSetConsumer] that streams the result to an XSLX document.
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
 * Given (2), it's recommended that the SQL is written to covert any non string types into a string in
 * the required format (e.g. format date/timestamps in the SQL as they should appear in the XLSX)
 */
class ExcelJdbcResultSetConsumer : JdbcResultSetConsumer, AutoCloseable {
  private val workbook = WorkbookFactory.create(true)
  private val sheet = workbook.createSheet("sheet0")
  private var currentRow = 1
  private var columnCount = 0

  fun writeBufferedWorkbook(outputStream: OutputStream) {
    workbook.write(outputStream)
  }

  override fun consume(resultSet: ResultSet) {
    resultSet.use {
      addHeaders(resultSet)
      while (resultSet.next()) {
        addRow(resultSet)
      }
    }
  }

  private fun addHeaders(resultSet: ResultSet) {
    val headers = 1.rangeTo(resultSet.metaData.columnCount).map { col ->
      resultSet.metaData.getColumnName(col)
    }

    val headerRow = sheet.createRow(0)
    headers.forEachIndexed { index, header ->
      headerRow
        .createCell(index)
        .setCellValue(CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.LOWER_CAMEL, header))
    }

    columnCount = headers.size
  }

  private fun addRow(resultSet: ResultSet) {
    val row = sheet.createRow(currentRow)

    for (col in 1.rangeTo(columnCount)) {
      when (resultSet.metaData.getColumnType(col)) {
        Types.INTEGER -> {
          val value = resultSet.getInt(col)
          if (!resultSet.wasNull()) {
            row.createCell(col - 1).setCellValue(value.toDouble())
          }
        }
        else -> {
          val value = resultSet.getString(col)
          if (!resultSet.wasNull()) {
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
