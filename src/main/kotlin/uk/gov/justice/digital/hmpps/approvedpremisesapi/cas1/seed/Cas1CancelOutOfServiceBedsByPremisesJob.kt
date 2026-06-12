package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.seed

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.jobs.seed.SeedException
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.jobs.seed.SeedJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.jobs.seed.trimToNull
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.PremisesService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1OutOfServiceBedService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.ensureEntityFromCasResultIsSuccess
import java.util.UUID

@Component
class Cas1CancelOutOfServiceBedsByPremisesJob(
  private val cas1OutOfServiceBedService: Cas1OutOfServiceBedService,
  private val premisesService: PremisesService,
) : SeedJob<Cas1CancelOutOfServiceBedsByPremisesCsvRow>(
  requiredHeaders = setOf(
    "premisesId",
    "notes",
  ),
) {

  private val log = LoggerFactory.getLogger(this::class.java)

  override fun deserializeRow(columns: Map<String, String>) = Cas1CancelOutOfServiceBedsByPremisesCsvRow(
    premisesId = UUID.fromString(columns["premisesId"]!!.trim()),
    notes = columns["notes"].trimToNull(),
  )

  override fun processRow(row: Cas1CancelOutOfServiceBedsByPremisesCsvRow) {
    premisesService.getPremises(row.premisesId)
      ?: throw SeedException("No Premises with ID ${row.premisesId} exists.")

    val activeOutOfServiceBeds = cas1OutOfServiceBedService.getActiveOutOfServiceBedsForPremisesId(row.premisesId)

    activeOutOfServiceBeds.forEach { outOfServiceBed ->
      val result = cas1OutOfServiceBedService.cancelOutOfServiceBed(outOfServiceBed, row.notes)
      ensureEntityFromCasResultIsSuccess(result)
    }

    log.info("Cancelled ${activeOutOfServiceBeds.size} out of service beds for premises ID: ${row.premisesId}")
  }
}

data class Cas1CancelOutOfServiceBedsByPremisesCsvRow(
  val premisesId: UUID,
  val notes: String?,
)
