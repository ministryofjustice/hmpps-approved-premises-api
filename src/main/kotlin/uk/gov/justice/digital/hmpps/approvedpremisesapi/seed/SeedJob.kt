package uk.gov.justice.digital.hmpps.approvedpremisesapi.seed

import org.jetbrains.kotlinx.dataframe.DataFrame
import java.util.UUID

abstract class SeedJob<RowType>(
  val requiredHeaders: Set<String>? = null,
  val runInTransaction: Boolean = true,
) {
  open fun verifyPresenceOfRequiredHeaders(headers: Set<String>) {
    if (requiredHeaders == null) return

    val missingHeaders = requiredHeaders - headers

    if (missingHeaders.any()) {
      throw RuntimeException("required headers: $missingHeaders")
    }
  }

  open fun preSeed() {
    // by default do nothing
  }
  open fun postSeed() {
    // by default do nothing
  }
  abstract fun deserializeRow(columns: Map<String, String>): RowType
  abstract fun processRow(row: RowType)
}

abstract class ExcelSeedJob(
  val fileName: String,
  val premisesId: UUID,
  val sheetName: String,
) {
  init {
    if (fileName.contains("/") || fileName.contains("\\")) {
      throw RuntimeException("Filename must be just the filename of a .xlsx file in the /seed directory, e.g. for /seed/upload.xlsx, just `upload` should be supplied")
    }
  }

  abstract fun processDataFrame(dataFrame: DataFrame<*>)
}

class SeedException(message: String) : RuntimeException(message)
