package uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.generator

import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BedEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas3.Cas3VoidBedspacesRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.model.VoidBedspaceReportRow
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.properties.VoidBedspaceReportProperties
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.earliestDateOf
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.getDaysUntilInclusive
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.latestDateOf
import java.time.LocalDate

class VoidBedspacesReportGenerator(
  private val cas3VoidBedspacesRepository: Cas3VoidBedspacesRepository,
) : ReportGenerator<BedEntity, VoidBedspaceReportRow, VoidBedspaceReportProperties>(VoidBedspaceReportRow::class) {
  override fun filter(properties: VoidBedspaceReportProperties): (BedEntity) -> Boolean = {
    checkServiceType(properties.serviceName, it.room.premises) &&
      (properties.probationRegionId == null || it.room.premises.probationRegion.id == properties.probationRegionId)
  }

  override val convert: BedEntity.(properties: VoidBedspaceReportProperties) -> List<VoidBedspaceReportRow> = { properties ->
    val startOfMonth = LocalDate.of(properties.year, properties.month, 1)
    val endOfMonth = LocalDate.of(properties.year, properties.month, startOfMonth.month.length(startOfMonth.isLeapYear))

    val voidBedspaces = cas3VoidBedspacesRepository.findAllByOverlappingDateForBedspace(startOfMonth, endOfMonth, this)

    voidBedspaces.map {
      val bed = it.bed
      val room = bed.room
      val premises = room.premises

      VoidBedspaceReportRow(
        roomName = room.name,
        bedName = bed.name,
        id = it.id.toString(),
        workOrderId = it.referenceNumber,
        region = premises.probationRegion.name,
        ap = premises.name,
        reason = it.reason.name,
        startDate = it.startDate,
        endDate = it.endDate,
        lengthDays = latestDateOf(startOfMonth, it.startDate).getDaysUntilInclusive(earliestDateOf(endOfMonth, it.endDate)).size,
      )
    }
  }
}
