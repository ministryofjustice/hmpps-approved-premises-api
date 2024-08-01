package uk.gov.justice.digital.hmpps.approvedpremisesapi.seed

import java.util.UUID

abstract class SeedJob<RowType> (
  val id: UUID = UUID.randomUUID(),
  val fileName: String,
  val requiredHeaders: Set<String>? = null,
) {
  init {
    if (fileName.contains("/") || fileName.contains("\\")) {
      throw RuntimeException("Filename must be just the filename of a .csv file in the /seed directory, e.g. for /seed/upload.csv, just `upload` should be supplied")
    }
  }

  open fun verifyPresenceOfRequiredHeaders(headers: Set<String>) {
    if (requiredHeaders == null) return

    val missingHeaders = requiredHeaders - headers

    if (missingHeaders.any()) {
      throw RuntimeException("required headers: $missingHeaders")
    }
  }

  abstract fun deserializeRow(columns: Map<String, String>): RowType
  abstract fun processRow(row: RowType)
}

class SeedException(message: String) : RuntimeException(message)
