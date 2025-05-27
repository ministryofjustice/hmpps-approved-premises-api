package uk.gov.justice.digital.hmpps.approvedpremisesapi.migration.cas3

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Component
import org.springframework.stereotype.Repository
import org.springframework.transaction.support.TransactionTemplate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationPremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas3.Cas3PremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas3.Cas3PremisesRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.migration.MigrationInBatchesJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.migration.MigrationLogger
import java.util.UUID

@Component
class Cas3MigrateNewBedspaceModelDataJob(
  private val temporaryAccommodationPremisesRepository: TemporaryAccommodationPremisesRepository,
  private val cas3PremisesRepository: Cas3PremisesRepository,
  private val migrationLogger: MigrationLogger,
  transactionTemplate: TransactionTemplate,
) : MigrationInBatchesJob(migrationLogger, transactionTemplate) {
  override val shouldRunInTransaction: Boolean = java.lang.Boolean.FALSE

  override fun process(pageSize: Int) {
    migrationLogger.info("Starting migration process...")
    val cas3PremisesIds = temporaryAccommodationPremisesRepository.findTemporaryAccommodationPremisesIds()
    super.processInBatches(cas3PremisesIds, batchSize = 100) { batchIds ->
      migrateDataToNewBedspaceModelTables(batchIds)
    }
    migrationLogger.info("Completed migration process...")
  }

  private fun migrateDataToNewBedspaceModelTables(premiseIds: List<UUID>) {
    migrationLogger.info("Starting bedspace model migration with batch size of ${premiseIds.size}...")
    migrateDataToCas3Premises(premiseIds)
    migrationLogger.info("Migrated batch size of ${premiseIds.size} to new cas3_premises table - data migrated from the premises and temporary_accommodation tables.")
  }

  private fun migrateDataToCas3Premises(premiseIds: List<UUID>) {
    val temporaryAccommodationPremisesBatch = temporaryAccommodationPremisesRepository.findTemporaryAccommodationPremisesByIds(premiseIds)
    val cas3Premises = transformCas3PremisesBatch(temporaryAccommodationPremisesBatch)
    cas3PremisesRepository.saveAllAndFlush(cas3Premises)
  }

  private fun transformCas3PremisesBatch(temporaryAccommodationPremisesBatch: List<TemporaryAccommodationPremisesEntity>) = temporaryAccommodationPremisesBatch.map { premise ->
    Cas3PremisesEntity(
      id = premise.id,
      name = premise.name,
      postcode = premise.postcode,
      addressLine1 = premise.addressLine1,
      addressLine2 = premise.addressLine2,
      town = premise.town,
      localAuthorityArea = premise.localAuthorityArea,
      status = premise.status,
      notes = premise.notes,
      probationDeliveryUnit = premise.probationDeliveryUnit!!,
    )
  }
}

@Repository
interface TemporaryAccommodationPremisesRepository : JpaRepository<TemporaryAccommodationPremisesEntity, UUID> {
  @Query("SELECT tap.id FROM TemporaryAccommodationPremisesEntity tap")
  fun findTemporaryAccommodationPremisesIds(): List<UUID>

  @Query("SELECT tap FROM TemporaryAccommodationPremisesEntity tap WHERE tap.id IN :ids")
  fun findTemporaryAccommodationPremisesByIds(ids: List<UUID>): List<TemporaryAccommodationPremisesEntity>
}
