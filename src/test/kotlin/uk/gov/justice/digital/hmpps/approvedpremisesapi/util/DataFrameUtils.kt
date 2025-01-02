package uk.gov.justice.digital.hmpps.approvedpremisesapi.util

import org.jetbrains.kotlinx.dataframe.DataFrame
import org.jetbrains.kotlinx.dataframe.api.dataFrameOf

object DataFrameUtils {

  fun createNameValueDataFrame(vararg rows: String): DataFrame<*> {
    val list = rows.toList()

    check(list.size >= 2) { "Should have at least two entries" }
    check(list.size % 2 == 0) { "Should contain an even number of entries" }

    return dataFrameOf(list.subList(0, 2), list.subList(2, rows.size))
  }
}
