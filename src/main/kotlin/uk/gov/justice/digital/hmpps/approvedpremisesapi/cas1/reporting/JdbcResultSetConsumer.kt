package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.reporting

import java.sql.ResultSet

interface JdbcResultSetConsumer {
  fun consume(resultSet: ResultSet)
}
