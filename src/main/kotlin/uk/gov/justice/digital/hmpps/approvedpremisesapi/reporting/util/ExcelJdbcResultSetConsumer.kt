package uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.util

import org.apache.poi.ss.usermodel.CellStyle
import org.apache.poi.xssf.streaming.SXSSFWorkbook
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
class ExcelJdbcResultSetConsumer(
  private val columnsToExclude: List<String> = emptyList(),
) : JdbcResultSetConsumer,
  AutoCloseable {
  private val workbook = SXSSFWorkbook()
  private val sheet = workbook.createSheet("sheet0")
  private var currentRow = 1
  private var columnToWrite: List<Int> = emptyList()

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
    val columnsToExcludeLowercase = columnsToExclude.map { it.lowercase() }

    val headers = (1..resultSet.metaData.columnCount).map { col ->
      resultSet.metaData.getColumnName(col)
    }

    columnToWrite = headers
      .mapIndexed { index, columnName -> index to columnName }
      .filter { (_, columnName) -> columnName.lowercase() !in columnsToExcludeLowercase }
      .map { (index, _) -> index + 1 }

    val headerRow = sheet.createRow(0)
    columnToWrite.forEachIndexed { index, colIndex ->
      val header = resultSet.metaData.getColumnName(colIndex)
      headerRow.createCell(index).setCellValue(header)
    }
  }

  private val dateCellStyle: CellStyle = createDateCellStyle()

  private fun addRow(resultSet: ResultSet) {
    val row = sheet.createRow(currentRow)

    columnToWrite.forEachIndexed { index, colIndex ->
      when (resultSet.metaData.getColumnType(colIndex)) {
        Types.INTEGER -> resultSet.getInt(colIndex).takeIf { !resultSet.wasNull() }
          ?.let {
            row.createCell(index).setCellValue(it.toDouble())
          }
        Types.DATE -> resultSet.getDate(colIndex)?.toLocalDate()
          ?.let {
            row.createCell(index).setCellValue(it)
            row.getCell(index).cellStyle = dateCellStyle
          }
        Types.DOUBLE -> resultSet.getDouble(colIndex).takeIf { !resultSet.wasNull() }
          ?.let {
            row.createCell(index).setCellValue(it)
          }
        else -> resultSet.getString(colIndex)?.let {
          row.createCell(index).setCellValue(it)
        }
      }
    }
    currentRow++
  }

  override fun close() {
    workbook.close()
  }

  private fun createDateCellStyle(): CellStyle {
    val createHelper = workbook.creationHelper
    val dateCellStyle = workbook.createCellStyle()
    dateCellStyle.dataFormat = createHelper.createDataFormat().getFormat("dd.mm.yyyy")
    return dateCellStyle
  }
}
