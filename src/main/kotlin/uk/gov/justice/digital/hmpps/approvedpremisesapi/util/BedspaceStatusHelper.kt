package uk.gov.justice.digital.hmpps.approvedpremisesapi.util

import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.Cas3BedspaceStatus
import java.time.LocalDate

object BedspaceStatusHelper {

  fun isCas3BedspaceActive(bedspaceEndDate: LocalDate?, archiveEndDate: LocalDate): Boolean = bedspaceEndDate == null || bedspaceEndDate > archiveEndDate
  fun isCas3BedspaceOnline(startDate: LocalDate?, endDate: LocalDate?) = (startDate == null || startDate <= LocalDate.now()) && (endDate == null || endDate > LocalDate.now())
  fun isCas3BedspaceUpcoming(startDate: LocalDate?) = startDate?.isAfter(LocalDate.now()) ?: false
  fun isCas3BedspaceArchived(endDate: LocalDate?) = endDate != null && endDate <= LocalDate.now()
  fun getBedspaceStatus(startDate: LocalDate?, endDate: LocalDate?) = when {
    this.isCas3BedspaceUpcoming(startDate) -> Cas3BedspaceStatus.upcoming
    this.isCas3BedspaceArchived(endDate) -> Cas3BedspaceStatus.archived
    else -> Cas3BedspaceStatus.online
  }
}
