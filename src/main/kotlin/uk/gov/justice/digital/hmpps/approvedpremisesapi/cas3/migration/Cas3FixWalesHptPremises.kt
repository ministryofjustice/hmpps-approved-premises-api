package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.migration

import org.springframework.stereotype.Component
import org.springframework.transaction.support.TransactionTemplate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3PremisesRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.migration.MigrationInBatchesJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.migration.MigrationLogger
import java.time.LocalDate
import java.util.UUID

@Component
class Cas3FixWalesHptPremises(
  private val cas3PremisesRepository: Cas3PremisesRepository,
  private val migrationLogger: MigrationLogger,
  transactionTemplate: TransactionTemplate,
) : MigrationInBatchesJob(migrationLogger, transactionTemplate) {
  override val shouldRunInTransaction = false

  override fun process(pageSize: Int) {
    val premises = cas3PremisesRepository.findAllById(listOf(UUID.fromString("1cf35a14-553e-435b-8b88-eeebdf4bbc28")))
    migrationLogger.info("Found ${premises.size} Wales HPT premises.")
    if (premises.isNotEmpty() && premises.size == 1) {
      val premisesToFix = premises[0]
      if (premisesToFix.startDate == LocalDate.parse("2025-12-05")) {
        premisesToFix.startDate = LocalDate.parse("2025-12-04")
        cas3PremisesRepository.save(premisesToFix)
        migrationLogger.info("Fixed the Wales HPT premises.")
      } else {
        migrationLogger.info("The Wales HPT premises start date is already fixed.")
      }
    } else {
      migrationLogger.info("Unable to find the Wales HPT premises to fix.")
    }
  }
}
