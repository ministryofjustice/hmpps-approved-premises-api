package uk.gov.justice.digital.hmpps.approvedpremisesapi.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.util.ReportJdbcTemplate
import javax.sql.DataSource

@Configuration
class ReportJdbcTemplateConfig(
  @Value("\${reports.jdbc-fetch-size}") val fetchSize: Int,
) {
  @Bean
  fun reportJdbcTemplate(datasource: DataSource): ReportJdbcTemplate {
    val jdbcTemplate = JdbcTemplate(datasource)
    jdbcTemplate.fetchSize = fetchSize

    return ReportJdbcTemplate(
      NamedParameterJdbcTemplate(
        jdbcTemplate,
      ),
    )
  }
}
