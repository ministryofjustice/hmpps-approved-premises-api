package uk.gov.justice.digital.hmpps.approvedpremisesapi.migration.cas1

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.zalando.problem.AbstractThrowableProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1OutOfServiceBedEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1OutOfServiceBedRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.migration.MigrationJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1OutOfServiceBedService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.ensureEntityFromCasResultIsSuccess
import java.time.LocalDate

class Cas1TruncateOosbMigrationJob(
  private val cas1OutOfServiceBedRepository: Cas1OutOfServiceBedRepository,
  private val outOfServiceBedService: Cas1OutOfServiceBedService,
) : MigrationJob() {
  override val shouldRunInTransaction = true

  var log: Logger = LoggerFactory.getLogger(this::class.java)

  override fun process() {
    val recordsToUpdate = cas1OutOfServiceBedRepository
      .findAllActive()
      .filter { oosb -> oosb.bed.endDate?.isBefore(oosb.endDate) == true }

    log.info("Have ${recordsToUpdate.size} oosb records to update")

    recordsToUpdate.forEach { oosb ->
      truncateRecord(
        oosb = oosb,
        newEndDate = oosb.bed.endDate!!.minusDays(1),
      )
    }
  }

  private fun truncateRecord(oosb: Cas1OutOfServiceBedEntity, newEndDate: LocalDate) {
    val bed = oosb.bed
    log.info("Truncating ooosb record '${oosb.id}' for bed '${bed.name}' in premises '${bed.room.premises.name}' to end on '$newEndDate'")

    val result = outOfServiceBedService.updateOutOfServiceBed(
      outOfServiceBedId = oosb.id,
      startDate = oosb.startDate,
      endDate = newEndDate,
      reasonId = oosb.reason.id,
      referenceNumber = null,
      notes = "End date has been automatically updated by application support as the bed has been removed as of ${bed.endDate}",
    )

    try {
      ensureEntityFromCasResultIsSuccess(result)
    } catch (problem: AbstractThrowableProblem) {
      log.info("There was a problem truncating the record", problem)
    }
  }
}
