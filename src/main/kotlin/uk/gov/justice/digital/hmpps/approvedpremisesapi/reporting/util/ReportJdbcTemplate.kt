package uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.util

import org.springframework.jdbc.core.ResultSetExtractor
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import java.sql.ResultSet

data class ReportJdbcTemplate(val namedParameterJdbcTemplate: NamedParameterJdbcTemplate) {
  fun query(
    sql: String,
    paramMap: Map<String, *>,
    jdbcReportRowConsumer: JdbcResultSetConsumer,
  ) {
    namedParameterJdbcTemplate.query(
      sql,
      paramMap,
      ResultSetExtractor { rs: ResultSet -> jdbcReportRowConsumer.consume(rs) },
    )
  }
}
