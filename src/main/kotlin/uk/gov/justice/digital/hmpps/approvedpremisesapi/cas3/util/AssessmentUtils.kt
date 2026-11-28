package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.util

import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.FullPersonSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SortDirection
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.Cas3AssessmentSummary

@SuppressWarnings("CyclomaticComplexMethod", "ThrowsCount")
fun List<Cas3AssessmentSummary>.sortCas3AssessmentsByName(sortDirection: SortDirection): List<Cas3AssessmentSummary> {
  val comparator = Comparator<Cas3AssessmentSummary> { a, b ->
    val ascendingCompare = compareValues((a.person as? FullPersonSummary)?.name, (b.person as? FullPersonSummary)?.name)

    when (sortDirection) {
      SortDirection.asc, null -> ascendingCompare
      SortDirection.desc -> -ascendingCompare
    }
  }

  return this.sortedWith(comparator)
}
