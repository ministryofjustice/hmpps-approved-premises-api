package uk.gov.justice.digital.hmpps.approvedpremisesapi.seed

import java.util.UUID

abstract class SeedJob<RowType> (
  val id: UUID = UUID.randomUUID(),
  val fileName: String,
) {
  init {
    if (fileName.contains("/") || fileName.contains("\\") || fileName.contains(".")) {
      throw RuntimeException("Filename must be just the filename of a .csv file in the /seed directory, e.g. for /seed/upload.csv, just `upload` should be supplied")
    }
  }

  abstract fun verifyPresenceOfRequiredHeaders(headers: Set<String>)
  abstract fun deserializeRow(columns: Map<String, String>): RowType
  abstract fun processRow(row: RowType)
}
