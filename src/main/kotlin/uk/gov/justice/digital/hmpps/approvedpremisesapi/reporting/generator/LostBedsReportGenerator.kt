package uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.generator

import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BedEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas3.Cas3LostBedsRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.model.LostBedReportRow
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.properties.LostBedReportProperties
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.earliestDateOf
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.getDaysUntilInclusive
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.latestDateOf
import java.time.LocalDate

class LostBedsReportGenerator(
  private val lostBedsRepository: Cas3LostBedsRepository,
) : ReportGenerator<BedEntity, LostBedReportRow, LostBedReportProperties>(LostBedReportRow::class) {
  override fun filter(properties: LostBedReportProperties): (BedEntity) -> Boolean = {
    checkServiceType(properties.serviceName, it.room.premises) &&
      (properties.probationRegionId == null || it.room.premises.probationRegion.id == properties.probationRegionId)
  }

  override val convert: BedEntity.(properties: LostBedReportProperties) -> List<LostBedReportRow> = { properties ->
    val startOfMonth = LocalDate.of(properties.year, properties.month, 1)
    val endOfMonth = LocalDate.of(properties.year, properties.month, startOfMonth.month.length(startOfMonth.isLeapYear))

    val lostBeds = lostBedsRepository.findAllByOverlappingDateForBed(startOfMonth, endOfMonth, this)

    lostBeds.map {
      val bed = it.bed
      val room = bed.room
      val premises = room.premises

      LostBedReportRow(
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
