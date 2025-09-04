package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.migration

import org.springframework.stereotype.Component
import org.springframework.transaction.support.TransactionTemplate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.Cas2UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.Cas2UserRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.Cas2UserType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.NomisUserRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.migration.MigrationInBatchesJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.migration.MigrationLogger
import java.time.OffsetDateTime
import java.util.*

@Component
class Cas2UserNomisMigrationJob(
  private val cas2UserRepository: Cas2UserRepository,
  private val nomisUserRepository: NomisUserRepository,
  private val migrationLogger: MigrationLogger,
  transactionTemplate: TransactionTemplate,
) : MigrationInBatchesJob(migrationLogger, transactionTemplate) {
  override val shouldRunInTransaction: Boolean = java.lang.Boolean.FALSE

  override fun process(pageSize: Int) {
    migrationLogger.info("Starting migration process...")

    val nomisIds = nomisUserRepository.findNomisUserIds()
    super.processInBatches(nomisIds, batchSize = 100) { batchIds ->
      migrateNomisUserDataToCas2UsersTable(batchIds)
    }
    migrationLogger.info("Completed migration process...")
  }

  private fun migrateNomisUserDataToCas2UsersTable(nomisIds: List<UUID>) {
    migrationLogger.info("Starting Nomis user migration with batch size of ${nomisIds.size}...")
    val nomisUserData = generateNomisUser(nomisIds)
    cas2UserRepository.saveAllAndFlush(nomisUserData)
    migrationLogger.info("Migrated batch size of ${nomisIds.size} to new cas2_users table - data migrated from the nomis_users tables.")
  }

  private fun generateNomisUser(nomisIds: List<UUID>) = nomisUserRepository.findAllById(nomisIds).map {
    Cas2UserEntity(
      id = it.id,
      name = it.name,
      email = it.email,
      createdAt = OffsetDateTime.now(),
      updatedAt = OffsetDateTime.now(),
      isActive = it.isActive,
      isEnabled = it.isEnabled,
      nomisStaffId = it.nomisStaffId,
      nomisAccountType = it.accountType,
      activeNomisCaseloadId = it.activeCaseloadId,
      deliusStaffCode = null,
      deliusTeamCodes = null,
      externalType = null,
      userType = Cas2UserType.NOMIS,
      username = it.nomisUsername,
    )
  }
}
