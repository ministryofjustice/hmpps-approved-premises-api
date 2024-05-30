package uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.util

import java.sql.ResultSet

interface JdbcResultSetConsumer {
  fun consume(resultSet: ResultSet)
}
