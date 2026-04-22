package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.seed

import org.slf4j.LoggerFactory
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1OutOfServiceBedReasonRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.SeedException
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.SeedJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.trimToNull
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.PremisesService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1OutOfServiceBedService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.ensureEntityFromCasResultIsSuccess
import java.time.LocalDate
import java.util.UUID

@Component
class Cas1UpdateOutOfServiceBedsByPremisesJob(
  private val cas1OutOfServiceBedService: Cas1OutOfServiceBedService,
  private val premisesService: PremisesService,
  private val cas1OutOfServiceBedReasonRepository: Cas1OutOfServiceBedReasonRepository,
) : SeedJob<Cas1UpdateOutOfServiceBedsByPremisesCsvRow>(
  requiredHeaders = setOf(
    "premises_id",
    "end_date",
    "notes",
    "reason_id",
  ),
) {

  private val log = LoggerFactory.getLogger(this::class.java)

  override fun deserializeRow(columns: Map<String, String>) = Cas1UpdateOutOfServiceBedsByPremisesCsvRow(
    premisesId = UUID.fromString(columns["premises_id"]!!.trim()),
    endDate = columns["end_date"]?.trimToNull()?.let { LocalDate.parse(it) },
    notes = columns["notes"].trimToNull(),
    reasonId = columns["reason_id"]?.trimToNull()?.let { UUID.fromString(it) },
  )

  override fun processRow(row: Cas1UpdateOutOfServiceBedsByPremisesCsvRow) {
    premisesService.getPremises(row.premisesId)
      ?: throw SeedException("No Premises with ID ${row.premisesId} exists.")

    val reason = row.reasonId?.let {
      cas1OutOfServiceBedReasonRepository.findByIdOrNull(it)
        ?: throw SeedException("No Out of Service Bed Reason with ID $it exists.")
    }

    val activeOutOfServiceBeds = cas1OutOfServiceBedService.getActiveOutOfServiceBedsForPremisesId(row.premisesId)

    activeOutOfServiceBeds.forEach { outOfServiceBed ->
      val result = cas1OutOfServiceBedService.updateOutOfServiceBed(
        outOfServiceBedId = outOfServiceBed.id,
        startDate = outOfServiceBed.startDate,
        endDate = row.endDate ?: outOfServiceBed.endDate,
        reasonId = reason?.id ?: outOfServiceBed.reason.id,
        referenceNumber = outOfServiceBed.referenceNumber,
        notes = row.notes,
        createdBy = null,
      )
      ensureEntityFromCasResultIsSuccess(result)
    }

    log.info("Updated ${activeOutOfServiceBeds.size} out of service beds for premises ID: ${row.premisesId}")
  }
}

data class Cas1UpdateOutOfServiceBedsByPremisesCsvRow(
  val premisesId: UUID,
  val endDate: LocalDate?,
  val notes: String?,
  val reasonId: UUID?,
)
