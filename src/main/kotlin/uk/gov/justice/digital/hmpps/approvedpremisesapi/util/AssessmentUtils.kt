package uk.gov.justice.digital.hmpps.approvedpremisesapi.util

import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.AssessmentSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.FullPerson
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SortDirection

@SuppressWarnings("CyclomaticComplexMethod", "ThrowsCount")
fun List<AssessmentSummary>.sortByName(sortDirection: SortDirection): List<AssessmentSummary> {
  val comparator = Comparator<AssessmentSummary> { a, b ->
    val ascendingCompare = compareValues((a.person as? FullPerson)?.name, (b.person as? FullPerson)?.name)

    when (sortDirection) {
      SortDirection.asc, null -> ascendingCompare
      SortDirection.desc -> -ascendingCompare
    }
  }

  return this.sortedWith(comparator)
}
