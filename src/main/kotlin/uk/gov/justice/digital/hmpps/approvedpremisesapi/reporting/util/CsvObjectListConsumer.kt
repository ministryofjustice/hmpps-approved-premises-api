package uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.util

import com.opencsv.CSVWriterBuilder
import com.opencsv.bean.CsvBindByName
import com.opencsv.bean.CsvToBeanBuilder
import com.opencsv.bean.HeaderColumnNameMappingStrategy
import com.opencsv.bean.StatefulBeanToCsvBuilder
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.model.FutureBookingsReportRow
import java.io.OutputStream
import java.io.StringReader
import java.nio.charset.Charset
import java.util.Arrays
import java.util.Objects
import java.util.stream.Collectors

class CsvObjectListConsumer(
  outputStream: OutputStream,
) {
  private val writer = CSVWriterBuilder(outputStream.writer(Charset.defaultCharset()))
    .build()

  fun consume(rows: List<FutureBookingsReportRow>) {
    val headerMappingStrategy = HeaderColumnNameMappingStrategy<FutureBookingsReportRow>().apply {
      type = FutureBookingsReportRow::class.java
    }

    val builder = StatefulBeanToCsvBuilder<FutureBookingsReportRow>(writer)
      .withMappingStrategy(headerMappingStrategy)
      .build()

    val headerLine = Arrays.stream(FutureBookingsReportRow::class.java.getDeclaredFields())
      .map { field -> field.getAnnotation(CsvBindByName::class.java) }
      .filter(Objects::nonNull)
      .map { obj: CsvBindByName -> obj.column }
      .collect(Collectors.joining(","))

    StringReader(headerLine).use { reader ->
      val header = CsvToBeanBuilder<FutureBookingsReportRow>(reader)
        .withType(FutureBookingsReportRow::class.java)
        .withMappingStrategy(headerMappingStrategy)
        .build()

      for (csvRow in header) {
        builder.write(csvRow)
      }
    }

    builder.write(rows)
    writer.close()
  }
}
