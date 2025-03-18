package uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.generator

import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BedEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1OutOfServiceBedReasonRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1OutOfServiceBedRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.model.Cas1OutOfServiceBedReportRow
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1ReportService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.earliestDateOf
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.getDaysUntilInclusive
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.latestDateOf
import java.time.LocalDate
import java.util.UUID

class Cas1OutOfServiceBedsReportGenerator(
  private val outOfServiceBedRepository: Cas1OutOfServiceBedRepository,
) : ReportGenerator<BedEntity, Cas1OutOfServiceBedReportRow, Cas1ReportService.MonthSpecificReportParams>(
  Cas1OutOfServiceBedReportRow::class,
) {
  override fun filter(properties: Cas1ReportService.MonthSpecificReportParams): (BedEntity) -> Boolean = {
    checkServiceType(ServiceName.approvedPremises, it.room.premises)
  }

  override val convert: BedEntity.(properties: Cas1ReportService.MonthSpecificReportParams) -> List<Cas1OutOfServiceBedReportRow> = { properties ->
    val startOfMonth = LocalDate.of(properties.year, properties.month, 1)
    val endOfMonth = LocalDate.of(properties.year, properties.month, startOfMonth.month.length(startOfMonth.isLeapYear))

    val outOfServiceBedIds = outOfServiceBedRepository.findByBedIdAndOverlappingDate(this.id, startOfMonth, endOfMonth, null)

    val outOfServiceBeds = outOfServiceBedRepository.findAllById(outOfServiceBedIds.map(UUID::fromString)).filter { it.reason.id != Cas1OutOfServiceBedReasonRepository.BED_ON_HOLD_REASON_ID }

    outOfServiceBeds.map {
      val bed = it.bed
      val room = bed.room
      val premises = room.premises

      Cas1OutOfServiceBedReportRow(
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
