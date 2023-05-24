package uk.gov.justice.digital.hmpps.approvedpremisesapi.util

data class LogEntry(
  val message: String,
  val level: String,
  val throwable: Throwable?
)
