package uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.util

import org.springframework.jdbc.core.RowCallbackHandler

interface JdbcReportConsumer {
  fun getHeadersCallbackHandler(): (List<String>) -> Unit
  fun getRowCallbackHandler(): RowCallbackHandler
}
