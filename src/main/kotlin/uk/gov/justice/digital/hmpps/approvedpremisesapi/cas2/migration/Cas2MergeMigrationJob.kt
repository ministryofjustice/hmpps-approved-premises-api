package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.migration

import org.springframework.stereotype.Component
import org.springframework.transaction.support.TransactionTemplate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.Cas2ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.Cas2ApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.Cas2UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.Cas2UserRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.Cas2UserType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.ExternalUserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.ExternalUserRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.NomisUserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.NomisUserRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2v2.jpa.entity.Cas2v2ApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2v2.jpa.entity.Cas2v2UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2v2.jpa.entity.Cas2v2UserRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2v2.jpa.entity.Cas2v2UserType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.migration.MigrationInBatchesJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.migration.MigrationLogger
import java.time.OffsetDateTime
import java.util.UUID

@Component
class Cas2MergeMigrationJob(
  private val cas2UserRepository: Cas2UserRepository,
  private val cas2v2UserRepository: Cas2v2UserRepository,
  private val nomisUserRepository: NomisUserRepository,
  private val externalUserRepository: ExternalUserRepository,
  private val cas2ApplicationRepository: Cas2ApplicationRepository,
  private val cas2v2ApplicationRepository: Cas2v2ApplicationRepository,
  private val migrationLogger: MigrationLogger,
  transactionTemplate: TransactionTemplate,
) : MigrationInBatchesJob(migrationLogger, transactionTemplate) {
  override val shouldRunInTransaction: Boolean = java.lang.Boolean.FALSE

  override fun process(pageSize: Int) {
    migrationLogger.info("Starting cas2 merge migration process...")
    migrateAllUsersToCas2UsersTable()
    updateCas2ApplicationsInCas2ApplicationsTable()
    migrateCas2v2ApplicationsToCas2ApplicationsTable()
    migrationLogger.info("Completed cas2 merge migration process...")
  }

  private fun migrateCas2v2ApplicationsToCas2ApplicationsTable() {
    migrationLogger.info("Starting cas2v2 applications migration process...")
    val applicationIds = cas2v2ApplicationRepository.findApplicationIds()
    super.processInBatches(applicationIds, batchSize = 100) { batchIds ->
      migrateCas2v2ApplicationsDataToCas2ApplicationsTable(batchIds)
    }
    migrationLogger.info("Completed cas2v2 applications migration process...")
  }

  private fun updateCas2ApplicationsInCas2ApplicationsTable() {
    migrationLogger.info("Starting applications update process...")
    val applicationIds = cas2ApplicationRepository.findApplicationIds()
    super.processInBatches(applicationIds, batchSize = 100) { batchIds ->
      updateCas2ApplicationsTable(batchIds)
    }
    migrationLogger.info("Completed users migration process...")
  }

  private fun migrateAllUsersToCas2UsersTable() {
    migrationLogger.info("Starting users migration process...")
    val nomisIds = nomisUserRepository.findNomisUserIds()
    super.processInBatches(nomisIds, batchSize = 100) { batchIds ->
      migrateNomisUserDataToCas2UsersTable(batchIds)
    }
    migrationLogger.info("Completed nomis migration process...")
    val externalIds = externalUserRepository.findExternalUserIds()
    super.processInBatches(externalIds, batchSize = 100) { batchIds ->
      migrateExternalUserDataToCas2UsersTable(batchIds)
    }
    migrationLogger.info("Completed external migration process...")
    val cas2v2Ids = cas2v2UserRepository.findCas2v2UserIds()
    super.processInBatches(cas2v2Ids, batchSize = 100) { batchIds ->
      migrateCas2v2UserDataToCas2UsersTable(batchIds)
      migrateCas2v2UserIdsToNomisUsersTable(batchIds)
      migrateCas2v2UserIdsToExternalUsersTable(batchIds)
    }
    migrationLogger.info("Completed cas2v2 migration process...")
    migrationLogger.info("Completed users migration process...")
  }

  private fun migrateCas2v2ApplicationsDataToCas2ApplicationsTable(applicationIds: List<UUID>) {
    migrationLogger.info("Starting Cas2v2 application migration with batch size of ${applicationIds.size}...")
    val applicationData = generateCas2v2Application(applicationIds)
    cas2ApplicationRepository.saveAllAndFlush(applicationData)
    migrationLogger.info("Migrated batch size of ${applicationIds.size} to cas_2_applications table - data migrated from the cas_2_v2_applications tables.")
  }

  private fun updateCas2ApplicationsTable(applicationIds: List<UUID>) {
    migrationLogger.info("Starting application update with batch size of ${applicationIds.size}...")
    val applicationData = generateCas2Application(applicationIds)
    cas2ApplicationRepository.saveAllAndFlush(applicationData)
    migrationLogger.info("Updated batch size of ${applicationIds.size} in cas_2_applications table.")
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

  private fun migrateCas2v2UserIdsToNomisUsersTable(cas2v2Ids: List<UUID>) {
    migrationLogger.info("Starting Cas2v2 user migration with batch size of ${cas2v2Ids.size}...")
    val dummyNomisUserData = generateDummyCas2v2NomisUser(cas2v2Ids)
    nomisUserRepository.saveAllAndFlush(dummyNomisUserData)
    migrationLogger.info("Migrated batch size of dummy ${cas2v2Ids.size} to nomis_users table - data migrated from the cas_2_v2_users tables.")
  }

  private fun migrateCas2v2UserIdsToExternalUsersTable(cas2v2Ids: List<UUID>) {
    migrationLogger.info("Starting Cas2v2 user migration with batch size of ${cas2v2Ids.size}...")
    val dummyExternalUserData = generateDummyCas2v2ExternalUser(cas2v2Ids)
    externalUserRepository.saveAllAndFlush(dummyExternalUserData)
    migrationLogger.info("Migrated batch size of dummy ${cas2v2Ids.size} to external_users table - data migrated from the cas_2_v2_users tables.")
  }

  private fun generateCas2Application(applicationIds: List<UUID>) = cas2ApplicationRepository.findAllById(applicationIds).map {
    Cas2ApplicationEntity(
      id = it.id,
      crn = it.crn,
      createdByUser = it.createdByUser,
      // TODO besscerule - the important part of populating the column!
      createdByCas2User = cas2UserRepository.findById(it.createdByUser.id).get(),
      data = it.data,
      document = it.document,
      createdAt = it.createdAt,
      submittedAt = it.submittedAt,
      abandonedAt = it.abandonedAt,
      statusUpdates = it.statusUpdates,
      nomsNumber = it.nomsNumber,
      telephoneNumber = it.telephoneNumber,
      notes = it.notes,
      assessment = it.assessment,
      applicationAssignments = it.applicationAssignments,
      referringPrisonCode = it.referringPrisonCode,
      hdcEligibilityDate = it.hdcEligibilityDate,
      conditionalReleaseDate = it.conditionalReleaseDate,
      preferredAreas = it.preferredAreas,
      applicationOrigin = it.applicationOrigin,
      bailHearingDate = it.bailHearingDate,
    )
  }

  private fun generateCas2v2Application(applicationIds: List<UUID>) = cas2v2ApplicationRepository.findAllById(applicationIds).map {
    Cas2ApplicationEntity(
      id = it.id,
      crn = it.crn,
      createdByUser = nomisUserRepository.findById(it.createdByUser.id).get(),
      createdByCas2User = cas2UserRepository.findById(it.createdByUser.id).get(),
      data = it.data,
      document = it.document,
      createdAt = it.createdAt,
      submittedAt = it.submittedAt,
      abandonedAt = it.abandonedAt,
      // TODO besscerule - empty until we can add the 2v2 table
      statusUpdates = mutableListOf(),
      nomsNumber = it.nomsNumber,
      telephoneNumber = it.telephoneNumber,
      // TODO besscerule - empty until we can add the 2v2 table
      notes = mutableListOf(),
      // TODO besscerule - empty until we can add the 2v2 table
      assessment = null,
      // TODO besscerule - empty until we can populate the assignments table
      applicationAssignments = mutableListOf(),
      referringPrisonCode = it.referringPrisonCode,
      hdcEligibilityDate = it.hdcEligibilityDate,
      conditionalReleaseDate = it.conditionalReleaseDate,
      preferredAreas = it.preferredAreas,
      applicationOrigin = it.applicationOrigin,
      bailHearingDate = it.bailHearingDate,
    )
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

  private fun generateDummyCas2v2NomisUser(cas2v2Ids: List<UUID>) = cas2v2Ids.map {
    NomisUserEntity(
      id = it,
      name = "DUMMY_USER",
      email = "DUMMY_USER",
      createdAt = OffsetDateTime.now(),
      isActive = true,
      isEnabled = true,
      nomisStaffId = 0,
      accountType = "GENERAL",
      activeCaseloadId = null,
      nomisUsername = "DUMMY_USER_$it",
    )
  }

  private fun generateDummyCas2v2ExternalUser(cas2v2Ids: List<UUID>) = cas2v2Ids.map {
    ExternalUserEntity(
      id = it,
      name = "DUMMY_USER",
      email = "DUMMY_USER",
      createdAt = OffsetDateTime.now(),
      isEnabled = true,
      username = "DUMMY_USER_$it",
      origin = "NACRO",
    )
  }

  private fun getUserType(user: Cas2v2UserEntity) = when (user.userType) {
    Cas2v2UserType.NOMIS -> Cas2UserType.NOMIS
    Cas2v2UserType.DELIUS -> Cas2UserType.DELIUS
    Cas2v2UserType.EXTERNAL -> Cas2UserType.EXTERNAL
  }
}
