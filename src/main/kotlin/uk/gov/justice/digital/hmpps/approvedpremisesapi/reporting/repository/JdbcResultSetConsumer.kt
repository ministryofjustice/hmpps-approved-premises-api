package uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.repository

import java.sql.ResultSet

interface JdbcResultSetConsumer {
  fun consume(resultSet: ResultSet)
}
