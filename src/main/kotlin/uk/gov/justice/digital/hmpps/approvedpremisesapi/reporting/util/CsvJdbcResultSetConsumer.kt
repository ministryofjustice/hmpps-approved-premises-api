package uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.util

import com.opencsv.CSVWriterBuilder
import java.io.OutputStream
import java.nio.charset.Charset
import java.sql.ResultSet

class CsvJdbcResultSetConsumer(val outputStream: OutputStream) : JdbcResultSetConsumer, AutoCloseable {
  var writer = CSVWriterBuilder(outputStream.writer(Charset.defaultCharset())).build()

  override fun consume(resultSet: ResultSet) {
    val includeHeaders = true
    writer.writeAll(resultSet, includeHeaders)
  }

  override fun close() {
    writer.close()
  }
}
