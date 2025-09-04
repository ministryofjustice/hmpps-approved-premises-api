package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.migration

import org.springframework.stereotype.Component
import org.springframework.transaction.support.TransactionTemplate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.Cas2UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.Cas2UserRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.Cas2UserType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.ExternalUserRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.NomisUserRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2v2.jpa.entity.Cas2v2UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2v2.jpa.entity.Cas2v2UserRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2v2.jpa.entity.Cas2v2UserType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.migration.MigrationInBatchesJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.migration.MigrationLogger
import java.time.OffsetDateTime
import java.util.UUID

@Component
class Cas2UserMigrationJob(
  private val cas2UserRepository: Cas2UserRepository,
  private val cas2v2UserRepository: Cas2v2UserRepository,
  private val nomisUserRepository: NomisUserRepository,
  private val externalUserRepository: ExternalUserRepository,
  private val migrationLogger: MigrationLogger,
  transactionTemplate: TransactionTemplate,
) : MigrationInBatchesJob(migrationLogger, transactionTemplate) {
  override val shouldRunInTransaction: Boolean = java.lang.Boolean.FALSE

  override fun process(pageSize: Int) {
    migrationLogger.info("Starting nomis migration process...")
    val nomisIds = nomisUserRepository.findNomisUserIds()
    super.processInBatches(nomisIds, batchSize = 100) { batchIds ->
      migrateNomisUserDataToCas2UsersTable(batchIds)
    }
    migrationLogger.info("Completed nomis migration process...")
    migrationLogger.info("Starting external migration process...")
    val externalIds = externalUserRepository.findExternalUserIds()
    super.processInBatches(externalIds, batchSize = 100) { batchIds ->
      migrateExternalUserDataToCas2UsersTable(batchIds)
    }
    migrationLogger.info("Completed external migration process...")
    migrationLogger.info("Starting cas2v2 migration process...")
    val cas2v2Ids = cas2v2UserRepository.findCas2v2UserIds()
    super.processInBatches(cas2v2Ids, batchSize = 100) { batchIds ->
      migrateCas2v2UserDataToCas2UsersTable(batchIds)
    }
    migrationLogger.info("Completed cas2v2 migration process...")
  }

  private fun migrateNomisUserDataToCas2UsersTable(nomisIds: List<UUID>) {
    migrationLogger.info("Starting Nomis user migration with batch size of ${nomisIds.size}...")
    val nomisUserData = generateNomisUser(nomisIds)
    cas2UserRepository.saveAllAndFlush(nomisUserData)
    migrationLogger.info("Migrated batch size of ${nomisIds.size} to new cas_2_users table - data migrated from the nomis_users tables.")
  }

  private fun migrateExternalUserDataToCas2UsersTable(externalIds: List<UUID>) {
    migrationLogger.info("Starting External user migration with batch size of ${externalIds.size}...")
    val externalUserData = generateExternalUser(externalIds)
    cas2UserRepository.saveAllAndFlush(externalUserData)
    migrationLogger.info("Migrated batch size of ${externalIds.size} to new cas_2_users table - data migrated from the external_users tables.")
  }

  private fun migrateCas2v2UserDataToCas2UsersTable(cas2v2Ids: List<UUID>) {
    migrationLogger.info("Starting Cas2v2 user migration with batch size of ${cas2v2Ids.size}...")
    val externalUserData = generateCas2v2User(cas2v2Ids)
    cas2UserRepository.saveAllAndFlush(externalUserData)
    migrationLogger.info("Migrated batch size of ${cas2v2Ids.size} to new cas_2_users table - data migrated from the cas_2_v2_users tables.")
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

  private fun generateExternalUser(externalIds: List<UUID>) = externalUserRepository.findAllById(externalIds).map {
    Cas2UserEntity(
      id = it.id,
      name = it.name,
      email = it.email,
      createdAt = OffsetDateTime.now(),
      updatedAt = OffsetDateTime.now(),
      isActive = true,
      isEnabled = it.isEnabled,
      nomisStaffId = null,
      nomisAccountType = null,
      activeNomisCaseloadId = null,
      deliusStaffCode = null,
      deliusTeamCodes = null,
      externalType = it.origin,
      userType = Cas2UserType.EXTERNAL,
      username = it.username,
    )
  }

  private fun generateCas2v2User(cas2v2Ids: List<UUID>) = cas2v2UserRepository.findAllById(cas2v2Ids).map {
    Cas2UserEntity(
      id = it.id,
      name = it.name,
      email = it.email,
      createdAt = OffsetDateTime.now(),
      updatedAt = OffsetDateTime.now(),
      isActive = it.isActive,
      isEnabled = it.isEnabled,
      nomisStaffId = it.nomisStaffId,
      // TODO besscerule - this matches the nomis table accountType column (it's usually GENERAL), what should we put here
      nomisAccountType = null,
      activeNomisCaseloadId = it.activeNomisCaseloadId,
      deliusStaffCode = it.deliusStaffCode,
      deliusTeamCodes = it.deliusTeamCodes,
      // TODO besscerule - this matches the external table origin column (it's usually NACRO), what should we put here
      externalType = null,
      userType = getUserType(it),
      username = it.username,
    )
  }

  fun getUserType(user: Cas2v2UserEntity) = when (user.userType) {
    Cas2v2UserType.NOMIS -> Cas2UserType.NOMIS
    Cas2v2UserType.DELIUS -> Cas2UserType.DELIUS
    Cas2v2UserType.EXTERNAL -> Cas2UserType.EXTERNAL
  }
}
