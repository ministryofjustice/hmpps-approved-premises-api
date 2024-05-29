package uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.util

import org.springframework.jdbc.core.ResultSetExtractor
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import java.sql.ResultSet

data class ReportJdbcTemplate(val namedParameterJdbcTemplate: NamedParameterJdbcTemplate) {
  fun query(sql: String, paramMap: Map<String, *>, jdbcReportConsumer: JdbcReportConsumer) {
    var headersProvided = false

    val resultSetExtractor = ResultSetExtractor { rs: ResultSet ->
      rs.use {
        if (!headersProvided) {
          val headers = 1.rangeTo(rs.metaData.columnCount).map { col ->
            rs.metaData.getColumnName(col)
          }
          jdbcReportConsumer.getHeadersCallbackHandler().invoke(headers)
          headersProvided = true
        }

        while (rs.next()) {
          jdbcReportConsumer.getRowCallbackHandler().processRow(rs)
        }
      }
    }

    namedParameterJdbcTemplate.query(
      sql,
      paramMap,
      resultSetExtractor,
    )
  }
}
