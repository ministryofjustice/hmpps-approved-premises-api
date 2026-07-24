package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.seed

import org.slf4j.LoggerFactory
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.jobs.seed.SeedColumns
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.jobs.seed.SeedJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas1.ApprovedPremisesRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas1.Cas1BedRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1OutOfServiceBedService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.ensureEntityFromCasResultIsSuccess
import java.time.LocalDate
import java.util.UUID

@Component
class Cas1ClosePremisesSeedJob(
  private val approvedPremisesRepository: ApprovedPremisesRepository,
  private val cas1BedRepository: Cas1BedRepository,
  private val cas1OutOfServiceBedService: Cas1OutOfServiceBedService,
) : SeedJob<Cas1ClosePremisesSeedJob.CsvRow>(
  requiredHeaders = setOf(
    "premises_id",
    "closure_date",
    "notes",
  ),
) {

  private val log = LoggerFactory.getLogger(this::class.java)

  override fun deserializeRow(columns: Map<String, String>): CsvRow {
    val seedColumns = SeedColumns(columns)

    return CsvRow(
      premisesId = seedColumns.getUuidOrNull("premises_id")!!,
      closureDate = seedColumns.getLocalDateOrNull("closure_date")!!,
      notes = seedColumns.getStringOrNull("notes"),
    )
  }

  override fun processRow(row: CsvRow) {
    val premisesId = row.premisesId
    val closureDate = row.closureDate

    log.info("Closing premises id $premisesId on $closureDate")

    val approvedPremises = approvedPremisesRepository.findByIdOrNull(premisesId)
      ?: error("Could not find approved premises with id $premisesId")

    val beds = cas1BedRepository.findByRoomPremisesId(premisesId)
    log.info("Setting end date for ${beds.size} beds for premises id $premisesId to $closureDate")
    beds.forEach { bed ->
      bed.endDate = closureDate
      cas1BedRepository.save(bed)
    }

    val activeOutOfServiceBeds = cas1OutOfServiceBedService.getActiveOutOfServiceBedsForPremisesId(premisesId)
    log.info("Setting end date for ${activeOutOfServiceBeds.size} active out of service beds for premises id $premisesId to $closureDate")
    activeOutOfServiceBeds.forEach { outOfServiceBed ->
      val result = cas1OutOfServiceBedService.updateOutOfServiceBed(
        outOfServiceBedId = outOfServiceBed.id,
        startDate = outOfServiceBed.startDate,
        endDate = closureDate,
        reasonId = outOfServiceBed.reason.id,
        referenceNumber = outOfServiceBed.referenceNumber,
        notes = row.notes ?: outOfServiceBed.notes ?: "Site Closure",
        createdBy = null,
      )
      ensureEntityFromCasResultIsSuccess(result)
    }

    log.info("Updating allow new space bookings for premises id $premisesId to false")
    approvedPremises.allowNewSpaceBookings = false
    approvedPremisesRepository.save(approvedPremises)
  }

  data class CsvRow(
    val premisesId: UUID,
    val closureDate: LocalDate,
    val notes: String?,
  )
}
