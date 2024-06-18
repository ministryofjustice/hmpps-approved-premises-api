package uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.util

import com.opencsv.CSVWriterBuilder
import com.opencsv.ICSVWriter
import com.opencsv.ResultSetColumnNameHelperService
import java.io.OutputStream
import java.nio.charset.Charset
import java.sql.ResultSet

class CsvJdbcResultSetConsumer(
  private val outputStream: OutputStream,
  private val columnsToExclude: List<String> = emptyList(),
) : JdbcResultSetConsumer, AutoCloseable {
  private lateinit var writer: ICSVWriter
  private lateinit var resultSet: ResultSet

  override fun consume(resultSet: ResultSet) {
    this.resultSet = resultSet

    val columnsToExcludeLowercase = columnsToExclude.map { it.lowercase() }
    val columnsToInclude = getAllColNames(resultSet)
      .filter { !columnsToExcludeLowercase.contains(it.lowercase()) }

    val columnNameHelper = ResultSetColumnNameHelperService()
    val columnNames = columnsToInclude.toTypedArray()
    val columnHeaders = columnsToInclude.toTypedArray()
    columnNameHelper.setColumnNames(columnNames, columnHeaders)

    writer = CSVWriterBuilder(outputStream.writer(Charset.defaultCharset()))
      .withResultSetHelper(columnNameHelper)
      .build()

    val includeHeaders = true
    writer.writeAll(resultSet, includeHeaders)
  }

  private fun getAllColNames(resultSet: ResultSet): List<String> {
    val metadata = resultSet.metaData
    val cols = mutableListOf<String>()
    for (it in 1.rangeTo(metadata.columnCount)) {
      cols.add(metadata.getColumnName(it))
    }
    return cols
  }

  override fun close() {
    writer.close()
    resultSet.close()
  }
}
