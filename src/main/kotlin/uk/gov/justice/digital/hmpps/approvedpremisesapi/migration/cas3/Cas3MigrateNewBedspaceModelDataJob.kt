package uk.gov.justice.digital.hmpps.approvedpremisesapi.migration.cas3

import org.springframework.stereotype.Component
import org.springframework.transaction.support.TransactionTemplate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PremisesRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationPremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas3.Cas3PremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas3.Cas3PremisesRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.migration.MigrationInBatchesJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.migration.MigrationLogger
import java.util.*

@Component
class Cas3MigrateNewBedspaceModelDataJob(
  private val premisesRepository: PremisesRepository,
  private val cas3PremisesRepository: Cas3PremisesRepository,
  private val migrationLogger: MigrationLogger,
  transactionTemplate: TransactionTemplate,
) : MigrationInBatchesJob(migrationLogger, transactionTemplate) {
  override val shouldRunInTransaction: Boolean = java.lang.Boolean.FALSE

  override fun process(pageSize: Int) {
    migrationLogger.info("Starting migration process...")
    val premises = getAllCas3Premises()
    super.processInBatches(premises, batchSize = 100) { batchIds ->
      migrateDataToNewBedspaceModelTables(batchIds)
    }
    migrationLogger.info("Completed migration process...")
  }

  private fun getAllCas3Premises(): List<UUID> = premisesRepository.findAllCas3Premises()

  private fun migrateDataToNewBedspaceModelTables(premiseIds: List<UUID>) {
    migrationLogger.info("Starting bedspace model migration with batch size of ${premiseIds.size}...")
    migrateDataToCas3Premises(premiseIds)
    migrationLogger.info("Migrated batch size of ${premiseIds.size} to new cas3_premises table - data migrated from the premises and temporary_accommodation tables.")
    // TODO: pass premisesBatch to transform and migrate data to next bedspace model refactor tables (upcoming tickets)
  }

  private fun migrateDataToCas3Premises(premiseIds: List<UUID>) {
    val premisesBatch = premisesRepository.findByIdIn(premiseIds)
    val temporaryAccommodationPremisesBatch = premisesRepository.findTemporaryAccommodationPremisesIdIn(premiseIds)
    val cas3Premises = transformCas3PremisesBatch(premisesBatch, temporaryAccommodationPremisesBatch)
    cas3PremisesRepository.saveAllAndFlush(cas3Premises)
  }

  private fun transformCas3PremisesBatch(premisesBatch: List<PremisesEntity>, temporaryAccommodationPremisesBatch: List<TemporaryAccommodationPremisesEntity>) =
    premisesBatch.map { premise ->
      val temporaryAccommodationPremises = temporaryAccommodationPremisesBatch.firstOrNull { it.id == premise.id }
      Cas3PremisesEntity(
        id = UUID.randomUUID(),
        name = premise.name,
        postcode = premise.postcode,
        addressLine1 = premise.addressLine1,
        addressLine2 = premise.addressLine2,
        town = premise.town,
        localAuthorityArea = premise.localAuthorityArea,
        status = premise.status,
        notes = premise.notes,
        probationDeliveryUnit = temporaryAccommodationPremises?.probationDeliveryUnit,
        turnaroundWorkingDayCount = temporaryAccommodationPremises?.turnaroundWorkingDayCount ?: 0
      )
    }
}