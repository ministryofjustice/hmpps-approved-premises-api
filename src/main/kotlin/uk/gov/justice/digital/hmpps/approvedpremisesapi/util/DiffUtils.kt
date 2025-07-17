package uk.gov.justice.digital.hmpps.approvedpremisesapi.util

import org.javers.common.string.PrettyValuePrinter
import org.javers.core.Javers
import org.javers.core.JaversBuilder
import org.javers.core.diff.ListCompareAlgorithm

object DiffUtils {

  private val javers: Javers = JaversBuilder.javers().withListCompareAlgorithm(ListCompareAlgorithm.AS_SET).build()

  /**
   * Provide a simple comma seperated list of property changes between two objects,
   * intended to be human-readable for to end users
   *
   * Returns null if there are no changes
   */
  fun <T> simplePrettyPrint(priorState: T, newState: T): String? {
    val diff = javers.compare(priorState, newState)

    if (!diff.hasChanges()) {
      return null
    }

    val b = StringBuilder()

    diff.groupByObject()[0].propertyChanges.forEachIndexed { i, change ->
      if (i > 0) {
        b.append(", ")
      }
      b.append(change!!.prettyPrint(PrettyValuePrinter.getDefault()))
    }

    return b.toString()
  }
}
