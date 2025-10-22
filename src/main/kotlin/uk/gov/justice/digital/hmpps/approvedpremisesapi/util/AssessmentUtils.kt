package uk.gov.justice.digital.hmpps.approvedpremisesapi.util

import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.AssessmentSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.FullPerson
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SortDirection
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.Cas3AssessmentSummary

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

@SuppressWarnings("CyclomaticComplexMethod", "ThrowsCount")
fun List<Cas3AssessmentSummary>.sortCas3AssessmentsByName(sortDirection: SortDirection): List<Cas3AssessmentSummary> {
  val comparator = Comparator<Cas3AssessmentSummary> { a, b ->
    val ascendingCompare = compareValues((a.person as? FullPerson)?.name, (b.person as? FullPerson)?.name)

    when (sortDirection) {
      SortDirection.asc, null -> ascendingCompare
      SortDirection.desc -> -ascendingCompare
    }
  }

  return this.sortedWith(comparator)
}
