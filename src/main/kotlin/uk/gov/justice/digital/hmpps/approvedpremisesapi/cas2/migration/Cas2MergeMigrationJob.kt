package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.migration

import org.springframework.stereotype.Component
import org.springframework.transaction.support.TransactionTemplate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.Cas2ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.Cas2ApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.Cas2AssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.Cas2AssessmentRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.Cas2UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.Cas2UserRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.Cas2UserType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.ExternalUserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.ExternalUserRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.NomisUserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.NomisUserRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2v2.jpa.entity.Cas2v2ApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2v2.jpa.entity.Cas2v2AssessmentRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2v2.jpa.entity.Cas2v2UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2v2.jpa.entity.Cas2v2UserRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2v2.jpa.entity.Cas2v2UserType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.migration.MigrationInBatchesJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.migration.MigrationLogger
import java.time.OffsetDateTime
import java.util.UUID

const val BATCH_SIZE = 100

@Component
class Cas2MergeMigrationJob(
  private val cas2UserRepository: Cas2UserRepository,
  private val cas2v2UserRepository: Cas2v2UserRepository,
  private val nomisUserRepository: NomisUserRepository,
  private val externalUserRepository: ExternalUserRepository,
  private val cas2ApplicationRepository: Cas2ApplicationRepository,
  private val cas2v2ApplicationRepository: Cas2v2ApplicationRepository,
  private val cas2AssessmentRepository: Cas2AssessmentRepository,
  private val cas2v2AssessmentRepository: Cas2v2AssessmentRepository,
  private val migrationLogger: MigrationLogger,
  transactionTemplate: TransactionTemplate,
) : MigrationInBatchesJob(migrationLogger, transactionTemplate) {
  override val shouldRunInTransaction: Boolean = java.lang.Boolean.FALSE

  override fun process(pageSize: Int) {
    migrationLogger.info("Starting cas2 merge migration process...")
    migrateCas2Users()
    migrateAndUpdateCas2Applications()
    migrateCas2Assessments()
    migrationLogger.info("Finished cas2 merge migration process...")
  }

  private fun migrateCas2Users() {
    migrationLogger.info("Starting cas2 user migration process...")
    val nomisIds = nomisUserRepository.findNomisUserIds()
    migrationLogger.info("Nomis users to migrate: $nomisIds.")
    super.processInBatches(nomisIds, batchSize = BATCH_SIZE) { batchIds ->
      migrationLogger.info("Migrate with batch size of ${batchIds.size} in process")
      cas2UserRepository.saveAllAndFlush(generateNomisUser(batchIds))
    }
    val externalIds = externalUserRepository.findExternalUserIds()
    migrationLogger.info("External users to migrate: $externalIds.")
    super.processInBatches(externalIds, batchSize = BATCH_SIZE) { batchIds ->
      migrationLogger.info("Migrate with batch size of ${batchIds.size} in process")
      cas2UserRepository.saveAllAndFlush(generateExternalUser(batchIds))
    }
    val cas2Ids = cas2v2UserRepository.findCas2v2UserIds()
    migrationLogger.info("Cas2 users to migrate: $cas2Ids.")
    super.processInBatches(cas2Ids, batchSize = BATCH_SIZE) { batchIds ->
      migrationLogger.info("Migrate with batch size of ${batchIds.size} in process")
      migrationLogger.info("Migrating to cas2 Users table")
      cas2UserRepository.saveAllAndFlush(generateCas2v2User(batchIds))
      migrationLogger.info("Migrating dummy data to nomis Users table")
      nomisUserRepository.saveAllAndFlush(generateDummyCas2v2NomisUser(batchIds))
      migrationLogger.info("Migrating dummy data to external Users table")
      externalUserRepository.saveAllAndFlush(generateDummyCas2v2ExternalUser(batchIds))
    }
    migrationLogger.info("Finished cas2 user migration process...")
  }

  private fun migrateAndUpdateCas2Applications() {
    migrationLogger.info("Starting cas2 application migration process...")
    val cas2ApplicationIds = cas2ApplicationRepository.findApplicationIds()
    migrationLogger.info("Cas2 applications to update: $cas2ApplicationIds.")
    super.processInBatches(cas2ApplicationIds, batchSize = BATCH_SIZE) { batchIds ->
      migrationLogger.info("Update with batch size of ${batchIds.size} in process")
      cas2ApplicationRepository.saveAllAndFlush(generateCas2Application(batchIds))
    }
    val cas2v2ApplicationIds = cas2v2ApplicationRepository.findApplicationIds()
    migrationLogger.info("Cas2v2 applications to migrate: $cas2v2ApplicationIds.")
    super.processInBatches(cas2v2ApplicationIds, batchSize = BATCH_SIZE) { batchIds ->
      migrationLogger.info("Migrate with batch size of ${batchIds.size} in process")
      cas2ApplicationRepository.saveAllAndFlush(generateCas2v2Application(batchIds))
    }
    migrationLogger.info("Finished cas2 application migration process...")
  }

  private fun migrateCas2Assessments() {
    migrationLogger.info("Starting cas2 assessment migration process...")
    val entityIds = cas2v2AssessmentRepository.findAssessmentIds()
    migrationLogger.info("Cas2 assessments to migrate: $entityIds.")
    super.processInBatches(entityIds, batchSize = BATCH_SIZE) { batchIds ->
      migrationLogger.info("Migrate with batch size of ${batchIds.size} in process")
      cas2AssessmentRepository.saveAllAndFlush(generateCas2v2Assessment(batchIds))
    }
    migrationLogger.info("Finished cas2 assessment migration process...")
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

  private fun generateCas2v2Assessment(assessmentIds: List<UUID>) = cas2v2AssessmentRepository.findAllById(assessmentIds).map {
    Cas2AssessmentEntity(
      id = it.id,
      createdAt = it.createdAt,
      application = cas2ApplicationRepository.findById(it.application.id).get(),
      nacroReferralId = it.nacroReferralId,
      assessorName = it.assessorName,
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
      nomsNumber = it.nomsNumber,
      telephoneNumber = it.telephoneNumber,
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
    val userType = getUserType(it)
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
      nomisAccountType = if (userType == Cas2UserType.NOMIS) "GENERAL" else null,
      activeNomisCaseloadId = it.activeNomisCaseloadId,
      deliusStaffCode = it.deliusStaffCode,
      deliusTeamCodes = it.deliusTeamCodes,
      // TODO besscerule - this matches the external table origin column (it's usually NACRO), what should we put here
      externalType = if (userType == Cas2UserType.EXTERNAL) "NACRO" else null,
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
